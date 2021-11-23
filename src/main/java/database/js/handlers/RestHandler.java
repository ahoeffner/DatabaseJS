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

import java.net.URL;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.servers.Server;
import database.js.handlers.rest.Rest;
import database.js.handlers.rest.Guid;
import database.js.control.Process.Type;
import database.js.database.DatabaseUtils;
import database.js.servers.rest.RESTClient;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;
import database.js.config.Handlers.HandlerProperties;


public class RestHandler extends Handler
{
  private final PathUtil path;
  private final TreeSet<String> domains;


  public RestHandler(Config config, HandlerProperties properties) throws Exception
  {
    super(config,properties);

    config.loadDatabaseConfig();
    this.path = new PathUtil(this);
    this.domains = new TreeSet<String>();
  }


  @Override
  public HTTPResponse handle(HTTPRequest request) throws Exception
  {
    HTTPResponse response = null;
    Server server = request.server();
    Logger logger = getLogger(Type.rest);

    server.request();
    logger.fine("REST request received: "+request.path());

    if (request.getHeader("Host") == null)
    {
      response = new HTTPResponse();

      response.setResponse(400);
      response.setContentType("text/html");
      response.setBody("<b>Bad Request</b>");

      return(response);
    }

    if (!server.embedded())
    {
      RESTClient client = server.worker();

      if (client == null)
      {
        response = new HTTPResponse();
        response.setContentType("application/json");

        logger.warning("No RESTServer's connected");
        response.setBody("{\"status\": \"failed\", \"message\": \"No RESTServer's connected\"}");

        return(response);
      }

      byte[] data = client.send(request.page());
      response = new HTTPResponse(data);

      log(logger,request,response);
      return(response);
    }

    String path = this.path.getPath(request.path());
    boolean modify = request.method().equals("PATCH");

    response = new HTTPResponse();

    if (path == null)
    {
      response.setResponse(404);
      response.setContentType("text/html");
      response.setBody("<b>Page not found</b>");
      return(response);
    }

    String mode = request.getHeader("Sec-Fetch-Mode");
    if (mode != null && mode.equalsIgnoreCase("cors"))
    {
      String origin = request.getHeader("Origin");

      if (origin == null)
      {
        response.setContentType("application/json");

        logger.warning("Null Cors Origin header detected. Request rejected");
        response.setBody("{\"status\": \"failed\", \"message\": \"Null Cors Origin header detected. Request rejected\"}");

        return(response);
      }

      if (!allow(origin))
      {
        response.setContentType("application/json");

        logger.warning("Origin "+origin+" rejected by Cors");
        response.setBody("{\"status\": \"failed\", \"message\": \"\"Origin \"+origin+\" rejected by Cors\"}");

        return(response);
      }

      response.setHeader("Access-Control-Allow-Headers","*");
      response.setHeader("Access-Control-Request-Method","*");
      response.setHeader("Access-Control-Request-Headers","*");
      response.setHeader("Access-Control-Allow-Origin",origin);
      response.setHeader("Access-Control-Allow-Credentials","true");
    }

    String session = request.getCookie("JSESSIONID");
    if (session == null) session = new Guid().toString();
    response.setCookie("JSESSIONID",session);

    if (request.body() == null && request.method().equals("OPTIONS"))
      return(response);

    byte[] body = request.body();
    if (body == null) body = "{}".getBytes();

    String payload = new String(body);

    Rest rest = new Rest(config(),path,modify,payload);
    response.setContentType("application/json");

    String xx = rest.execute();
    System.out.println(xx);
    response.setBody(xx);

    log(logger,request,response);
    return(response);
  }


  private boolean allow(String origin) throws Exception
  {
    if (this.domains.contains(origin)) return(true);
    ArrayList<String> corsheaders = config().getHTTP().corsdomains();

    URL url = new URL(origin);

    origin = url.getHost();
    String host = config().getHTTP().host();

    if (origin.equals(host))
    {
      this.domains.add(origin);
      return(true);
    }

    origin = "." + origin + ".";
    for(String pattern : corsheaders)
    {
      pattern = pattern.replace(".","\\.");
      pattern = pattern.replace("*",".*");

      if (origin.matches(".*"+pattern+".*"))
      {
        this.domains.add(origin);
        return(true);
      }
    }

    return(false);
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