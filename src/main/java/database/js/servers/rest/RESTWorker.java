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
      HTTPRequest request = new HTTPRequest(srv,bridge.data());
      Handlers handlers = rserver.config().getHTTP().handlers();
      
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
      
      RESTComm bridge = new RESTComm(id,extend,data);
      rserver.respond(bridge);
    }
    catch (Exception e)
    {
      this.workers.done();
      logger.log(Level.SEVERE,e.getMessage(),e);
    }
  }
}
