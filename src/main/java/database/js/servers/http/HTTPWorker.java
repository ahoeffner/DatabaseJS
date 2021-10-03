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
  private final HTTPChannel server;
  private final HTTPRequest request;


  public HTTPWorker(HTTPChannel server, SelectionKey key, HTTPRequest request) throws Exception
  {
    this.key = key;
    this.server = server;
    this.request = request;
    this.logger = server.logger();
    this.handlers = server.config().getHTTP().handlers();
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
        int ssl = server.config().getHTTP().ssl();
        int plain = server.config().getHTTP().plain();

        String host = request.getHeader("Host");
        host = host.replace(plain+"",ssl+"");
        
        HTTPResponse response = new HTTPResponse();

        response.setResponse(301);
        response.setHeader("Location","https://"+host);

        request.respond(response.page());        
        server.workers().done();
        
        return;
      }

      Handler handler = null;
      boolean admin = server.admin();
      
      if (!admin) handler = handlers.getHandler(path,method);
      else        handler = new AdminHandler(server.config());

      HTTPResponse response = handler.handle(request);
      request.respond(response.page());
      
      server.workers().done();
    }
    catch(Exception e)
    {
      request.failed();
      server.workers().done();
      logger.log(Level.SEVERE,e.getMessage(),e);
    }
  }
}
