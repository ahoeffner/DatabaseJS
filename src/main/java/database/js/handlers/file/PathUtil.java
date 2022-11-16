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

package database.js.handlers.file;

import java.io.File;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.handlers.Handler;
import database.js.config.Handlers.HandlerProperties;


public class PathUtil
{
  private final HandlerProperties properties;
  private final Logger logger = Logger.getLogger("http");


  public PathUtil(Handler handler) throws Exception
  {
    this.properties = handler.properties();
  }


  public String getPath(String urlpath)
  {
    String prefix = properties.prefix();

    if (prefix.length() > urlpath.length()) return(null);
    String path = "/"+urlpath.substring(prefix.length());

    path = path.replaceAll("//","/");

    while(path.length() > 1 && path.endsWith("/"))
      path = path.substring(0,path.length()-1);

    return(path);
  }


  public boolean checkPath(String path)
  {
    try
    {
      File p = new File("/mnt"+path);

      if (Config.windows())
      {
        // Remove drive letter
        if (p.getCanonicalPath().substring(2).startsWith("\\mnt"))
          return(true);
      }
      else
      {
        if (p.getCanonicalPath().startsWith("/mnt"))
          return(true);
      }

      logger.warning("Page "+path+" looks insecure");
      return(false);
    }
    catch (Exception e)
    {
      return(false);
    }
  }
}