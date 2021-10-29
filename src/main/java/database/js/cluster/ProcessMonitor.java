package database.js.cluster;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Paths;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import database.js.config.Config;
import database.js.servers.Server;
import java.nio.channels.FileLock;
import java.nio.channels.FileChannel;


public class ProcessMonitor
{
  private final Server server;
  private final Logger logger;
  private final Config config;
  private final FileChannel channel;
  private static ProcessMonitor mon = null;

  private FileLock mgr = null;
  private FileLock http = null;

  private static final int MGR = 0;
  private static final int HTTP = 1;


  ProcessMonitor(Server server) throws Exception
  {
    this.server = server;
    this.config = server.config();
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


  public static void init(Server server) throws Exception
  {
    mon = new ProcessMonitor(server);
  }


  public static boolean isHTTP()
  {
    return(mon.http != null);
  }


  public static boolean isManager()
  {
    return(mon.mgr != null);
  }


  public static boolean noManager()
  {
    try
    {
      FileLock test = mon.channel.tryLock(MGR,1,false);

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
      mon.http = mon.channel.tryLock(HTTP,1,false);

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
      mon.mgr = mon.channel.tryLock(MGR,1,false);

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


  public static boolean releaseManagerLock()
  {
    try
    {
      mon.mgr.release();
      return(true);
    }
    catch (Exception e)
    {
      mon.logger.log(Level.SEVERE,e.getMessage(),e);
    }

    return(false);
  }


  public static void watchHTTP()
  {
    ProcessWatch watcher = new ProcessWatch(mon,HTTP);
    watcher.start();
  }


  public static void watchManager()
  {
    ProcessWatch watcher = new ProcessWatch(mon,MGR);
    watcher.start();
  }


  private void onServerDown(ProcessWatch watcher)
  {
    mon.logger.fine("Process "+watcher.type+" died");

    if (watcher.lock == HTTP)
    {
      if (server.isHttpType())
        server.setHTTP();
    }

    if (watcher.lock == MGR && server.isRestType() && !server.manager())
      server.setManager();

    if (server.manager())
      server.ensure();
  }


  private static class ProcessWatch extends Thread
  {
    private final int lock;
    private final String type;
    private FileLock flock = null;
    private final ProcessMonitor monitor;


    ProcessWatch(ProcessMonitor monitor, int lock)
    {
      this.lock = lock;
      this.monitor = monitor;
      this.type = lock == MGR ? "Manager" : "HTTP";

      this.setDaemon(true);
      this.setName("ProcessMonitor");
    }


    @Override
    public void run()
    {
      monitor.logger.fine("Watching "+type+" process");

      try
      {
        for (int i = 0; i < 512; i++)
        {
          long time = System.currentTimeMillis();
          flock = mon.channel.lock(lock,1,false);

          if (flock != null)
          {
            flock.release();            
            // If never obtained, try again
            if (System.currentTimeMillis() - time < 256) sleep(32);
            else break;
          }
        }
      }
      catch (Exception e)
      {
        monitor.logger.fine("FileLock : "+e.getMessage());
      }

      monitor.logger.fine("Watcher could not obtain "+type+" lock");
      monitor.onServerDown(this);
    }
  }
}
