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

package database.js.config;

import java.io.File;
import org.json.JSONObject;


public class Java
{
  private final String exe;
  private final String http;
  private final String server;
  
  
  Java(JSONObject config) throws Exception
  {
    String exe = null;
    
    if (!config.isNull("java")) 
      exe = config.getString("java");

    if (exe == null) exe = current();
    if (Config.windows()) exe += ".exe";
    
    this.exe = exe;
    this.http = config.getString("http");
    this.server = config.getString("server");
  }
  
  
  private String current()
  {
    String home = System.getProperties().getProperty("java.home");
    String bindir = home + File.separator + "bin" + File.separator;
    return(bindir + "java");
  }


  public String exe()
  {
    return(exe);
  }

  public String getHttpOptions()
  {
    return(http);
  }

  public String getServerOptions()
  {
    return(server);
  }
}
