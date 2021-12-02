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
  private final String opts;
  private final String jars;
  private final String httpopts;
  private final String restopts;
  private final String httpjars;
  private final String restjars;


  Java(JSONObject config) throws Exception
  {
    this.opts = Config.get(config,"opts","");
    this.httpopts = Config.get(config,"http.opts","");
    this.restopts = Config.get(config,"rest.opts","");

    String exe = Config.get(config,"java",current());
    this.exe = exe + (Config.windows() ? ".exe" : "");

    String srvjars = Config.get(config,"jars","");

    if (srvjars.length() > 0)
    {
      String path = "";
      String[] jars = srvjars.split(", ;:");

      for(String jar : jars)
        path += File.pathSeparator+jar;

      srvjars = path;
    }


    String httpjars = Config.get(config,"http.jars","");

    if (httpjars.length() > 0)
    {
      String path = "";
      String[] jars = httpjars.split(", ;:");

      for(String jar : jars)
        path += File.pathSeparator+jar;

      httpjars = path;
    }


    String restjars = Config.get(config,"rest.jars","");

    if (restjars.length() > 0)
    {
      String path = "";
      String[] jars = restjars.split(", ;:");

      for(String jar : jars)
        path += File.pathSeparator+jar;

      restjars = path;
    }

    this.jars = srvjars;
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

  public String getClassPath()
  {
    return(jars);
  }

  public String getOptions()
  {
    return(opts);
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