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
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.logging.Logger;


public class Source
{
   private final static Logger logger = Logger.getLogger("json");

   private static final String ID = "id";
   private static final String SQL = "sql";
   private static final String QUERY = "query";
   private static final String TABLE = "table";
   private static final String ORDER = "order";
   private static final String LIMIT = "limit";
   private static final String ACCEPT = "accept";
   private static final String SELECT = "select";
   private static final String INSERT = "insert";
   private static final String UPDATE = "update";
   private static final String DELETE = "delete";
   private static final String FUNCTION = "name";

   private static final String DERIVED = "derived";
   private static final String PRIMARYKEY = "primarykey";

   private static final String FNAME = "name";
   private static final String PARAMS = "parameters";
   private static final String RESTRCT = "restricts";
   private static final String WHERECL = "where-clause";
   private static final String CFILTERS = "custom-filters";


   private static HashMap<String,Source> sources =
      new HashMap<String,Source>();


   public static String deny(Source source)
   {
      return("Access to source "+source.id+" is not allowed");
   }

   public static String deny(String source)
   {
      return("Access to source "+source+" is not allowed");
   }

   public static Source getSource(String id)
   {
      return(Source.sources.get(id.toLowerCase()));
   }

   public static void setSources(HashMap<String,Source> sources)
   {
      Source.sources = sources;
   }


   public final String id;
   public final String stmt;
   public final String query;
   public final String table;
   public final String order;
   public final String function;
   public final SourceType type;
   public final String[] primary;
   public final String[] derived;

   public final HashMap<String,FilterDefinition> filters =
      new HashMap<String,FilterDefinition>();

   public final Limitation select;
   public final Limitation insert;
   public final Limitation update;
   public final Limitation delete;


   public Source(SourceType type, JSONObject definition) throws Exception
   {
      String id = null;
      String stmt = null;
      String query = null;
      String table = null;
      String order = null;
      String function = null;
      String[] primary = null;
      String[] derived = null;

      Limitation insert = Limitation.blocked;
      Limitation select = Limitation.singlerow;
      Limitation update = Limitation.singlerow;
      Limitation delete = Limitation.singlerow;

      id = definition.getString(ID);
      this.id = (id+"").toLowerCase();

      if (type == SourceType.table)
      {
         if (definition.has(TABLE))
            table = definition.getString(TABLE);

         if (definition.has(ORDER))
            order = definition.getString(ORDER);

         if (definition.has(PRIMARYKEY))
         {
            String pkey = definition.getString(PRIMARYKEY);

            primary = pkey.split((","));

            for (int i = 0; i < primary.length; i++)
               primary[i] = primary[i].trim();
         }

         if (definition.has(DERIVED))
         {
            String cols = definition.getString(DERIVED);

            derived = cols.split((","));

            for (int i = 0; i < derived.length; i++)
               derived[i] = derived[i].trim();
         }

         if (definition.has(CFILTERS))
         {
            JSONArray pars = definition.getJSONArray(CFILTERS);

            for (int p = 0; p < pars.length(); p++)
            {
               FilterDefinition def = new FilterDefinition(pars.getJSONObject(p));
               filters.put(def.name,def);
            }
         }

         if (definition.has(INSERT))
         {
            JSONObject sec = definition.getJSONObject(INSERT);
            if (sec.has(ACCEPT) && sec.getBoolean(ACCEPT)) insert = Limitation.none;
         }

         if (definition.has(UPDATE))
         {
            JSONObject sec = definition.getJSONObject(UPDATE);
            if (sec.has(ACCEPT) && !sec.getBoolean(ACCEPT)) update = Limitation.blocked;
            else if (sec.has(LIMIT)) update = Limitation.valueOf(sec.getString(LIMIT).toLowerCase());
         }

         if (definition.has(DELETE))
         {
            JSONObject sec = definition.getJSONObject(DELETE);
            if (sec.has(ACCEPT) && !sec.getBoolean(ACCEPT)) delete = Limitation.blocked;
            else if (sec.has(LIMIT)) delete = Limitation.valueOf(sec.getString(LIMIT).toLowerCase());
         }

         if (definition.has(SELECT))
         {
            JSONObject sec = definition.getJSONObject(SELECT);
            if (sec.has(ACCEPT) && !sec.getBoolean(ACCEPT)) select = Limitation.blocked;
            else if (sec.has(LIMIT)) select = Limitation.valueOf(sec.getString(LIMIT).toLowerCase());
         }

         if (definition.has(QUERY))
         {
            Object obj = definition.get(QUERY);
            if (obj instanceof String) query = (String) obj;

            else if (obj instanceof JSONArray)
            {
               query = "";
               JSONArray lines = (JSONArray) obj;

               for (int i = 0; i < lines.length(); i++)
                  query += lines.getString(i) + " ";

               query = query.trim();
            }
         }

         String ops = "["+
            (insert+"").charAt(0) + "|" +
            (select+"").charAt(0) + "|" +
            (update+"").charAt(0) + "|" +
            (delete+"").charAt(0) + "]";

         logger.info("register datasource '"+id+"', CRUD Limitation: "+ops);

         if (filters.size() > 0)
         {
            for (FilterDefinition flt : filters.values())
               logger.info("'"+id+"'' custom filter '"+flt);
         }
      }
      else if (type == SourceType.function)
      {
         function = id;

         if (definition.has(FUNCTION))
            function = definition.getString(FUNCTION);

         logger.info("register function/procedure '"+id+"'");
      }
      else if (type == SourceType.sql)
      {
         if (definition.has(SQL))
         {
            Object obj = definition.get(SQL);
            if (obj instanceof String) stmt = (String) obj;

            else if (obj instanceof JSONArray)
            {
               stmt = "";
               JSONArray lines = (JSONArray) obj;

               for (int i = 0; i < lines.length(); i++)
                  stmt += lines.getString(i) + " ";

               stmt = stmt.trim();
            }

            logger.info("register sql '"+id+"'");
         }
      }

      this.type = type;
      this.stmt = stmt;
      this.query = query;
      this.table = table;
      this.order = order;
      this.primary = primary;
      this.derived = derived;
      this.function = function;

      this.select = select;
      this.insert = insert;
      this.update = update;
      this.delete = delete;
   }


   public String getCustomSQL(JSONObject fltdef) throws Exception
   {
      String filter = null;

      if (fltdef.has(FNAME))
         filter = fltdef.getString(FNAME).trim().toLowerCase();

      FilterDefinition def = filters.get(filter);
      if (def == null) throw new Exception("Unknown custom filter '"+filter+"'");

      String whcl = def.whcl;

      if (fltdef.has(PARAMS))
      {
         JSONArray pdef = fltdef.getJSONArray(PARAMS);

         for (int p = 0; p < pdef.length(); p++)
         {
            JSONObject param = pdef.getJSONObject(p);

            String name = param.getString("name");
            String value = param.getString("value");

            whcl = whcl.replace("{"+name.trim().toLowerCase()+"}",value);
         }
      }


      return(whcl);
   }


   public static class FilterDefinition
   {
      public final String name;
      public final String whcl;
      public final boolean restricts;
      public final ArrayList<String> params;

      private FilterDefinition(JSONObject fltdef)
      {
         String name = null;
         String whcl = null;
         boolean restricts = false;

         ArrayList<String> params = new ArrayList<String>();

         if (fltdef.has(FNAME))
            name = fltdef.getString(FNAME).trim().toLowerCase();

         if (fltdef.has(WHERECL))
            whcl = fltdef.getString(WHERECL);

         if (fltdef.has(RESTRCT))
            restricts = fltdef.getBoolean(RESTRCT);

         if (fltdef.has(PARAMS))
         {
            JSONArray pdef = fltdef.getJSONArray(PARAMS);

            for (int p = 0; p < pdef.length(); p++)
               params.add(pdef.getString(p).trim().toLowerCase());
         }

         whcl = normalize(whcl);

         this.name = name;
         this.whcl = whcl;
         this.params = params;
         this.restricts = restricts;
      }

      private String normalize(String whcl)
      {
         if (whcl == null) return(null);

         boolean param = false;
         Character quote = ' ';
         StringBuffer norm = new StringBuffer();

         for (int i = 0; i < whcl.length(); i++)
         {
            char c = whcl.charAt(i);

            if (c == '"') quote = quote == c ? ' ' : c;
            if (c == '\'') quote = quote == c ? ' ' : c;

            if (quote != ' ')
            {
               norm.append(c);
               continue;
            }

            if (c == '{')
            {
               param = true;
               norm.append(c);
               continue;
            }

            if (c == '}')
            {
               param = false;
               norm.append(c);
               continue;
            }

            if (param)
            {
               if (c == ' ') continue;
               c = Character.toLowerCase(c);
            }

            norm.append(c);
         }

         return(norm.toString());
      }

      @Override
      public String toString()
      {
         return(whcl);
      }
   }
}