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
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;


class SSLHandshake extends Thread
{
  private HTTPChannel helper;
  private HTTPBuffers buffers;
  private final Config config;
  private final Logger logger;
  private final boolean twoway;
  private final SelectionKey key;
  private final HTTPServer server;
  private final SocketChannel channel; 
  
  
  SSLHandshake(HTTPServer server, SelectionKey key, SocketChannel channel, boolean twoway) throws Exception
  {
    this.key = key;
    this.twoway = twoway;
    this.server = server;
    this.channel = channel;
    this.config = server.config();
    this.logger = server.logger();
  }
  
  
  SelectionKey key()
  {
    return(key);
  }
  
  
  HTTPChannel helper()
  {
    return(helper);
  }
  
  
  SocketChannel channel()
  {
    return(channel);
  }
  
  
  @Override
  public void run()
  {
    try
    {
      this.buffers = new HTTPBuffers();
      this.helper = new HTTPChannel(config,buffers,channel,true,twoway);
      
      if (this.helper.accept())
      {
        buffers.done();
        server.queue().done(this);
      }
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
    }
  }
}