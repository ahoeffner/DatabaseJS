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


public class Process
{
  private final String cmd;
  private final String htopt;
  private final String srvopt;
  private final Logger logger;
  
  
  public Process(Config config) throws Exception
  {
    logger = config.getLogger().control;
    String java = config.getJava().exe();
    
    this.htopt = config.getJava().getHttpOptions();
    this.srvopt = config.getJava().getServerOptions();
    String classpath = (String) System.getProperties().get("java.class.path");

    String cmd = java+" -cp "+classpath;
    this.cmd = cmd;
  }
  

  public void start(Type type, int inst)
  {
    String options = null;
    
    if (type == Type.http) options = htopt;
    else                   options = srvopt;

    String cmd = this.cmd + " " + options + " control.Server " + inst;

    try
    {
      byte[] status = new byte[4094];
      logger.info("Starting instance["+inst+"]");
      
      InputStream in = Runtime.getRuntime().exec(cmd).getInputStream();
      int read = in.read(status);
      
      logger.info("Instance["+inst+"]: "+new String(status,0,read)); 
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
