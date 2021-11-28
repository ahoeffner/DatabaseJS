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

import database.js.config.Config;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;
import database.js.config.Handlers.HandlerProperties;


public abstract class Handler
{
  private final Config config;
  private final HandlerProperties properties;


  public Handler(Config config, HandlerProperties properties) throws Exception
  {
    this.config = config;
    this.properties = properties;
  }


  public Config config()
  {
    return(config);
  }


  public HandlerProperties properties()
  {
    return(properties);
  }


  public abstract HTTPResponse handle(HTTPRequest request) throws Exception;
}