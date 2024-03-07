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
import org.json.JSONArray;
import org.json.JSONObject;


public class Source
{
   private static final String ID = "id";
   private static final String SQL = "sql";
   private static final String QUERY = "query";
   private static final String TABLE = "table";
   private static final String ORDER = "order";
   private static final String ACCEPT = "accept";
   private static final String SELECT = "select";
   private static final String INSERT = "insert";
   private static final String UPDATE = "update";
   private static final String DELETE = "delete";
   private static final String FUNCTION = "name";

   private static final String RELAXED = "relaxed";
   private static final String SINGLEROW = "singlerow";
   private static final String PRIMARYKEY = "primarykey";


   private static HashMap<String,Source> sources =
      new HashMap<String,Source>();


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
   public final SourceType type;
   public final String function;
   public final String[] primary;

   public final boolean selectallowed;
   public final boolean insertallowed;
   public final boolean updateallowed;
   public final boolean deleteallowed;

   public final boolean selectrelaxed;
   public final boolean updaterelaxed;
   public final boolean deleterelaxed;

   public final boolean selectsinglerow;
   public final boolean updatesinglerow;
   public final boolean deletesinglerow;


   public Source(SourceType type, JSONObject definition) throws Exception
   {
      String id = null;
      String stmt = null;
      String query = null;
      String table = null;
      String order = null;
      String function = null;
      String[] primary = null;

      boolean selectallowed = false;
      boolean insertallowed = false;
      boolean updateallowed = false;
      boolean deleteallowed = false;

      boolean selectrelaxed = false;
      boolean updaterelaxed = false;
      boolean deleterelaxed = false;

      boolean updatesinglerow = true;
      boolean deletesinglerow = true;
      boolean selectsinglerow = false;

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

         if (definition.has(INSERT))
         {
            JSONObject sec = definition.getJSONObject(INSERT);
            if (sec.has(ACCEPT)) insertallowed = sec.getBoolean(ACCEPT);
         }

         if (definition.has(UPDATE))
         {
            JSONObject sec = definition.getJSONObject(UPDATE);
            if (sec.has(ACCEPT)) updateallowed = sec.getBoolean(ACCEPT);
            if (sec.has(RELAXED)) updaterelaxed = sec.getBoolean(RELAXED);
            if (sec.has(SINGLEROW)) updatesinglerow = sec.getBoolean(SINGLEROW);
         }

         if (definition.has(DELETE))
         {
            JSONObject sec = definition.getJSONObject(DELETE);
            if (sec.has(ACCEPT)) deleteallowed = sec.getBoolean(ACCEPT);
            if (sec.has(RELAXED)) deleterelaxed = sec.getBoolean(RELAXED);
            if (sec.has(SINGLEROW)) deletesinglerow = sec.getBoolean(SINGLEROW);
         }

         if (definition.has(SELECT))
         {
            JSONObject sec = definition.getJSONObject(SELECT);
            if (sec.has(ACCEPT)) selectallowed = sec.getBoolean(ACCEPT);
            if (sec.has(RELAXED)) selectrelaxed = sec.getBoolean(RELAXED);
            if (sec.has(SINGLEROW)) selectsinglerow = sec.getBoolean(SINGLEROW);
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
      }
      else if (type == SourceType.function)
      {
         function = id;

         if (definition.has(FUNCTION))
            function = definition.getString(FUNCTION);
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
         }
      }

      this.type = type;
      this.stmt = stmt;
      this.query = query;
      this.table = table;
      this.order = order;
      this.primary = primary;
      this.function = function;

      this.selectallowed = selectallowed;
      this.insertallowed = insertallowed;
      this.updateallowed = updateallowed;
      this.deleteallowed = deleteallowed;

      this.selectrelaxed = selectrelaxed;
      this.updaterelaxed = updaterelaxed;
      this.deleterelaxed = deleterelaxed;

      this.selectsinglerow = selectsinglerow;
      this.updatesinglerow = updatesinglerow;
      this.deletesinglerow = deletesinglerow;
   }
}