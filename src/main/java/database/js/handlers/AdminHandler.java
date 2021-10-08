package database.js.handlers;

import java.nio.ByteBuffer;
import database.js.config.Config;
import database.js.servers.Server;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;


public class AdminHandler extends Handler
{
  private final ByteBuffer buf = ByteBuffer.allocate(16);
  
  
  public AdminHandler(Config config) throws Exception
  {
    super(config);
  }
  
  
  @Override
  public HTTPResponse handle(HTTPRequest request) throws Exception
  {
    Server server = request.server();
    HTTPResponse response = new HTTPResponse();
    
    server.request();
    getAdminLogger().info("adm request received <"+request.path()+">");
    
    if (request.path().equals("/connect"))
    {
      buf.clear();
      buf.putShort(server.id());
      buf.putLong(server.started());
      //response.setBody((byte[]) buf.flip().array(),0,10);
      response.setBody("Hello");
      getAdminLogger().info("replied with "+server.id()+" "+server.started());
    }
        
    if (request.path().equals("/shutdown"))
      server.shutdown();      

    return(response);
  }
}
