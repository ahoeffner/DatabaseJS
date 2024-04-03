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
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import database.json.database.BindValue;
import database.json.database.BindValueDef;


public class Delete implements SQLObject
{
   private final boolean lock;
   private final Source source;
   private final WhereClause whcl;
   private final JSONObject payload;
   private final String[] returning;
   private final BindValue[] assertions;
   private final BindValue[] bindvalues;


   public Delete(JSONObject definition) throws Exception
   {
      boolean lock = false;
      WhereClause whcl = null;

      ArrayList<BindValue> bindvalues = new ArrayList<BindValue>();

      if (definition.has(Parser.LOCK))
         lock = definition.getBoolean(Parser.LOCK);

      if (!definition.has(Parser.SOURCE)) source = null;
      else source = Source.getSource(definition.getString(Parser.SOURCE));

      if (definition.has(Parser.FILTERS) || source.delflt)
      {
         whcl = new WhereClause(source); whcl.parse(definition);
         bindvalues.addAll(Arrays.asList(whcl.getBindValues()));
         if (source.delflt) whcl.filter(source.filter);
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

      bindvalues.addAll(Arrays.asList(Parser.getBindValues(definition)));

      this.whcl = whcl;
      this.lock = lock;
      this.payload = definition;
      this.returning = returning;
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
      Check lim = source.delete;

      if (lim == Check.blocked)
         throw new Exception(WhereClause.deny(source));

      if (whcl == null && lim != Check.none)
         throw new Exception(WhereClause.deny(source));

      if (whcl == null) return(true);
      return(whcl.validate(source,lim));
   }


   @Override
   public String sql() throws Exception
   {
      String sql = "delete from "+source.table;

      if (whcl != null && !whcl.isEmpty())
         sql += " where " + whcl.sql();

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
      return(assertions);
   }

   @Override
   public String path()
   {
      return(Parser.DELETE);
   }

   @Override
   public JSONObject toApi() throws Exception
   {
      JSONObject parsed = Parser.toApi(this);
      if (lock) parsed.put(Parser.LOCK,lock);

      if (returning.length > 0)
         parsed.put("returning",true);

      return(parsed);
   }

   @Override
   public boolean lock()
   {
      return(lock);
   }
}
