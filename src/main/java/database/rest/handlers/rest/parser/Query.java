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

package database.rest.handlers.rest.parser;

import org.json.JSONArray;
import org.json.JSONObject;


public class Query implements SQLObject
{
   public final String order;
   public final String source;
   public final String[] columns;
   public final WhereClause whcl;

   public Query(JSONObject json) throws Exception
   {
      String order = null;
      String source = null;
      WhereClause whcl = null;
      String[] columns = new String[0];

      if (json.has(Parser.ORDER))
         order = json.getString(Parser.ORDER);

      if (json.has(Parser.SOURCE))
         source = json.getString(Parser.SOURCE);

      if (json.has(Parser.COLUMNS))
      {
         JSONArray jarr = json.getJSONArray(Parser.COLUMNS);
         columns = new String[jarr.length()];

         for (int i = 0; i < jarr.length(); i++)
            columns[i] = jarr.getString(i);
      }

      if (json.has(Parser.FILTERS))
      {
         whcl = new WhereClause();
         whcl.parse(json);
      }

      this.whcl = whcl;
      this.order = order;
      this.source = source;
      this.columns = columns;
   }

   public String toString()
   {
      String str = source;

      for (int i = 0; i < columns.length; i++)
      {
         str += " " + columns[i];
         if (i < columns.length - 1) str += ",";
      }

      if (order != null)
         str += " "+order;

      return(str);
   }
}
