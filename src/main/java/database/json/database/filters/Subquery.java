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

package database.json.database.filters;

import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;
import database.json.database.BindValue;
import database.json.handlers.json.parser.Query;
import database.json.handlers.json.parser.Parser;


public class Subquery implements Filter
{
   private String query = null;
   private String[] columns = null;
   private BindValue[] bindvalues = null;

   @Override
   public String sql()
   {
      if (query == null || columns == null || columns.length == 0)
         return("1 = 2");

      String clause = "("+columns[0];

      for (int i = 1; i < columns.length; i++)
         clause += "," + columns[i];

      clause += ") in (" + query + ")";
      return(clause);
   }

   @Override
   public BindValue[] getBindValues()
   {
      return(bindvalues);
   }

   @Override
   public void parse(JSONObject definition) throws Exception
   {
      ArrayList<BindValue> bindvals = new ArrayList<BindValue>();

      if (definition.has(Parser.QUERY))
      {
         Query query = new Query(definition.getJSONObject(Parser.QUERY));
         bindvals.addAll(Arrays.asList(query.getBindValues()));
         this.query = query.sql();
      }

      if (definition.has(Parser.COLUMNS))
      {
         JSONArray cols = definition.getJSONArray(Parser.COLUMNS);

         this.columns = new String[cols.length()];
         for (int i = 0; i < this.columns.length; i++)
            this.columns[i] = cols.getString(i);
      }

      bindvals.addAll(Arrays.asList(Parser.getBindValues(definition)));
      this.bindvalues = bindvals.toArray(new BindValue[0]);
   }
}