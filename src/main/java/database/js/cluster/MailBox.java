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
import java.util.HashMap;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.logging.Logger;
import database.js.config.Paths;
import java.nio.file.FileSystem;
import database.js.config.Config;
import java.nio.MappedByteBuffer;
import java.nio.file.FileSystems;
import java.nio.channels.FileChannel;
import static java.nio.file.StandardOpenOption.*;
import java.nio.file.attribute.PosixFilePermission;


public class MailBox
{
  private final int extnds;
  private final int extsize;
  private final Logger logger;
  private final MappedByteBuffer shmmem;
  private final HashMap<Integer,Long> extmap;

  private final Object RLOCK = new Object();
  private final Object WLOCK = new Object();


  public MailBox(Config config, short id) throws Exception
  {
    String filename = getFileName(id);
    this.logger = config.getLogger().intern;
    FileSystem fs = FileSystems.getDefault();

    this.extmap = new HashMap<Integer,Long>();
    this.extnds = config.getTopology().extnds();
    this.extsize = config.getTopology().extsize();

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
        logger.warning("Unable to set file permissions for mailbox");
      }
    }

    this.shmmem = fc.map(FileChannel.MapMode.READ_WRITE,0,extnds*extsize);
  }


  public boolean fits(byte[] data)
  {
    return(data.length <= extsize);
  }


  public boolean write(int extend, byte[] data)
  {
    if (data.length > extsize)
      return(false);

    synchronized(WLOCK)
    {
      shmmem.position(extend*extsize);
      shmmem.put(data);
    }

    return(true);
  }


  public int write(long id, byte[] data)
  {
    if (data.length > extsize)
      return(-1);

    int start = (int) (id % extnds);

    synchronized(WLOCK)
    {
      for (int i = 0; i < extnds; i++)
      {
        int extend = (start + i) % extnds;
        if (extmap.get(extend) == null)
        {
          shmmem.position(extend*extsize);
          shmmem.put(data);
          return(extend);
        }
      }
    }

    logger.warning("No available extends in mailbox");
    return(-1);
  }


  public byte[] read(int extend, int size)
  {
    byte[] data = new byte[size];

    synchronized(RLOCK)
    {
      extmap.remove(extend);
      shmmem.position(extend*extsize);
      shmmem.get(data);
    }

    return(data);
  }


  public void clear(int extend)
  {
    extmap.remove(extend);
  }


  private String getFileName(short id)
  {
    String hex = Integer.toHexString(id);
    hex = String.format("%4s",hex).replace(' ','0');
    return(Paths.ipcdir + File.separator + "ipc" + hex + ".mbx");
  }
}