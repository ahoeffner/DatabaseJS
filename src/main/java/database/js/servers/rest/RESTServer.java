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

package database.js.servers.rest;

import java.util.logging.Logger;
import database.js.config.Config;
import database.js.servers.Server;
import database.js.pools.ThreadPool;


public class RESTServer
{
  private final short rid;
  private final Server server;
  private final Config config;
  private final Logger logger;
  private final RESTEngine engine;
  private final ThreadPool workers;
  
  
  public static void main(String[] args) throws Exception
  {
    Server.main(new String[] {"3"});
  }

  
  public RESTServer(Server server) throws Exception
  {
    this.server = server;
    this.config = server.config();
    this.logger = config.getLogger().rest;
    
    int port = config.getHTTP().admin();
    
    int http = 1;
    if (config.getTopology().hotstandby()) http++;
    
    this.rid = (short) (server.id() - http);
    this.engine = new RESTEngine(server,port,true);
    this.workers = new ThreadPool(config.getTopology().workers());
  }
}
