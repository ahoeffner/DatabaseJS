package database.js.handlers;

import java.util.logging.Level;
import database.js.config.Config;
import database.js.servers.Server;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;


public class AdminHandler extends Handler implements Runnable
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
    {
      Thread thread = new Thread(this);
      thread.start();
    }

    return(response);
  }


  @Override
  public void run()
  {
    try
    {
      server.shutdown();      
    }
    catch (Exception e)
    {
      this.server.logger().log(Level.SEVERE,e.getMessage(),e);
    }
  }
}
