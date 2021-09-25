package database.js.handlers;

import database.js.config.Config;
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
    HTTPResponse response = new HTTPResponse();
    
    if (request.path().equals("/stop"))
    {
      logger.info("Shutting down");
      System.exit(0);
    }
    
    return(response);
  }
}
