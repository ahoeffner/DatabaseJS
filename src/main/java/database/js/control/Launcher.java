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

import java.net.URL;
import java.io.File;
import java.util.ArrayList;
import org.json.CookieList;
import java.net.URLClassLoader;
import database.js.config.Paths;
import database.js.config.Config;
import database.js.config.Topology;
import database.js.handlers.Handler;

import org.json.JSONTokener;


/**
 *
 * In case there are no json-lib in classpath, the launcher will dynamically load it.
 * It then reads the topologi and starts the servers as per config.
 *
 */
public class Launcher implements ILauncher
{
  private final static String psep = System.getProperty("path.separator");
  

  public static void main(String[] args) throws Exception
  {
    if (args.length != 1)
      usage();
    
    ILauncher launcher = createII();
    String cmd = args[0].toLowerCase();
    
    switch(cmd)
    {
      case "start": launcher.startProcesses();  break;
      
      default: usage();
    }
    
  }
  
  
  private static void usage()
  {
    System.out.println("usage database.js start|stop|status");
    System.exit(-1);
  }


  public void startProcesses() throws Exception
  {
    String cp = classpath();
    Config config = new Config();    
    config.getLogger().openControlLog();
    Process process = new Process(config,cp);
    Topology topology = config.getTopology();
    
    if (topology.type() == Topology.Type.Micro)
    {
      process.start(Process.Type.http,0);

      if (topology.hotstandby())
        process.start(Process.Type.http,1);
    }
  }


  @SuppressWarnings("unchecked")
  private static ILauncher create() throws Exception
  {
    ILauncher launcher = null;

    try
    {
      // Test json-lib
      new CookieList();
      launcher = new Launcher();
    }
    catch (Throwable e)
    {
      try
      {
        Loader loader = new Loader(ILauncher.class, Handler.class);

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
      }
      catch (Exception le)
      {
        throw le;
      }
    }

    return(launcher);
  }


  @SuppressWarnings("unchecked")
  private static ILauncher createII() throws Exception
  {
    ILauncher launcher = null;

    try
    {
      // Test json-lib
      new CookieList();
      launcher = new Launcher();
    }
    catch (Throwable e)
    {
      ArrayList<String> cp = new ArrayList<String>();
      
      String path = Paths.libdir+File.separator+"json";
      String classpath = (String) System.getProperties().get("java.class.path");

      File dir = new File(path);
      String[] jars = dir.list();

      for(String jar : jars)
        cp.add(path + File.separator + jar);

      path = Paths.libdir+File.separator+"ipc";
      dir = new File(path); jars = dir.list();

      for(String jar : jars)
        cp.add(path + File.separator + jar);

      jars = classpath.split(psep);

      for(String jar : jars)
        cp.add(jar);
      
      URL[] urls = new URL[cp.size()];
      
      for (int i = 0; i < cp.size(); i++)
        urls[i] = new URL("file://"+cp.get(i));
      
      URLClassLoader loader = new URLClassLoader(urls);

      Class Test = loader.loadClass("database.js.config.Config");
      System.out.println("test "+Test);

      Class Launcher = loader.loadClass(Launcher.class.getName());
      launcher = (ILauncher) Launcher.getDeclaredConstructor().newInstance();
    }

    return(launcher);
  }
  
  
  public static String classpath() throws Exception
  {
    return(classpath(Paths.libdir).substring(1));
  }
  
  
  private static String classpath(String libdir) throws Exception
  {
    String cpath = "";
    File dir = new File(libdir);
    String[] content = dir.list();
    
    for(String c : content)
    {
      String fp = libdir+File.separator+c;

      File f = new File(fp);
      
      if (f.isFile()) cpath += psep + fp;
      else            cpath += classpath(fp);
    }
    
    return(cpath);
  }
}
