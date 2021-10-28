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

import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.servers.Server;
import database.js.pools.ThreadPool;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;


class SSLHandshake extends Thread
{
  private final Config config;
  private final Logger logger;
  private final boolean admin;
  private final SelectionKey key;
  private final HTTPServer httpserv;
  private final SocketChannel channel;


  SSLHandshake(HTTPServer httpserv, SelectionKey key, SocketChannel channel, boolean admin) throws Exception
  {
    this.key = key;
    this.admin = admin;
    this.channel = channel;
    this.httpserv = httpserv;
    this.config = httpserv.config();
    this.logger = httpserv.logger();
  }


  @Override
  public void run()
  {
    try
    {
      Server server = httpserv.server();
      ThreadPool workers = httpserv.workers();
      HTTPChannel client = new HTTPChannel(server,workers,channel,true,admin);

      if (client.accept()) 
        httpserv.assign(client);
      
      httpserv.workers().done();
    }
    catch (Exception e)
    {
      httpserv.workers().done();
      logger.log(Level.SEVERE,e.getMessage(),e);
    }
  }
}