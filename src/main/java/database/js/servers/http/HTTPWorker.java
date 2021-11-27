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
import database.js.pools.ThreadPool;


public class HTTPWorker implements Runnable
{
  private final Logger logger;
  private final Handlers handlers;
  private final ThreadPool workers;
  private final HTTPChannel channel;
  private final HTTPRequest request;


  public HTTPWorker(ThreadPool workers, HTTPRequest request) throws Exception
  {
    this.workers  = workers;
    this.request  = request;
    this.channel  = request.channel();
    this.logger   = request.channel().logger();
    this.handlers = channel.config().getHTTP().handlers();

    this.channel.stayalive(true);
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
        this.channel.stayalive(false);
        channel.workers().done();

        return;
      }

      Handler handler = null;
      boolean admin = channel.admin();

      if (admin) handler = handlers.getAdminHandler();
      else       handler = handlers.getHandler(path,method);

      if (handler == null)
      {
        logger.warning("No appropiate handler mapped to path="+path+" method="+method);

        this.workers.done();
        this.channel.failed();
        this.channel.stayalive(false);

        return;
      }

      HTTPResponse response = handler.handle(request);
      if (response != null) request.respond(response.page());

      channel.workers().done();
    }
    catch(Throwable e)
    {
      this.workers.done();
      this.channel.failed();
      logger.log(Level.SEVERE,e.getMessage(),e);
    }
    finally
    {
      this.channel.stayalive(false);
    }
  }
}