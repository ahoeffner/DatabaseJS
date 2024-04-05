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

package database.json.handlers.json.parser;

import org.json.JSONArray;
import java.util.ArrayList;
import org.json.JSONObject;
import database.json.database.BindValue;


public class Script implements SQLObject
{
   private final JSONArray steps;
   private final JSONObject payload;
   private final boolean disconnect;

   private final ArrayList<SQLObject> sqlsteps =
      new ArrayList<SQLObject>();


   public Script(JSONObject definition) throws Exception
   {
      JSONArray steps = new JSONArray();

      if (definition.has(Parser.STEPS))
         steps = definition.getJSONArray(Parser.STEPS);

      if (!definition.has(Parser.DISCONNECT)) disconnect = true;
      else disconnect = definition.getBoolean(Parser.DISCONNECT);

      this.steps = steps;
      this.payload = definition;
   }


   @Override
   public String path() throws Exception
   {
      return(Parser.BATCH);
   }


   @Override
   public JSONObject payload()
   {
      return(payload);
   }


   @Override
   public JSONObject toApi() throws Exception
   {
      JSONArray steps = new JSONArray();
      JSONObject parsed = Parser.toApi(this);

      if (!disconnect)
         parsed.put(Parser.DISCONNECT,false);

      for (int i = 0; i < this.steps.length(); i++)
      {
         APIObject step = Parser.parse(this.steps.getJSONObject(i));
         if (step instanceof SQLObject) sqlsteps.add((SQLObject) step);

         JSONObject next = new JSONObject();
         next.put(Parser.PATH,step.path());
         next.put(Parser.PAYLOAD,step.toApi());

         steps.put(next);
      }

      parsed.put(Parser.BATCH,steps);
      return(parsed);
   }


   @Override
   public String sql() throws Exception
   {
      return(null);
   }


   @Override
   public boolean lock() throws Exception
   {
      return(false);
   }


   @Override
   public boolean validate() throws Exception
   {
      boolean success = true;

      for (SQLObject sql : sqlsteps)
      {
         if (!sql.validate())
            success = false;
      }

      return(success);
   }


   @Override
   public BindValue[] getAssertions() throws Exception
   {
      return(new BindValue[0]);
   }

   @Override
   public BindValue[] getBindValues() throws Exception
   {
      return(new BindValue[0]);
   }
}
