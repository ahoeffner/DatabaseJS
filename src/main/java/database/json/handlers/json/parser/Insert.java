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

import java.util.HashMap;
import java.util.HashSet;
import org.json.JSONArray;
import java.util.ArrayList;
import org.json.JSONObject;
import database.json.database.BindValue;
import database.json.database.BindValueDef;
import database.json.database.Database;


public class Insert implements SQLObject
{
   private final Source source;
   private final String[] returning;
   private final JSONObject payload;
   private final BindValue[] bindvalues;

   private final HashMap<String,BindValue> values =
      new HashMap<String,BindValue>();


   public Insert(JSONObject definition) throws Exception
   {
      ArrayList<BindValue> bindvalues = new ArrayList<BindValue>();

      if (!definition.has(Parser.SOURCE)) this.source = null;
      else this.source = Source.getSource(definition.getString(Parser.SOURCE));

      if (this.source == null)
      throw new Exception(Source.deny(source));

      HashSet<String> derived = new HashSet<String>();

      for (int i = 0; source.derived != null && i < source.derived.length; i++)
         derived.add(source.derived[i].toLowerCase());

      if (definition.has(Parser.VALUES))
      {
         JSONArray jarr = definition.getJSONArray(Parser.VALUES);

         for (int i = 0; i < jarr.length(); i++)
         {
            String name = null;
            String type = null;
            Object value = null;
            String column = null;

            JSONObject bdef = jarr.optJSONObject(i);

            if (bdef.has("column"))
               column = bdef.getString("column");

            if (column == null)
               throw new Exception("Syntax error");

            if (derived.contains(column.toLowerCase()))
               continue;

            if (bdef.has(Parser.VALUE))
            {
               bdef = bdef.getJSONObject(Parser.VALUE);

               if (bdef.has("value")) value = bdef.get("value");
               if (bdef.has("name")) name = bdef.getString("name");
               if (bdef.has("type")) type = bdef.getString("type");

               BindValue bval = new BindValue(new BindValueDef(name,type,false,value),false);

               bindvalues.add(bval);
               values.put(column,bval);
            }
         }
      }

      String[] returning = new String[0];

      if (definition.has(Parser.RETURNING))
      {
         JSONArray jarr = definition.getJSONArray(Parser.RETURNING);

         returning = new String[jarr.length()];

         for (int i = 0; i < jarr.length(); i++)
         {
            JSONObject rdef = jarr.optJSONObject(i);
            returning[i] = rdef.getString(Parser.COLUMN);

            String type = rdef.getString(Parser.TYPE);
            String name = rdef.getString(Parser.COLUMN);

            returning[i] = name;
            BindValueDef bdef = new BindValueDef(name,type,true,null);

            bindvalues.add(new BindValue(bdef,true));
         }
      }

      this.payload = definition;
      this.returning = returning;
      this.bindvalues = bindvalues.toArray(new BindValue[0]);
   }


   @Override
   public JSONObject payload()
   {
      return(payload);
   }


   @Override
   public boolean validate()
   {
      return(true);
   }


   @Override
   public String sql() throws Exception
   {
      String sql = "insert into "+source.table;

      sql += "(";

      String[] columns = values.keySet().toArray(new String[0]);

      for (int i = 0; i < columns.length; i++)
      {
         if (i > 0) sql += ",";
         sql += columns[i];
      }

      sql += ") values (";

      for (int i = 0; i < columns.length; i++)
      {
         if (i > 0) sql += ",";
         sql += ":"+values.get(columns[i]).getName();
      }

      sql += ")";

      if (returning.length > 0)
      {
         sql += " returning "+returning[0];
         for (int i = 1; i < returning.length; i++)
            sql += "," + returning[i];
      }

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
      return(new BindValue[0]);
   }

   @Override
   public String path()
   {
      return(Parser.INSERT);
   }

   @Override
   public JSONObject toApi() throws Exception
   {
      JSONObject parsed = Parser.toApi(this);

      if (returning.length > 0)
         parsed.put("returning",true);
         
      return(parsed);
   }

   @Override
   public boolean lock()
   {
      return(false);
   }
}
