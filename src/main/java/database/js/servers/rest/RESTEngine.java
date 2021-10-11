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
import database.js.servers.Server;
import java.nio.channels.SocketChannel;
import database.js.servers.http.HTTPChannel;


public class RESTEngine
{
  private short id = -1;
  private long started = -1;
  
  private final int port;
  private final Logger logger;
  private final Server server;
  private final boolean client;
  private final HTTPChannel channel;
  
  
  public RESTEngine(HTTPChannel channel, short id) throws Exception
  {
    this.id = id;
    this.port = 0;
    this.client = true;
    this.channel = channel;
    this.server = channel.server();
    this.started = System.currentTimeMillis();
    this.logger = server.config().getLogger().rest;
  }
  
  
  public RESTEngine(Server server, int port, boolean ssl) throws Exception
  {
    this.port = port;
    this.client = false;
    this.server = server;
    this.id = server.id();
    this.started = System.currentTimeMillis();
    SocketChannel channel = SocketChannel.open();    
    this.logger = server.config().getLogger().rest;
    this.channel = new HTTPChannel(server,channel,ssl);
  }
  
  
  public short id()
  {
    return(id);
  }
  
  
  Logger logger()
  {
    return(logger);
  }
  
  
  public long started()
  {
    return(started);
  }
}
