package database.js.handlers;

import database.js.config.Config;
import database.js.servers.Server;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;


public class AdminHandler extends Handler
{
  private Server server = null;
  
  
  public AdminHandler(Config config) throws Exception
  {
    super(config);
  }
  
  
  public AdminHandler server(Server server)
  {
    this.server = server;
    return(this);
  }
  
  
  @Override
  public HTTPResponse handle(HTTPRequest request) throws Exception
  {
    HTTPResponse response = new HTTPResponse();
    
    if (request.path().equals("/shutdown"))
      server.shutdown();      

    return(response);
  }
}
