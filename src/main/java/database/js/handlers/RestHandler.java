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
import java.util.Base64;
import java.util.TreeSet;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.servers.Server;
import database.js.security.OAuth;
import database.js.database.Database;
import database.js.handlers.rest.Rest;
import database.js.handlers.rest.Guid;
import database.js.handlers.file.PathUtil;
import database.js.servers.rest.RESTClient;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;
import database.js.config.Handlers.HandlerProperties;


public class RestHandler extends Handler
{
  private final PathUtil path;
  private final TreeSet<String> domains;
  private final static Logger logger = Logger.getLogger("rest");


  public RestHandler(Config config, HandlerProperties properties) throws Exception
  {
    super(config,properties);
    this.path = new PathUtil(this);
    this.domains = new TreeSet<String>();
  }


  @Override
  public HTTPResponse handle(HTTPRequest request) throws Exception
  {
    HTTPResponse response = null;
    Server server = request.server();

    String html = config().getHTTP().mimetypes.get("html");
    String json = config().getHTTP().mimetypes.get("json");

    server.request();
    logger.finest("REST request received: "+request.path());

    if (request.getHeader("Host") == null)
    {
      response = new HTTPResponse();

      response.setResponse(400);
      response.setContentType(html);
      response.setBody("<b>Bad Request</b>");

      return(response);
    }

    if (!server.embedded())
    {
      RESTClient client = null;
      short rsrv = getClient(config(),request);

      if (rsrv < 0) client = server.worker();
      else          client = server.worker(rsrv);

      if (client == null)
      {
        response = new HTTPResponse();
        response.setContentType(json);

        logger.warning("No RESTServer's connected");
        response.setBody("{\"status\": \"failed\", \"message\": \"No RESTServer's connected\"}");

        return(response);
      }

      String host = request.remote();
      byte[] data = client.send(host,request.page());

      response = new HTTPResponse(data);
      return(response);
    }

    response = new HTTPResponse();
    setClient(config(),request,response);

    String path = this.path.getPath(request.path());

    boolean modify = false;
    if (request.method().equals("PATCH")) modify = true;
    if (request.method().equals("DELETE")) modify = true;

    if (path == null)
    {
      response.setResponse(404);
      response.setContentType(html);
      response.setBody("<b>Page not found</b>");
      return(response);
    }

    String mode = request.getHeader("Sec-Fetch-Mode");
    if (mode != null && mode.equalsIgnoreCase("cors"))
    {
      String origin = request.getHeader("Origin");

      if (origin == null)
      {
        response.setContentType(json);

        logger.warning("Null Cors Origin header detected. Request rejected");
        response.setBody("{\"status\": \"failed\", \"message\": \"Null Cors Origin header detected. Request rejected\"}");

        return(response);
      }

      if (!allow(origin))
      {
        response.setContentType(json);

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

    String remote = request.remote();
    String payload = new String(body);

    Rest rest = new Rest(server,modify,remote);
    response.setContentType(json);

    response.setBody(rest.execute(path,payload));

    log(logger,request,response);
    return(response);
  }


  private boolean allow(String origin) throws Exception
  {
    if (this.domains.contains(origin)) return(true);
    ArrayList<String> corsheaders = config().getHTTP().corsdomains;

    URL url = new URL(origin);

    origin = url.getHost();
    String host = config().getHTTP().host;

    int pos = host.indexOf(':');
    if (pos > 0) host = host.substring(0,pos);

    if (origin.startsWith("http://"))
      origin = origin.substring(7);

    if (origin.startsWith("https://"))
      origin = origin.substring(8);

    pos = origin.indexOf(':');
    if (pos > 0) origin = origin.substring(0,pos);

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


  public static short getClient(Config config, HTTPRequest request) throws Exception
  {
    Server server = request.server();

    long date = 0;
    short rsrv = -1;
    String cinst = null;
    String instance = config.instance();
    String cookie = request.getCookie("RESTSRVID");

    if (cookie == null)
      return(-1);

    byte[] bytes = Base64.getDecoder().decode(cookie);
    ByteBuffer buffer = ByteBuffer.allocate(bytes.length);

    buffer.put(bytes);
    buffer.flip();

    date = buffer.getLong();
    rsrv = buffer.getShort();

    byte[] inst = new byte[bytes.length-10];

    buffer.get(inst);
    cinst = new String(inst);

    if (date >= server.started() && cinst.equals(instance))
      return(rsrv);

    return(-1);
  }


  public static void setClient(Config config, HTTPRequest request, HTTPResponse response) throws Exception
  {
    Server server = request.server();
    if (!server.isRestType()) return;

    short rsrv = server.id();
    long date = server.started();
    byte[] instance = config.instance().getBytes();

    ByteBuffer buffer = ByteBuffer.allocate(10+instance.length);

    buffer.putLong(date);
    buffer.putShort(rsrv);
    buffer.put(instance);

    byte[] cookie = Base64.getEncoder().encode(buffer.array());
    response.setCookie("RESTSRVID",new String(cookie));
  }


  private static String req = "\n----------------- request -----------------\n";
  private static String rsp = "\n----------------- response -----------------\n";
  private static String end = "\n---------------------------------------------\n";

  private void log(Logger logger, HTTPRequest request, HTTPResponse response)
  {
    long time = System.nanoTime() - request.start();

    if (logger.getLevel() == Level.INFO)
      logger.log(logger.getLevel(),request.path()+" ["+time/1000000+"]ms");

    if (logger.getLevel() == Level.FINE)
      logger.log(logger.getLevel(),request.path()+" ["+time/1000000+"]ms"+req+new String(request.body())+rsp+new String(response.body())+end);

    if (logger.getLevel() == Level.FINEST)
      logger.log(logger.getLevel(),request.path()+" ["+time/1000000+"]ms"+req+new String(request.page())+rsp+new String(response.page())+end);
  }
}