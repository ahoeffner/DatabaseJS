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

package database.js.servers.http;

import java.util.logging.Logger;
import database.js.config.Config;
import database.js.servers.Server;
import java.nio.channels.Selector;
import database.js.pools.ThreadPool;


public class HTTPWaiter extends Thread
{
  private final int threads;
  private final Server server;
  private final Config config;
  private final Logger logger;
  private final boolean embedded;
  private final Selector selector;
  private final ThreadPool workers;
  
  
  public HTTPWaiter(Server server, boolean embedded) throws Exception
  {
    this.server = server;
    this.embedded = embedded;
    this.config = server.config();
    this.selector = Selector.open();
    this.logger = config.getLogger().http;
    this.threads = config.getTopology().threads();

    this.setDaemon(true);
    this.setName("HTTPWaiter");
    this.workers = new ThreadPool(threads);
  }
}
