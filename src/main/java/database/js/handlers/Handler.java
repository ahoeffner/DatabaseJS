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

import java.util.logging.Logger;
import database.js.config.Config;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;


public abstract class Handler
{
  public final Logger logger;
  
  
  public Handler(Config config) throws Exception
  {
    this.logger = config.getLogger().logger;  
  }
  
  
  public abstract HTTPResponse handle(HTTPRequest request) throws Exception;
}
