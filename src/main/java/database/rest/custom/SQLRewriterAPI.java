/*
  MIT License

  Copyright © 2023 Alex Høffner

  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
  and associated documentation files (the “Software”), to deal in the Software without
  restriction, including without limitation the rights to use, copy, modify, merge, publish,
  distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
  Software is furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all copies or
  substantial portions of the Software.

  THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
  BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package database.rest.custom;

import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.sql.PreparedStatement;
import database.rest.servers.Server;
import database.rest.database.SQLParser;
import database.rest.handlers.rest.Request;
import database.rest.database.BindValueDef;
import database.rest.handlers.rest.Rest.SessionState;


public class SQLRewriterAPI
{
   private final Server server;
   private final SessionState state;
   private final static Logger logger = Logger.getLogger("rest");


   public SQLRewriterAPI(Server server, SessionState state)
   {
      this.state = state;
      this.server = server;
   }

   public Logger logger()
   {
      return(SQLRewriterAPI.logger);
   }

   public JSONObject parse(String payload) throws Exception
   {
      return(Request.parse(payload));
   }

   public String getSQL(JSONObject payload)
   {
      if (payload.has("sql"))
         return(payload.getString("sql"));

      return(null);
   }

   public ArrayList<BindValue> getBindValues(JSONObject payload)
   {
      JSONArray bindv = new JSONArray();
      ArrayList<BindValue> bindvalues = new ArrayList<BindValue>();

      if (payload.has("bindvalues"))
         bindv = payload.getJSONArray("bindvalues");

      for (int i = 0; i < bindv.length(); i++)
      {
         JSONObject bvalue = bindv.getJSONObject(i);

         Object value = null;
         boolean outval = false;

         String name = bvalue.getString("name");
         String type = bvalue.getString("type");

         if (!bvalue.has("value")) outval = true;
         else value = bvalue.get("value");

         bindvalues.add(new BindValue(name,value,type,outval));
      }

      return(bindvalues);
   }

   public PreparedStatement parseSQL(JSONObject payload) throws Exception
   {
      String sql = getSQL(payload);
      ArrayList<BindValue> bindValues = getBindValues(payload);
      return(parseSQL(sql,bindValues));
   }

   public PreparedStatement parseSQL(String sql) throws Exception
   {
      return(state.session().prepare(sql,null));
   }

   public PreparedStatement parseSQL(String sql, ArrayList<BindValue> bindvalues) throws Exception
   {
      HashMap<String,BindValueDef> binds = new HashMap<String,BindValueDef>();

      for(BindValue b : bindvalues)
      {
         BindValueDef bv = new BindValueDef(b.name,b.getType(),b.outval);
         binds.put(bv.name,bv);
      }

      SQLParser parser = new SQLParser(binds,sql);
      return(state.session().prepare(parser.sql(),parser.bindvalues()));
   }
}
