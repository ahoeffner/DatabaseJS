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
  private final String httpopts;
  private final String restopts;
  private final String httpjars;
  private final String restjars;


  Java(JSONObject config) throws Exception
  {
    String exe = null;

    if (!config.isNull("java"))
      exe = config.getString("java");

    if (exe == null) exe = current();
    if (Config.windows()) exe += ".exe";

    this.exe = exe;
    this.httpopts = config.getString("http.opts");
    this.restopts = config.getString("rest.opts");

    String httpjars = "";
    if (!config.isNull("http.jars"))
    {
      String path = "";
      httpjars = config.getString("http.jars");
      String[] jars = httpjars.split(", ;:");

      for(String jar : jars)
        path += File.pathSeparator+jar;

      httpjars = path;
    }

    String restjars = "";
    if (!config.isNull("http.jars"))
    {
      String path = "";
      restjars = config.getString("rest.jars");
      String[] jars = restjars.split(", ;:");

      for(String jar : jars)
        path += File.pathSeparator+jar;

      restjars = path;
    }

    this.httpjars = httpjars;
    this.restjars = restjars;
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

  public String getHTTPClassPath()
  {
    return(httpjars);
  }

  public String getRESTClassPath()
  {
    return(restjars);
  }

  public String getHttpOptions()
  {
    return(httpopts);
  }

  public String getRestOptions()
  {
    return(restopts);
  }
}