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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;


public class Source
{
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
   public final String sql;
   public final String table;
   public final String order;
   public final SourceType type;
   public final String[] primary;


   public Source(JSONObject definition) throws Exception
   {
      String sql = null;
      String table = null;
      String order = null;
      String[] primary = null;

      this.id = definition.getString("id");

      if (definition.has("table"))
         table = definition.getString("table");

      if (definition.has("order"))
         order = definition.getString("order");

      if (definition.has("primarykey"))
      {
         String pkey = definition.getString("primarykey");

         primary = pkey.split((","));
         for (int i = 0; i < primary.length; i++)
            primary[i] = primary[i].trim();
      }

      if (definition.has("sql"))
      {
         table = null;
         Object obj = definition.get("sql");
         if (obj instanceof String) sql = (String) obj;

         else if (obj instanceof JSONArray)
         {
            sql = "";
            JSONArray lines = (JSONArray) obj;

            for (int i = 0; i < lines.length(); i++)
               sql += lines.getString(i) + " ";

            sql = sql.trim();
         }
      }

      this.sql = sql;
      this.table = table;
      this.order = order;
      this.primary = primary;
      this.type = table != null ? SourceType.table : SourceType.query;
   }
}