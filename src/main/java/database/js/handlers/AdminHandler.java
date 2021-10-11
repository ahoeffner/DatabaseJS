package database.js.handlers;

import java.util.logging.Logger;
import database.js.config.Config;
import database.js.servers.Server;
import database.js.servers.rest.RESTClient;
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
    Logger logger = getAdminLogger();
    Server server = request.server();
    HTTPResponse response = new HTTPResponse();
    
    server.request();
    logger.info("adm request received <"+request.path()+">");
    
    if (request.path().equals("/connect"))
    {
      String body = new String(request.getBody());
      response.setBody(server.id()+" "+server.started());
      
      request.unlist();
      
      short id = Short.parseShort(body);
      RESTClient engine = new RESTClient(request.channel(),id);
      
      server.engine(engine);
    }
        
    if (request.path().equals("/shutdown"))
      server.shutdown();      

    return(response);
  }
}
