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
    boolean silent = false;
    ILauncher launcher = null;

    switch(args.length)
    {
      case 0: usage(); break;
      case 1: cmd = args[0].toLowerCase(); break;
      case 2: silent = true; cmd = args[1].toLowerCase(); break;
    }

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
    System.out.println("usage database.js start|stop|status|reset");
    System.exit(-1);
  }
  
  
  public void setConfig() throws Exception
  {
    this.config = new Config();
    config.getLogger().openControlLog();
    this.logger = config.getLogger().control;
  }


  public void stop() throws Exception
  {
    int admin = config.getPorts()[2];
    Client client = new Client("localhost",admin);
    
    client.connect();
    client.send("shutdown");
  }


  public void start() throws Exception
  {
    config.getJava().exe();
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
    String hhits = String.format("%8s"," hits  ");

    String hmgr = String.format("%4s"," mgr");
    String hsec = String.format("%4s"," sec");

    String hused = String.format("%-9s"," used");
    String halloc = String.format("%-9s"," alloc");
    String htotal = String.format("%-10s"," total");

    String hstarted = String.format("%-21s","  started");
    String hupdated = String.format("%-21s","  updated");

    // Memory
    
    System.out.println("Memory in MB");
    line = String.format("%40s"," ").replace(" ","-");

    System.out.println(line);
    System.out.println("|"+hid+" |"+htotal+" |"+halloc+" |"+hused+" |");    
    System.out.println(line);

    for (Statistics stats : statistics)
    {
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
    line = String.format("%84s"," ").replace(" ","-");

    System.out.println(line);
    System.out.println("|"+hid+" |"+hpid+" |"+hmgr+" |"+hsec+" |"+hstarted+" |"+hupdated+" |"+hhits+" |");    
    System.out.println(line);
    
    for (Statistics stats : statistics)
    {
      String id = String.format(" %2s ",stats.id());
      String pid = String.format("%8s ",stats.pid());
      String hits = String.format("%8s ",stats.requests());
      
      String mgr = stats.manager() ? "  X  " : "     ";
      String sec = stats.secretary() ? "  X  " : "     ";

      String started = " "+format.format(new Date(stats.started()))+" ";
      String updated = " "+format.format(new Date(stats.updated()))+" ";

      System.out.print("|"+id+"");
      System.out.print("|"+pid+"");
      System.out.print("|"+mgr+"");
      System.out.print("|"+sec+"");
      System.out.print("|"+started+"");
      System.out.print("|"+updated+"");
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
  private static ILauncher create() throws Exception
  {
    ILauncher launcher = null;

    try
    {
      // Doesn't work with java 1.8
      Loader loader = new Loader(ILauncher.class, Handler.class, Paths.class);

      String path = Paths.libdir+File.separator+"json";
      String classpath = (String) System.getProperties().get("java.class.path");

      File dir = new File(path);
      String[] jars = dir.list();

      for(String jar : jars)
        loader.load(path + File.separator + jar);

      path = Paths.libdir+File.separator+"ipc";
      dir = new File(path); jars = dir.list();

      for(String jar : jars)
        loader.load(path + File.separator + jar);

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
