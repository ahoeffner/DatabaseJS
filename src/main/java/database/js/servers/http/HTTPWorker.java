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

import database.js.config.Handlers;
import database.js.handlers.Handler;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


public class HTTPWorker implements Runnable
{
  private final Handlers handlers;
  private final HTTPServer server;
  private final HTTPRequest request;
  
  
  public HTTPWorker(HTTPServer server, HTTPRequest request) throws Exception
  {
    this.server = server;
    this.request = request;
    this.handlers = server.config().getHTTP().handlers();
  }


  @Override
  public void run()
  {
    String path = request.path();
    String method = request.method();
    Handler handler = handlers.getHandler(path, method);
    
    try
    {
      HTTPResponse response = handler.handle(request);
      
      SocketChannel channel = request.channel();
      channel.write(ByteBuffer.wrap(response.body()));
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }    
  }
}
