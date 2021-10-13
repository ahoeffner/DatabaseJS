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

import java.net.Socket;
import java.util.logging.Logger;
import database.js.servers.Server;
import database.js.servers.http.HTTPChannel;


public class RESTClient
{
  private short id = -1;
  private long started = -1;
  
  private final Logger logger;
  private final Server server;
  private final Socket socket;
  private final HTTPChannel channel;


  public RESTClient(HTTPChannel channel, short id) throws Exception
  {
    this.id = id;
    this.channel = channel;
    this.socket = channel.socket();
    this.server = channel.server();
    this.started = System.currentTimeMillis();
    this.logger = server.config().getLogger().rest;
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
  
  
  public void init() throws Exception
  {
    channel.configureBlocking(true);
  }
  
  
  public void send(byte[] buf) throws Exception
  {
    socket.getOutputStream().write(buf);    
  }
}
