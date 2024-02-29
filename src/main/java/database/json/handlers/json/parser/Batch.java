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

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import database.json.database.BindValue;


public class Batch implements SQLObject
{
   private final Object custom;
   private final String session;
   private final JSONArray steps;

   private final ArrayList<SQLObject> sqlsteps =
      new ArrayList<SQLObject>();


   public Batch(JSONObject definition) throws Exception
   {
      Object custom = null;
      String session = null;
      JSONArray steps = new JSONArray();

      if (definition.has(Parser.STEPS))
         steps = definition.getJSONArray(Parser.STEPS);

      if (definition.has(Parser.SESSION))
         session = definition.getString(Parser.SESSION);

      if (definition.has(Parser.CUSTOM))
         custom = definition.get(Parser.CUSTOM);

      this.steps = steps;
      this.custom = custom;
      this.session = session;
   }

   @Override
   public String path() throws Exception
   {
      return(Parser.BATCH);
   }

   @Override
   public JSONObject toApi() throws Exception
   {
      JSONObject parsed = Parser.toApi(this);

      if (session != null)
         parsed.put(Parser.SESSION,session);

      if (custom != null)
         parsed.put(Parser.CUSTOM,session);

      JSONArray steps = new JSONArray();

      for (int i = 0; i < this.steps.length(); i++)
      {
         APIObject step = Parser.parse(this.steps.getJSONObject(i));
         System.out.println(step+"");
         steps.put(step.toApi());
      }

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
   public Object custom() throws Exception
   {
      return(custom);
   }

   @Override
   public String session() throws Exception
   {
      return(session);
   }

   @Override
   public boolean validate() throws Exception
   {
      return(true);
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
