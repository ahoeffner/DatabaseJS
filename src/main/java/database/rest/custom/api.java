package database.rest.custom;

import org.json.JSONObject;
import database.rest.config.Config;
import database.rest.servers.Server;
import database.rest.handlers.rest.Request;
import database.rest.handlers.rest.Rest;


public class api
{
   private final String host;
   private final Server server;

   public api(Server server, String host)
   {
      this.host = host;
      this.server = server;
   }

   public Config config()
   {
      return(server.config());
   }

   public JSONObject parse(String payload) throws Exception
   {
      return(Request.parse(payload));
   }

   public String execute(JSONObject payload) throws Exception
   {
      return(execute(null,payload.toString(2)));
   }

   public String execute(String user, JSONObject payload) throws Exception
   {
      return(execute(user,payload.toString(2)));
   }

   public String execute(String payload) throws Exception
   {
      return(execute(null,payload));
   }

   public String execute(String user, String payload) throws Exception
   {
      Rest rest = new Rest(server,true,host);
      //rest.preauth(true); // dedicated
      String response = rest.execute("exec",payload,false);
      //rest.preauth(false);
      return(response);
   }
}
