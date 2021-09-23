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

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Config;

import database.js.config.Paths;

import java.io.File;


public class Process
{
  private final String cmd;
  private final String htopt;
  private final String srvopt;
  private final Logger logger;
  private final static String psep = System.getProperty("path.separator");
  
  
  public Process(Config config, String classpath) throws Exception
  {
    logger = config.getLogger().control;
    String java = config.getJava().exe();
    
    this.htopt = config.getJava().getHttpOptions();
    this.srvopt = config.getJava().getServerOptions();

    String cmd = java+" -cp "+classpath;
    this.cmd = cmd;
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
    
    String cmd = exe + " -cp " + classpath + " database.js.control.Launcher" + argv;
    Runtime.getRuntime().exec(cmd);
  }
  

  public void start(Type type, int inst)
  {
    String options = null;
    
    if (type == Type.http) options = htopt;
    else                   options = srvopt;

    String cmd = this.cmd + " " + options + " database.js.control.Server " + inst;

    try
    {
      byte[] status = new byte[4094];
      logger.info("Starting instance["+inst+"]");
      
      InputStream in = Runtime.getRuntime().exec(cmd).getInputStream();
      int read = in.read(status);
      
      if (read > 0) logger.info("Instance["+inst+"]: "+new String(status,0,read)); 
      in.close();
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,null,e);
    } 
  }
  
  
  public static enum Type
  {
    http,
    rest,
    micro
  }
}
