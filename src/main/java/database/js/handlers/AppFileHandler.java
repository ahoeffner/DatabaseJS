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
import database.js.handlers.file.PathUtil;
import database.js.servers.rest.RESTClient;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;
import database.js.handlers.rest.JSONFormatter;
import database.js.config.Handlers.HandlerProperties;


public class AppFileHandler extends Handler
{
  private final PathUtil path;
  private final CorsDomains domains;
  private final Logger logger = Logger.getLogger("http");


  public AppFileHandler(Config config, HandlerProperties properties) throws Exception
  {
    super(config,properties);
    this.path = new PathUtil(this);
    this.domains = new CorsDomains(config);
  }


  @Override
  public HTTPResponse handle(HTTPRequest request) throws Exception
  {
    request.server().request();
    Server server = request.server();
    HTTPResponse response = new HTTPResponse();
    String json = config().getHTTP().mimetypes.get("json");

    server.request();
    logger.finest("AppFile request received: "+request.path());

    response.setContentType(json);
    String errm = domains.allow(request);

    if (errm != null)
    {
      response.setBody(errm);
      return(response);
    }

    domains.addCorsHeaders(request,response);

    String boundary = null;
    String ctype = request.getHeader("Content-Type");
    if (ctype.startsWith("multipart/form-data"))
    {
      int pos = ctype.indexOf("boundary=");
      if (pos > 0) boundary = ctype.substring(pos+9);
    }

    if (!server.embedded())
    {
      errm = ensure(request);

      if (errm != null)
      {
        JSONFormatter jfmt = new JSONFormatter();

        jfmt.success(false);
        jfmt.add("message",errm);

        response.setBody(jfmt.toString());
        logger.warning(errm);
        return(response);
      }
    }

    //System.out.println(new String(request.page()));

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
}