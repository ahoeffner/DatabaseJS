package database.rest.custom;

import java.util.logging.Logger;

import org.json.JSONObject;
import database.rest.config.Config;
import database.rest.servers.Server;
import database.rest.handlers.rest.Rest;
import database.rest.handlers.rest.Request;


public class AuthenticatorAPI
{
   private final String host;
   private final Server server;
   private final static Logger logger = Logger.getLogger("rest");


   public AuthenticatorAPI(Server server, String host)
   {
      this.host = host;
      this.server = server;
   }

   public Config config()
   {
      return(server.config());
   }

   public Logger logger()
   {
      return(AuthenticatorAPI.logger);
   }

   public JSONObject parse(String payload) throws Exception
   {
      return(Request.parse(payload));
   }

   public String connect() throws Exception
   {
      return(connect(null));
   }

   public String connect(String user) throws Exception
   {
      Rest rest = new Rest(server,true,host);
      return(rest.connect(user));
   }

   public boolean disconnect(String sesid) throws Exception
   {
      Rest rest = new Rest(server,true,host);
      String response = rest.execute("disconnect","{\"session\": \""+sesid+"\"}",false);

      JSONObject rsp = parse(response);
      return(rsp.getBoolean("success"));
   }

   public String execute(JSONObject payload) throws Exception
   {
      return(execute(payload.toString(2)));
   }

   public String execute(String payload) throws Exception
   {
      Rest rest = new Rest(server,true,host);
      String response = rest.execute("exec",payload,false);
      return(response);
   }
}
