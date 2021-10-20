package database.js.cluster;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Paths;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import database.js.config.Config;
import java.nio.channels.FileLock;
import java.nio.channels.FileChannel;


public class ProcessMonitor
{
  private final Logger logger;
  private final Config config;
  private final FileChannel channel;
  private static ProcessMonitor mon = null;
  
  private FileLock mgr = null;
  private FileLock http = null;


  ProcessMonitor(Config config) throws Exception
  {
    this.config = config;    
    File lfile = new File(getFileName());

    if (!lfile.exists())
    {
      byte[] bytes = new byte[2];
      FileOutputStream out = new FileOutputStream(lfile);
      out.write(bytes);
      out.close();
    }
    
    this.logger = config.getLogger().logger;
    this.channel = new RandomAccessFile(lfile,"rw").getChannel();    
  }


  private String getFileName()
  {
    return(Paths.ipcdir + File.separator + "cluster.lck");
  }


  public static void init(Config config) throws Exception
  {
    mon = new ProcessMonitor(config);
  }
  
  
  public static boolean noLocks()
  {
    try
    {
      FileLock test = mon.channel.tryLock(0,1,false);

      if (test != null)
      {
        test.release();
        return(true);
      }
      
      return(false);
    }
    catch (Exception e)
    {
      mon.logger.log(Level.SEVERE,e.getMessage(),e);
    }

    return(true);
  }
  
  
  public static boolean aquireHTTPLock()
  {
    try
    {
      mon.http = mon.channel.tryLock(0,1,false);

      if (mon.http == null)
        return(false);
      
      return(true);
    }
    catch (Exception e)
    {
      mon.logger.log(Level.SEVERE,e.getMessage(),e);
    }

    return(false);
  }
  
  
  public static boolean aquireManagerLock()
  {
    try
    {
      mon.mgr = mon.channel.tryLock(0,1,false);

      if (mon.mgr == null)
        return(false);
      
      return(true);
    }
    catch (Exception e)
    {
      mon.logger.log(Level.SEVERE,e.getMessage(),e);
    }

    return(false);
  }
}
