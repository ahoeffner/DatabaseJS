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

import java.util.Arrays;
import java.util.HashMap;
import org.json.JSONArray;
import java.util.ArrayList;
import org.json.JSONObject;
import database.json.database.BindValue;
import database.json.database.BindValueDef;


public class Update implements SQLObject
{
   private final boolean lock;
   private final Source source;
   private final WhereClause whcl;
   private final JSONObject payload;
   private final BindValue[] assertions;
   private final BindValue[] bindvalues;

   private final HashMap<String,BindValue> values =
      new HashMap<String,BindValue>();


   public Update(JSONObject definition) throws Exception
   {
      String source = null;
      boolean lock = false;
      WhereClause whcl = null;

      ArrayList<BindValue> bindvalues = new ArrayList<BindValue>();

      if (definition.has(Parser.LOCK))
         lock = definition.getBoolean(Parser.LOCK);

      if (definition.has(Parser.SOURCE))
         source = definition.getString(Parser.SOURCE);

      if (definition.has(Parser.FILTERS))
      {
         whcl = new WhereClause(); whcl.parse(definition);
         bindvalues.addAll(Arrays.asList(whcl.getBindValues()));
      }

      if (definition.has(Parser.UPDATE))
      {
         JSONArray jarr = definition.getJSONArray(Parser.UPDATE);

         for (int i = 0; i < jarr.length(); i++)
         {
            String name = null;
            String type = null;
            Object value = null;
            String column = null;

            JSONObject bdef = jarr.optJSONObject(i);

            if (bdef.has("value")) value = bdef.get("value");
            if (bdef.has("name")) name = bdef.getString("name");
            if (bdef.has("type")) type = bdef.getString("type");
            if (bdef.has("column")) column = bdef.getString("column");

            BindValue bval = new BindValue(new BindValueDef(name,type,false,value),false);

            bindvalues.add(bval);
            values.put(column,bval);
         }
      }

      bindvalues.addAll(Arrays.asList(Parser.getBindValues(definition)));

      this.whcl = whcl;
      this.lock = lock;
      this.payload = definition;
      this.source = Source.getSource(source);
      this.assertions = Parser.getAssertions(definition);
      this.bindvalues = bindvalues.toArray(new BindValue[0]);

      if (this.source == null)
         throw new Exception(Source.deny(source));
   }


   @Override
   public JSONObject payload()
   {
      return(payload);
   }


   @Override
   public boolean validate() throws Exception
   {
      if (values.keySet().size() == 0)
         return(false);

      Limitation lim = source.update;

      if (lim == Limitation.blocked)
         throw new Exception(WhereClause.deny(source));

      if (whcl == null && lim != Limitation.none)
         throw new Exception(WhereClause.deny(source));

      return(whcl.validate(source,lim));
   }


   @Override
   public String sql() throws Exception
   {
      String sql = "update "+source.table+" set ";
      String[] columns = values.keySet().toArray(new String[0]);

      sql += columns[0]+" = :"+values.get(columns[0]).getName();

      for (int i = 1; i < columns.length; i++)
         sql += columns[i]+" = :"+values.get(columns[i]).getName();

      if (whcl != null && !whcl.isEmpty())
         sql += " where " + whcl.sql();

      return(sql);
   }


   @Override
   public BindValue[] getBindValues()
   {
      return(bindvalues);
   }


   @Override
   public BindValue[] getAssertions()
   {
      return(assertions);
   }

   @Override
   public String path()
   {
      return(Parser.UPDATE);
   }

   @Override
   public JSONObject toApi() throws Exception
   {
      JSONObject parsed = Parser.toApi(this);
      if (lock) parsed.put(Parser.LOCK,lock);
      return(parsed);
   }

   @Override
   public boolean lock()
   {
      return(lock);
   }
}
