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
import org.json.JSONTokener;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.admin.Client;
import database.js.config.Paths;
import database.js.config.Config;


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
  private final static String psep = System.getProperty("path.separator");


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
      Loader loader = new Loader(ILauncher.class, Paths.class);

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
