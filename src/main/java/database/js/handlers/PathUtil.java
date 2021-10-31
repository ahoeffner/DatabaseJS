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

package database.js.handlers;

import database.js.config.Handlers.HandlerProperties;


public class PathUtil
{
  private final HandlerProperties properties;
  
  
  public PathUtil(Handler handler) throws Exception
  {
    this.properties = handler.properties();
  }
  
  
  public String getPath(String urlpath)
  {
    String prefix = properties.prefix();
    String path = "/"+urlpath.substring(prefix.length());
    return(path);
  }
}
