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
    logger.fine("adm request received <"+request.path()+">");
    
    if (request.path().equals("/connect"))
    {
      request.unlist();

      String body = new String(request.body());
      response.setBody(server.id()+" "+server.started());
      
      String[] args = body.split(" ");
      short id = Short.parseShort(args[0]);
      long started = Long.parseLong(args[1]);

      RESTClient worker = server.worker(id);
      
      if (worker == null) logger.info("RESTServer connecting");
      else logger.fine("RESTServer connecting secondary channel");
      
      if (worker == null || started != worker.started()) 
        worker = new RESTClient(server,id,started);        
      
      server.register(worker);
      request.respond(response.page());
     
      worker.init(request.channel());
      return(null);
    }
        
    if (request.path().equals("/shutdown"))
      server.shutdown();      

    return(response);
  }
}
