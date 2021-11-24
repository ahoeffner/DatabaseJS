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

import java.util.logging.Logger;
import database.js.config.Config;
import database.js.servers.Server;
import database.js.control.Launcher;
import database.js.servers.rest.RESTClient;
import database.js.handlers.file.Deployment;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;
import database.js.config.Handlers.HandlerProperties;


public class AdminHandler extends Handler
{
  public AdminHandler(Config config, HandlerProperties properties) throws Exception
  {
    super(config,properties);
  }


  @Override
  public HTTPResponse handle(HTTPRequest request) throws Exception
  {
    Logger logger = getAdminLogger();
    Server server = request.server();
    HTTPResponse response = new HTTPResponse();

    server.request();
    logger.fine("adm request received <"+request.path()+">");

    if (request.path().equals("/connect"))
    {
      request.unlist();
      request.channel().stayalive(true);

      String body = new String(request.body());
      response.setBody(server.id()+" "+server.started());

      String[] args = body.split(" ");
      short id = Short.parseShort(args[0]);
      long started = Long.parseLong(args[1]);

      RESTClient worker = server.worker(id);

      if (worker == null) logger.info("RESTServer connecting");
      else logger.fine("RESTServer connecting secondary channel");

      if (worker == null || started != worker.started())
        worker = new RESTClient(server,id,started);

      server.register(worker);
      request.respond(response.page());

      worker.init(request.channel());
      return(null);
    }

    if (request.path().equals("/shutdown"))
      server.shutdown();

    if (request.path().equals("/deploy"))
      Deployment.get().deploy();

    if (request.path().equals("/status"))
    {
      String status = Launcher.getStatus(config());
      response.setBody(status);
    }

    return(response);
  }
}