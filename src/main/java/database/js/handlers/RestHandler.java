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


public class RestHandler extends Handler
{
  public RestHandler(Config config) throws Exception
  {
    super(config);
  }
  
  
  @Override
  public HTTPResponse handle(HTTPRequest request) throws Exception
  {
    Server server = request.server();
    Logger logger = getLogger(Type.rest);
    HTTPResponse response = new HTTPResponse();

    server.request();
    logger.info("REST request received <"+request.path()+"> embedded="+server.embedded());
    
    if (!server.embedded())
    {
      RESTClient client = server.worker();

      if (client == null)
      {
        logger.warning("No RESTServer's connected");
        response.setBody("{\"status\": \"failed\"}");
        return(response);
      }
      
      client.send(request.page());
    }

    response.setBody("{\"status\": \"ok\"}");
    return(response);
  }
}
