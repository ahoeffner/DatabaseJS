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
import database.js.handlers.file.PathUtil;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;
import database.js.config.Handlers.HandlerProperties;
import database.js.servers.Server;
import database.js.servers.rest.RESTClient;


public class AppFileHandler extends Handler
{
  private final PathUtil path;
  private final Logger logger = Logger.getLogger("http");

  public AppFileHandler(Config config, HandlerProperties properties) throws Exception
  {
    super(config,properties);
    this.path = new PathUtil(this);
  }


  @Override
  public HTTPResponse handle(HTTPRequest request) throws Exception
  {
    request.server().request();
    HTTPResponse response = null;
    Server server = request.server();
    String json = config().getHTTP().mimetypes().get("json");
    
    if (!server.embedded())
    {
      RESTClient client = null;
      short rsrv = RestHandler.getClient(config(),request);
      
      if (rsrv >= 0) client = server.worker(rsrv);
      else
      {
        response = new HTTPResponse();
        response.setContentType(json);

        logger.warning("Not connected");
        response.setBody("{\"status\": \"failed\", \"message\": \"Not connected\"}");

        return(response);
      }

      if (client == null)
      {
        response = new HTTPResponse();
        response.setContentType(json);

        logger.warning("Could not connect to RESTServer");
        response.setBody("{\"status\": \"failed\", \"message\": \"Could not connect to RESTServer\"}");

        return(response);
      }

      if (!ensure(client,request))
      {
        response = new HTTPResponse();
        response.setContentType(json);

        logger.warning("Not connected");
        response.setBody("{\"status\": \"failed\", \"message\": \"Not connected\"}");

        return(response);
      }
    }
    
    return(null);
  }
  
  
  private boolean ensure(RESTClient client, HTTPRequest request) throws Exception
  {
    String path = this.path.getPath(request.path());
    if (path == null) return(false);
    
    String ensure = "";
    String nl = "\r\n";
    String session = path.split("/")[0];
    
    ensure += "POST /"+session+"/status HTTP/1.1"+nl;
    ensure += "Host: localhost"+nl+nl+nl;
    
    byte[] response = client.send("localhost",ensure.getBytes());
    System.out.println(new String(response));
    
    return(true);
  }
}