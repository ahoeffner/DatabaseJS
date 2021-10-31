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
import database.js.control.Process.Type;
import database.js.servers.rest.RESTClient;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;
import database.js.config.Handlers.HandlerProperties;


public class RestHandler extends Handler
{
  public RestHandler(Config config, HandlerProperties properties) throws Exception
  {
    super(config,properties);
  }
  
  
  @Override
  public HTTPResponse handle(HTTPRequest request) throws Exception
  {
    HTTPResponse response = null;
    Server server = request.server();
    Logger logger = getLogger(Type.rest);

    server.request();
    logger.fine("REST request received <"+request.path()+"> embedded="+server.embedded());
    
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

      return(response);
    }    

    response = new HTTPResponse();
    response.setBody("{\"status\": \"ok\"}");
    return(response);
  }
}
