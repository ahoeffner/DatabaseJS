package database.js.handlers;

import database.js.servers.Server;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;


public class AdminHandler extends Handler
{
  public AdminHandler(Server server) throws Exception
  {
    super(server);
  }
  
  
  @Override
  public HTTPResponse handle(HTTPRequest request) throws Exception
  {
    HTTPResponse response = new HTTPResponse();
    
    if (request.path().equals("/shutdown"))
      this.server.shutdown();
    
    return(response);
  }
}
