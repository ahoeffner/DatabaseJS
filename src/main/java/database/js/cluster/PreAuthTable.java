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
 * To align with 2k, the first extend starts at byte 1 and size 2047 bytes.
 *
 */
public class PreAuthTable
{
  private final int extend;
  private final int offset;
  private final int entries;
  private final int timeout;
  private final MappedByteBuffer shmmem;

  public final static int reclen = 16 + Long.BYTES + 1;
  private final Logger logger = Logger.getLogger("rest");
  
  
  public static void main(String[] args) throws Exception
  {
    Config config = new Config();
    PreAuthTable table = new PreAuthTable(config);

    Reader reader = table.getReader();
    Writer writer = table.getWriter();
    
    
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

    this.shmmem = fc.map(FileChannel.MapMode.READ_WRITE,0,256*2048);
    this.timeout = config.getSSO().timeout;
    this.extend = shmmem.get(0);
    
    int offset = 0;
    entries = entries(extend);
    
    for (int i = 0; i < entries; i++)
      offset = next(extend,offset);
    
    this.offset = offset;
  }
  
  
  int entries(int extend)
  {
    int pos = 0;
    if (extend == 0) pos++;
    return(shmmem.get(extend*2048+pos));
  }
  
  
  int next(int extend, int offset)
  {
    if (offset == 0)
      offset++;
    
    if (extend == 0 && offset == 1)
      offset++;

    return(offset + reclen + shmmem.get(extend*2048+offset));
  }


  boolean fits(int offset, PreAuthRecord entry)
  {
    return(offset + reclen + entry.username.length() < 2048);
  }


  int put(PreAuthRecord entry, int extend, int offset)
  {
    if (offset == 0)
      offset++;
    
    if (extend == 0 && offset == 1)
      offset++;

    long time = entry.time;
    byte[] guid = entry.guid.getBytes();
    byte[] user = entry.username.getBytes();

    byte len = (byte) user.length;

    shmmem.put(offset++,len);
    shmmem.put(offset,guid);
    shmmem.putLong(offset+16,time);
    shmmem.put(offset+reclen,user);

    return(offset+reclen+len);
  }


  PreAuthRecord get(int extend, int offset)
  {
    if (offset == 0)
      offset++;
    
    if (extend == 0 && offset == 1)
      offset++;

    int len = shmmem.get(offset++);

    byte[] guid = new byte[16];
    byte[] user = new byte[len];

    shmmem.get(offset,guid);
    long time = shmmem.getLong(offset+16);
    shmmem.get(offset+reclen,user);

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


  public static class Reader
  {
    private int extend = 0;
    private int offset = 0;
    private final int timeout;
    private final PreAuthTable table;
    private final MappedByteBuffer shmmem;


    private Reader(PreAuthTable table)
    {
      this.table = table;
      this.shmmem = table.shmmem;
      this.extend = table.extend;
      this.offset = table.offset;
      this.timeout = table.timeout * 1000;
    }
    
    
    public ArrayList<PreAuthRecord> refresh()
    {
      ArrayList<PreAuthRecord> recs = new ArrayList<PreAuthRecord>();
      
      return(recs);
    }
  }


  public static class Writer
  {
    private int extend = 0;
    private int offset = 0;
    private final int timeout;
    private final PreAuthTable table;
    private final MappedByteBuffer shmmem;


    private Writer(PreAuthTable table)
    {
      this.table = table;
      this.shmmem = table.shmmem;
      this.extend = table.extend;
      this.offset = table.offset;
      this.timeout = table.timeout * 1000;
    }
    
    
    public void write(PreAuthRecord auth)
    {
      if (!table.fits(offset,auth))
      {
        extend++;
        offset = 0;
        shmmem.put(0,(byte) extend);
        System.out.println("Next extend"); 
      }
      
      offset = table.put(auth,extend,offset);
    }
  }
}