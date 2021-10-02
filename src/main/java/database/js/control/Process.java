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
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Paths;
import database.js.config.Config;


public class Process
{
  private final String java;
  private final String htopt;
  private final String srvopt;
  private final Config config;
  private final Logger logger;
  private final static String psep = System.getProperty("path.separator");


  public Process(Config config) throws Exception
  {
    this.config = config;
    this.java = config.getJava().exe();
    this.logger = config.getLogger().control;
    this.htopt = config.getJava().getHttpOptions();
    this.srvopt = config.getJava().getServerOptions();
  }


  public void start(Type type, int inst)
  {
    String options = null;

    if (type == Type.http) options = htopt;
    else                   options = srvopt;
    
    String classpath = classpath(type != Type.http);
    String cmd = this.java + " -cp " + classpath + " " + options + " database.js.servers.Server " + inst;

    try
    {
      java.lang.Process p = Runtime.getRuntime().exec(cmd);
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,null,e);
    }
  }


  private String classpath(boolean jdbc)
  {
    String classpath = "";
    String path = Paths.libdir;

    File dir = new File(path);
    String[] jars = dir.list();
    
    for(String jar : jars)
    {
      if (jar.startsWith("database.js"))
        classpath = path + File.separator + jar;
    }
    
    classpath += classpath("ipc");
    classpath += classpath("json");
    if (jdbc) classpath += classpath("json");
    
    return(classpath);
  }
  
  
  private String classpath(String sub)
  {
    String classpath = "";
    String path = Paths.libdir+File.separator+sub;

    File dir = new File(path);
    String[] jars = dir.list();

    for(String jar : jars)
      classpath += psep + path + File.separator + jar;
    
    return(classpath);
  }


  public static enum Type
  {
    http,
    rest
  }
}
