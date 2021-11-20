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

package database.js.handlers;

import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.servers.Server;
import database.js.database.Database;
import database.js.handlers.rest.Rest;
import database.js.control.Process.Type;
import database.js.servers.rest.RESTClient;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;
import database.js.config.Handlers.HandlerProperties;


public class RestHandler extends Handler
{
  private final PathUtil path;


  public RestHandler(Config config, HandlerProperties properties) throws Exception
  {
    super(config,properties);

    Database.init(config);
    this.path = new PathUtil(this);
  }


  @Override
  public HTTPResponse handle(HTTPRequest request) throws Exception
  {
    HTTPResponse response = null;
    Server server = request.server();
    Logger logger = getLogger(Type.rest);

    server.request();
    logger.fine("REST request received: "+request.path());

    if (!server.embedded())
    {
      RESTClient client = server.worker();

      if (client == null)
      {
        response = new HTTPResponse();
        logger.warning("No RESTServer's connected");
        response.setBody("{\"status\": \"failed\"}");
        return(response);
      }

      byte[] data = client.send(request.page());
      response = new HTTPResponse(data);

      log(logger,request,response);
      return(response);
    }

    String payload = new String(request.body());
    String path = this.path.getPath(request.path());
    boolean modify = request.method().equals("PATCH");

    Rest rest = new Rest(config(),path,modify,payload);

    response = new HTTPResponse();
    String xx = rest.execute();
    System.out.println(xx);
    response.setBody(xx);

    log(logger,request,response);
    return(response);
  }


  private void log(Logger logger, HTTPRequest request, HTTPResponse response)
  {
    long time = System.nanoTime() - request.start();

    if (logger.getLevel() == Level.FINE)
      logger.log(logger.getLevel(),request.path()+" ["+time/1000000+"]ms");

    if (logger.getLevel() == Level.FINER)
      logger.log(logger.getLevel(),request.path()+" ["+time/1000000+"]ms\n\n"+new String(request.body())+"\n\n"+new String(response.body())+"\n");

    if (logger.getLevel() == Level.FINEST)
      logger.log(logger.getLevel(),request.path()+" ["+time/1000000+"]ms\n\n"+new String(request.page())+"\n\n"+new String(response.page())+"\n");
  }
}