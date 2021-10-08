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
import database.js.config.Handlers;
import database.js.handlers.Handler;
import java.nio.channels.SelectionKey;
import database.js.handlers.AdminHandler;


public class HTTPWorker implements Runnable
{
  private final Logger logger;
  private final SelectionKey key;
  private final Handlers handlers;
  private final HTTPChannel channel;
  private final HTTPRequest request;


  public HTTPWorker(SelectionKey key, HTTPRequest request) throws Exception
  {
    this.key = key;
    this.request = request;
    this.channel = request.channel();
    this.logger = channel.logger();
    this.handlers = channel.config().getHTTP().handlers();
  }


  @Override
  public void run()
  {
    try
    {
      request.parse();
      String path = request.path();
      String method = request.method();
            
      if (request.redirect())
      {
        int ssl = channel.config().getHTTP().ssl();
        int plain = channel.config().getHTTP().plain();

        String host = request.getHeader("Host");
        host = host.replace(plain+"",ssl+"");
        
        HTTPResponse response = new HTTPResponse();

        response.setResponse(301);
        response.setHeader("Location","https://"+host);

        request.respond(response.page());        
        channel.workers().done();
        
        return;
      }

      Handler handler = null;
      boolean admin = channel.admin();
      
      if (!admin) handler = handlers.getHandler(path,method);
      else        handler = new AdminHandler(channel.config());

      HTTPResponse response = handler.handle(request);
      request.respond(response.page());
      
      channel.workers().done();
    }
    catch(Exception e)
    {
      request.failed();
      channel.workers().done();
      logger.log(Level.SEVERE,e.getMessage(),e);
    }
  }
}
