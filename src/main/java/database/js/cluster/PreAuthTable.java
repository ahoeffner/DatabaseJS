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
import java.nio.file.attribute.PosixFilePermission;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardOpenOption.CREATE;


/**
 *
 * A record consists of guid (16 bytes) + created (long) + length (1 byte) + username.
 * Records are written to extends (2 k) which is recycled when the file is full
 * Each extend has a header that consists of the number of entries (1 byte)
 * The file has has a header that consists of 1 byte that indicates the current extend
 *
 */
public class PreAuthTable
{
  private int extend = 0;
  private final int timeout;
  private final MappedByteBuffer shmmem;

  public final static int rechead = 16 + Long.BYTES + 1;
  private final Logger logger = Logger.getLogger("rest");


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
        logger.warning("Unable to set file permissions for mailbox");
      }
    }

    this.shmmem = fc.map(FileChannel.MapMode.READ_WRITE,0,256*2048);
    this.timeout = config.getSSO().timeout;
    this.extend = shmmem.get(0);
    logger.info("PreAuthTab");
  }


  public Reader getReader()
  {
    return(new Reader(shmmem,extend,timeout));
  }


  public Writer getWriter()
  {
    return(new Writer(shmmem,extend,timeout));
  }


  public static class Reader
  {
    private int extend = 0;
    private int timeout = 0;
    private final MappedByteBuffer shmmem;


    private Reader(MappedByteBuffer shmmem, int extend, int timeout)
    {
      this.shmmem = shmmem;
      this.extend = extend;
      this.timeout = timeout * 1000;
    }
  }


  public static class Writer
  {
    private int extend = 0;
    private int timeout = 0;
    private final MappedByteBuffer shmmem;


    private Writer(MappedByteBuffer shmmem, int extend, int timeout)
    {
      this.shmmem = shmmem;
      this.extend = extend;
      this.timeout = timeout * 1000;
    }
  }
}