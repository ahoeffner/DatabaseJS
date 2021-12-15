/*
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.

 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

package database.js.cluster;

import java.io.File;
import java.util.Set;
import java.util.HashSet;
import java.nio.file.Path;
import java.nio.file.Files;
import database.js.config.Paths;
import java.nio.file.FileSystem;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.handlers.rest.Guid;
import java.nio.MappedByteBuffer;
import java.nio.file.FileSystems;
import java.nio.channels.FileChannel;
import static java.nio.file.StandardOpenOption.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;


/**
 *
 * A record consists of user.length (1 byte) + guid (16 bytes) + created (long) + user.
 * Records are written to extends (2 k) which is recycled when the file is full
 * Each extend has a header that consists of the number of entries (1 byte)
 * The file has has a header that consists of 1 byte that indicates the current extend.
 * To align with extend size, the first extend starts at byte 1 and size EXTENDSIZE - 1 bytes.
 *
 */
public class PreAuthTable
{
  private final int extend;
  private final int offset;
  private final int entries;
  private final int timeout;
  private final MappedByteBuffer shmmem;

  public static final int EXTENDS = 6;
  public static final int EXTENDSIZE = 1024;
  private final Logger logger = Logger.getLogger("rest");
  
  
  public static void main(String[] args) throws Exception
  {
    Config config = new Config();
    PreAuthTable table = new PreAuthTable(config);

    Reader reader = table.getReader();
    Writer writer = table.getWriter();
    
    for (int i = 0; i < 5; i++)
    {
      String guid = new Guid().toString();
      PreAuthRecord auth = new PreAuthRecord(guid,"alex "+i);
      writer.write(auth);
    }
    
    ArrayList<PreAuthRecord> records = reader.refresh();
    System.out.println("read "+records.size());
  }


  public PreAuthTable(Config config) throws Exception
  {
    FileSystem fs = FileSystems.getDefault();
    String filename = Paths.ipcdir + File.separator + "auth.tab";

    Path path = fs.getPath(filename);
    FileChannel fc = FileChannel.open(path,CREATE,READ,WRITE);

    if (!System.getProperty("os.name").startsWith("Windows"))
    {
      try
      {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        Files.setPosixFilePermissions(path,perms);
      }
      catch (Exception e)
      {
        logger.warning("Unable to set file permissions for PreAuthTable");
      }
    }

    this.shmmem = fc.map(FileChannel.MapMode.READ_WRITE,0,EXTENDS*EXTENDSIZE);
    this.timeout = config.getSSO().timeout;

    this.extend = current();
    
    entries = entries(extend);
    int offset = align(extend);
    
    for (int i = 0; i < entries; i++)
      offset = next(extend,offset);
    
    this.offset = offset;
  }
  
  
  int next(int extend)
  {
    return(next(extend,false));
  }
  
  
  int next(int extend, boolean set)
  {
    extend = ++extend % EXTENDS;
    if (set) current(extend);
    return(extend);
  }
  
  
  int current()
  {
    return(shmmem.get(0));
  }
  
  
  void current(int extend)
  {
    shmmem.put(0,(byte) extend);
  }
  
  
  int align(int extend)
  {
    if (extend != 0) return(1);
    else             return(2); 
  }
  
  
  int entries(int extend)
  {
    int pos = align(extend) - 1;
    return(shmmem.get(extend*EXTENDSIZE+pos));
  }
  
  
  void entries(int extend, int entries)
  {
    int pos = align(extend) - 1;
    shmmem.put(extend*EXTENDSIZE+pos,(byte) entries);
  }
  
  
  int next(int extend, int offset)
  {
    return(offset + PreAuthRecord.reclen + shmmem.get(extend*EXTENDSIZE+offset));
  }


  boolean fits(int offset, PreAuthRecord entry)
  {
    return(offset + entry.size() < EXTENDSIZE);
  }


  int put(PreAuthRecord entry, int extend, int entries, int offset)
  {
    long time = entry.time;
    byte[] guid = entry.guid.getBytes();
    byte[] user = entry.username.getBytes();

    offset += extend*EXTENDSIZE;
    byte len = (byte) user.length;    

    shmmem.put(offset,len);
    shmmem.put(offset+1,guid);
    shmmem.putLong(offset+1+16,time);
    shmmem.put(offset+PreAuthRecord.reclen,user);
    
    entries(extend,entries);
    return(offset+PreAuthRecord.reclen+len);
  }


  PreAuthRecord get(int extend, int offset)
  {
    offset += extend*EXTENDSIZE;
    int len = shmmem.get(offset);

    byte[] guid = new byte[16];
    byte[] user = new byte[len];

    shmmem.get(offset+1,guid);
    long time = shmmem.getLong(offset+1+16);
    shmmem.get(offset+PreAuthRecord.reclen,user);

    return(new PreAuthRecord(guid,time,user));
  }


  public Reader getReader()
  {
    return(new Reader(this));
  }


  public Writer getWriter()
  {
    return(new Writer(this));
  }
  
  
  public void scan()
  {
    System.out.println();
    System.out.println("current extend = "+current());
    
    for (int i = 0; i < EXTENDS; i++)
    {
      int offset = align(i);
      int records = entries(i);
      System.out.println("extend "+i+" records "+records);
      
      for (int j = 0; j < records; j++)
      {
        PreAuthRecord record = get(extend,offset);
        System.out.println(record);
        offset += record.size();
      }
    }
  }


  public static class Reader
  {
    private int extend = 0;
    private int offset = 0;
    private int entries = 0;
    private final int timeout;
    private final PreAuthTable table;
    private final MappedByteBuffer shmmem;


    private Reader(PreAuthTable table)
    {
      this.table = table;
      this.shmmem = table.shmmem;
      this.extend = table.extend;
      this.offset = table.offset;
      this.entries = table.entries;
      this.timeout = table.timeout * 1000;
    }
    
    
    public ArrayList<PreAuthRecord> refresh()
    {
      ArrayList<PreAuthRecord> recs = new ArrayList<PreAuthRecord>();
      
      int records = entries;
      int current = table.current();

      entries = table.entries(extend);

      System.out.println();
      System.out.println();
      System.out.println("extend "+extend+" pos = "+records+" total = "+entries);
      
      while(records < entries)
      {
        records++;
        PreAuthRecord record = table.get(extend,offset);
        
        recs.add(record);
        offset += record.size();

        System.out.println("extend="+extend+" ["+record+"] size="+record.size()+" offset="+offset);
        
        if (records == entries)
        {
          System.out.println("next, extend="+extend+" current="+current);
        }
      }
      
      return(recs);
    }
  }


  public static class Writer
  {
    private int extend = 0;
    private int offset = 0;
    private int entries = 0;
    private final int timeout;
    private final PreAuthTable table;
    private final MappedByteBuffer shmmem;


    private Writer(PreAuthTable table)
    {
      this.table = table;
      this.shmmem = table.shmmem;
      this.extend = table.extend;
      this.offset = table.offset;
      this.entries = table.entries;
      this.timeout = table.timeout * 1000;
    }
    
    
    public void write(PreAuthRecord auth)
    {
      if (!table.fits(offset,auth))
      {
        entries = 0;
        extend = table.next(extend,true);
        offset = table.align(extend);
      }
      
      offset = table.put(auth,extend,++entries,offset);
      System.out.println("extend="+extend+" ["+auth+"] size="+auth.size()+" offset="+offset);
    }
  }
}