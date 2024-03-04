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
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import database.json.database.BindValue;


public class Query implements SQLObject
{
   private final boolean lock;
   private final boolean describe;

   private final int skip;
   private final int rows;
   private final String order;
   private final String source;
   private final String cursor;
   private final Object custom;
   private final String session;
   private final String[] columns;
   private final WhereClause whcl;
   private final BindValue[] assertions;
   private final BindValue[] bindvalues;

   public static Query describe(JSONObject definition) throws Exception
   {
      JSONObject whcl = new JSONObject();
      JSONObject filter = new JSONObject();
      JSONObject describe = new JSONObject();

      JSONArray filters = new JSONArray();
      JSONArray columns = new JSONArray().put("*");

      String source = definition.getString(Parser.SOURCE);
      String session = definition.getString(Parser.SESSION);

      filters.put(whcl);
      whcl.put(Parser.FILTER,filter);
      filter.put("type","False");

      describe.put(Parser.SOURCE,source);
      describe.put(Parser.SESSION,session);
      describe.put(Parser.COLUMNS,columns);
      describe.put(Parser.FILTERS,filters);
      describe.put(Parser.DESCRIBE,true);

      return(new Query(describe));
   }


   public Query(JSONObject definition) throws Exception
   {
      int skip = 0;
      int rows = 0;
      String order = null;
      String source = null;
      String cursor = null;
      boolean lock = false;
      Object custom = null;
      String session = null;
      WhereClause whcl = null;
      boolean describe = false;
      String[] columns = new String[0];

      ArrayList<BindValue> bindvalues = new ArrayList<BindValue>();
      bindvalues.addAll(Arrays.asList(Parser.getBindValues(definition)));

      if (definition.has(Parser.LOCK))
         lock = definition.getBoolean(Parser.LOCK);

      if (definition.has(Parser.SKIP))
         skip = definition.getInt(Parser.SKIP);

      if (definition.has(Parser.ROWS))
         rows = definition.getInt(Parser.ROWS);

      if (definition.has(Parser.ORDER))
         order = definition.getString(Parser.ORDER);

      if (definition.has(Parser.SOURCE))
         source = definition.getString(Parser.SOURCE);

      if (definition.has(Parser.CURSOR))
         cursor = definition.getString(Parser.CURSOR);

      if (definition.has(Parser.SESSION))
         session = definition.getString(Parser.SESSION);

      if (definition.has(Parser.DESCRIBE))
         describe = definition.getBoolean(Parser.DESCRIBE);

      if (definition.has(Parser.COLUMNS))
      {
         JSONArray jarr = definition.getJSONArray(Parser.COLUMNS);
         columns = new String[jarr.length()];

         for (int i = 0; i < jarr.length(); i++)
            columns[i] = jarr.getString(i);
      }

      if (definition.has(Parser.FILTERS))
      {
         whcl = new WhereClause(); whcl.parse(definition);
         bindvalues.addAll(Arrays.asList(whcl.getBindValues()));
      }

      if (definition.has(Parser.CUSTOM))
         custom = definition.get(Parser.CUSTOM);

      this.whcl = whcl;
      this.skip = skip;
      this.rows = rows;
      this.lock = lock;
      this.order = order;
      this.source = source;
      this.cursor = cursor;
      this.custom = custom;
      this.session = session;
      this.columns = columns;
      this.describe = describe;
      this.assertions = Parser.getAssertions(definition);
      this.bindvalues = bindvalues.toArray(new BindValue[0]);
   }


   public String source()
   {
      return(source);
   }


   public boolean describe()
   {
      return(describe);
   }


   @Override
   public String session()
   {
      return(session);
   }


   @Override
   public boolean validate()
   {
      Source source = Source.getSource(this.source);
      if (source == null || columns.length == 0) return(false);
      if (whcl != null) return(whcl.validate(source));
      return(true);
   }


   @Override
   public String sql() throws Exception
   {
      String sql = "";
      Source source = Source.getSource(this.source);
      if (source == null) throw new Exception("Permission denied, source: '"+this.source+"'");

      sql += "select "+columns[0];

      for (int i = 1; i < columns.length; i++)
         sql += ","+columns[i];

      if (source.sql == null)
      {
         sql += " from "+source.table;

         if (whcl != null && !whcl.isEmpty())
            sql += " where " + whcl.sql();

         if (order != null)
            sql += " order by "+order;
      }
      else
      {
         sql += " from ("+source.sql;

         if (whcl != null)
            sql += " where " + whcl.sql();

         if (order != null)
            sql += " order by "+order;

         sql += ") "+source.id;
      }

      return(sql);
   }


   @Override
   public Object custom()
   {
      return(custom);
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
      return(Parser.QUERY);
   }

   @Override
   public JSONObject toApi() throws Exception
   {
      JSONObject parsed = Parser.toApi(this);

      parsed.put(Parser.COMPACT,true);

      if (lock) parsed.put(Parser.LOCK,lock);
      if (skip > 0) parsed.put(Parser.SKIP,skip);
      if (rows > 0) parsed.put(Parser.ROWS,rows);
      if (describe) parsed.put(Parser.DESCRIBE,describe);
      if (cursor != null) parsed.put(Parser.CURSOR,cursor);

      return(parsed);
   }

   @Override
   public boolean lock()
   {
      return(lock);
   }
}