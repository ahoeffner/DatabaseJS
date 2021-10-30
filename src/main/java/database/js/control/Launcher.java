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

package database.js.control;

import java.io.File;
import java.util.Date;
import java.util.ArrayList;
import org.json.JSONTokener;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.admin.Client;
import database.js.config.Paths;
import database.js.config.Config;
import java.text.SimpleDateFormat;
import database.js.cluster.Cluster;
import database.js.handlers.Handler;
import database.js.cluster.Statistics;


/**
 *
 * In case there are no json-lib in classpath, the launcher will dynamically load it.
 * If the loader fails (java 1.8) it starts a new process with appropiate classpath.
 *
 * It then reads the topologi and starts the servers as per config.
 *
 */
public class Launcher implements ILauncher
{
  private Config config = null;
  private Logger logger = null;
  private final static String psep = File.pathSeparator;


  public static void main(String[] args) throws Exception
  {
    String cmd = null;
    ILauncher launcher = null;
    
    if (version() < 13)
    {
      System.out.println("database.js requires java version > 13.0");
      System.exit(-1);
    }
    
    if (args.length != 1)
      usage();

    if (!testcp())
    {
      launcher = create();

      if (launcher == null)
      {
        start(args);
        return;
      }
    }

    if (launcher == null)
      launcher = new Launcher();

    try
    {
      launcher.setConfig();

      switch(cmd)
      {
        case "stop": launcher.stop();  break;
        case "start": launcher.start();  break;
        case "status": launcher.status();  break;

        default: usage();
      }
    }
    catch (Exception e)
    {
      launcher.log(e);
      throw e;
    }
  }


  private static void usage()
  {
    System.out.println("usage database.js start|stop|status");
    System.exit(-1);
  }
  
  
  public static int version()
  {
    String version = System.getProperty("java.version");
    int mvers = Integer.parseInt(version.substring(0,version.indexOf('.')));
    return(mvers);
  }
  
  
  public Logger logger()
  {
    return(logger);
  }


  public void setConfig() throws Exception
  {
    this.config = new Config();
    config.getLogger().openControlLog();
    this.logger = config.getLogger().control;
  }


  public void stop() throws Exception
  {
    logger.fine("Shutting down");
    Cluster.init(config); Cluster.stop();
    Thread.sleep((int) (1.25*config.getTopology().heartbeat()));
  }


  public void start() throws Exception
  {
    Cluster.init(config);
    config.getJava().exe();

    if (Cluster.isRunning((short) 0))
    {
      logger.info("database.js instance "+config.instance()+" is already running");
      return;
    }

    Process process = new Process(config);
    process.start(Process.Type.http,0);
  }


  public void status() throws Exception
  {
    String line = null;
    System.out.println();

    SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    ArrayList<Statistics> statistics = Cluster.getStatistics(config);

    String hid = String.format("%3s"," id");
    String hpid = String.format("%8s"," pid ");
    String hhits = String.format("%12s","hits  ");

    String htype = String.format("%-8s"," type");

    String hused = String.format("%-9s"," used");
    String halloc = String.format("%-9s"," alloc");
    String htotal = String.format("%-10s"," total");

    String hstarted = String.format("%-21s","  started");
    String hupdated = String.format("%-13s","    uptime");

    // Memory

    System.out.println("Memory in MB");
    line = String.format("%40s"," ").replace(" ","-");

    System.out.println(line);
    System.out.println("|"+hid+" |"+htotal+" |"+halloc+" |"+hused+" |");
    System.out.println(line);

    for (Statistics stats : statistics)
    {
      if (!stats.online()) continue;

      long alloc = stats.usedmem() + stats.freemem();

      String id = String.format(" %2s ",stats.id());
      String am = String.format(" %8s ",alloc/(1024*1024));
      String tm = String.format(" %9s ",stats.totmem()/(1024*1024));
      String um = String.format(" %8s ",stats.usedmem()/(1024*1024));

      System.out.print("|"+id+"");
      System.out.print("|"+tm+"");
      System.out.print("|"+am+"");
      System.out.print("|"+um+"");

      System.out.print("|");
      System.out.print(System.lineSeparator());
    }

    System.out.println(line);
    System.out.println();


    // Processes

    System.out.println("Processes");
    line = String.format("%77s"," ").replace(" ","-");

    System.out.println(line);
    System.out.println("|"+hid+" |"+hpid+" |"+htype+" |"+hstarted+" |"+hupdated+" |"+hhits+" |");
    System.out.println(line);

    for (Statistics stats : statistics)
    {
      if (!stats.online()) continue;

      String id = String.format(" %2s ",stats.id());
      String pid = String.format("%8s ",stats.pid());
      String hits = String.format("%12s ",stats.requests());

      String type = stats.http() ? "http" : "rest";
      if (stats.httpmgr() || stats.restmgr()) type += "(+)";
      type = String.format(" %-8s",type);

      int up = (int) ((stats.updated() - stats.started())/1000);

      int days = up/(24*3600);
      up -= days * 24*3600;

      int hours = up/3600;
      up -= hours * 3600;

      int mins = up/60;
      up -= mins * 60;

      int secs = up;

      String uptime = " ";
      uptime += String.format("%3d",days) + " ";
      uptime += String.format("%2d",hours).replace(' ','0') + ":";
      uptime += String.format("%2d",mins) .replace(' ','0') + ":";
      uptime += String.format("%2d",secs) .replace(' ','0') + " ";

      String started = " "+format.format(new Date(stats.started()))+" ";

      System.out.print("|"+id+"");
      System.out.print("|"+pid+"");
      System.out.print("|"+type+"");
      System.out.print("|"+started+"");
      System.out.print("|"+uptime+"");
      System.out.print("|"+hits+"");

      System.out.print("|");
      System.out.print(System.lineSeparator());
    }

    System.out.println(line);
    System.out.println();
  }


  public void log(Exception e)
  {
    if (logger != null)
      logger.log(Level.SEVERE,e.getMessage(),e);
  }


  // Test json in classpath
  private static boolean testcp()
  {
    try {new JSONTokener("{}");}
    catch (Throwable e) {return(false);}
    return(true);
  }


  @SuppressWarnings("unchecked")
  static ILauncher create() throws Exception
  {
    ILauncher launcher = null;

    try
    {
      String path = Paths.libdir+File.separator+"json";
      Loader loader = new Loader(ILauncher.class, Handler.class, Paths.class);

      File dir = new File(path);
      String[] jars = dir.list();

      for(String jar : jars)
        loader.load(path + File.separator + jar);

      String classpath = (String) System.getProperties().get("java.class.path");

      jars = classpath.split(psep);

      for(String jar : jars)
        loader.load(jar);

      Class Launcher = loader.getClass(Launcher.class);
      launcher = (ILauncher) Launcher.getDeclaredConstructor().newInstance();

      return(launcher);
    }
    catch (Exception e)
    {
      return(null);
    }
  }


  public static void start(String[] args) throws Exception
  {
    String classpath = (String) System.getProperties().get("java.class.path");

    String home = System.getProperties().getProperty("java.home");
    String bindir = home + File.separator + "bin" + File.separator;

    String exe = bindir + "java";
    if (Config.windows()) exe += ".exe";

    String path = Paths.libdir+File.separator+"json";

    File dir = new File(path);
    String[] jars = dir.list();

    for(String jar : jars)
      classpath += psep + path + File.separator + jar;

    String argv = "";
    for(String arg : args) argv += " "+arg;

    String cmd = exe + " -cp " + classpath + " database.js.control.Launcher -s " + argv;
    Runtime.getRuntime().exec(cmd);
  }
}
