package database.js.handlers;

import database.js.config.Config;
import database.js.servers.Server;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;


public class AdminHandler extends Handler
{
  public AdminHandler(Config config) throws Exception
  {
    super(config);
  }
  
  
  @Override
  public HTTPResponse handle(HTTPRequest request) throws Exception
  {
    Server server = request.server();
    HTTPResponse response = new HTTPResponse();
    
    getAdminLogger().info("adm request received");
    
    server.request();
    
    if (request.path().equals("/shutdown"))
      server.shutdown();      

    return(response);
  }
}
