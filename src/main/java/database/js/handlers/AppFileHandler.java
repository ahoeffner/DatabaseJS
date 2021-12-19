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
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.servers.Server;
import database.js.handlers.file.PathUtil;
import database.js.servers.rest.RESTClient;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;
import database.js.handlers.rest.JSONFormatter;
import database.js.config.Handlers.HandlerProperties;


public class AppFileHandler extends Handler
{
  private final PathUtil path;
  private final TreeSet<String> domains;
  private final Logger logger = Logger.getLogger("http");


  public AppFileHandler(Config config, HandlerProperties properties) throws Exception
  {
    super(config,properties);
    this.path = new PathUtil(this);
    this.domains = new TreeSet<String>();
  }


  @Override
  public HTTPResponse handle(HTTPRequest request) throws Exception
  {
    request.server().request();
    HTTPResponse response = null;
    Server server = request.server();

    String json = config().getHTTP().mimetypes.get("json");
    String html = config().getHTTP().mimetypes.get("html");

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

    String boundary = null;
    String ctype = request.getHeader("Content-Type");
    if (ctype.startsWith("multipart/form-data"))
    {
      int pos = ctype.indexOf("boundary=");
      if (pos > 0) boundary = ctype.substring(pos+9);
    }

    String mode = request.getHeader("Sec-Fetch-Mode");
    if (mode != null && mode.equalsIgnoreCase("cors"))
    {
      String origin = request.getHeader("Origin");

      if (origin == null)
      {
        response = new HTTPResponse();
        response.setContentType(json);

        logger.warning("Null Cors Origin header detected. Request rejected");
        response.setBody("{\"status\": \"failed\", \"message\": \"Null Cors Origin header detected. Request rejected\"}");

        return(response);
      }

      if (!allow(origin))
      {
        response = new HTTPResponse();
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

    if (!server.embedded())
    {
      String errm = ensure(request);

      if (errm != null)
      {
        response = new HTTPResponse();
        response.setContentType(json);
        JSONFormatter jfmt = new JSONFormatter();

        jfmt.success(false);
        jfmt.add("message",errm);

        response.setBody(jfmt.toString());
        logger.warning(errm);
        return(response);
      }
    }

    //System.out.println(new String(request.page()));

    response = new HTTPResponse();
    response.setContentType(json);
    JSONFormatter jfmt = new JSONFormatter();

    jfmt.success(true);
    jfmt.add("message","file uploaded");

    response.setBody(jfmt.toString());
    return(response);
  }


  private String ensure(HTTPRequest request) throws Exception
  {
    if (1 == 1) return(null);

    String path = this.path.getPath(request.path());
    if (path == null) return("Illegal path specification");

    short rsrv = RestHandler.getClient(config(),request);

    if (rsrv < 0)
      return("Not connected");

    RESTClient client = request.server().worker(rsrv);

    if (client == null)
      return("Could not connect to RESTServer");

    String ensure = "";
    String nl = "\r\n";
    String session = path.split("/")[0];
    System.out.println("Session "+session);

    ensure += "POST /"+session+"/status HTTP/1.1"+nl;
    ensure += "Host: localhost"+nl+nl+nl;

    byte[] response = client.send("localhost",ensure.getBytes());
    System.out.println(new String(response));

    return(null);
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
}