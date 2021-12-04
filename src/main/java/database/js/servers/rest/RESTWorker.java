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

import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.servers.Server;
import database.js.config.Handlers;
import database.js.pools.ThreadPool;
import database.js.handlers.RestHandler;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;


public class RESTWorker implements Runnable
{
  private final Logger logger;
  private final RESTComm bridge;
  private final RESTServer rserver;
  private final ThreadPool workers;


  public RESTWorker(RESTServer rserver, ThreadPool workers, RESTComm bridge)
  {
    this.bridge = bridge;
    this.rserver = rserver;
    this.workers = workers;
    this.logger = rserver.logger();
  }


  @Override
  public void run()
  {
    try
    {
      Server srv = rserver.server();

      String host = new String(bridge.host);
      HTTPRequest request = new HTTPRequest(srv,host,bridge.page());

      Handlers handlers = rserver.config().getHTTP().handlers;
      RestHandler handler = handlers.getRESTHandler();

      HTTPResponse response = handler.handle(request);
      byte[] data = response.page();

      if (data == null)
      {
        logger.severe("Received null respond from RestHandler");
        data = "{\"status\": \"failed\"}".getBytes();
      }

      long id = bridge.id();
      int extend = bridge.extend();

      RESTComm bridge = new RESTComm(id,extend,host.getBytes(),data);
      rserver.respond(bridge);
    }
    catch (Exception e)
    {
      this.workers.done();
      logger.log(Level.SEVERE,e.getMessage(),e);
      
      byte[] data = ("{\"status\": \""+e.getMessage()+"\"}").getBytes();
      RESTComm error = new RESTComm(bridge.id(),bridge.extend(),bridge.host(),data);
      rserver.respond(bridge);
    }
  }
}