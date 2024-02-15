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
import java.util.ArrayList;
import database.rest.database.BindValue;
import database.rest.database.filters.Filter;


public class WhereClause implements SQLObject, Filter
{
  private ArrayList<BindValue> bindvals =
    new ArrayList<BindValue>();

  private ArrayList<FilterEntry> entries =
    new ArrayList<FilterEntry>();


  @Override
  public String sql()
  {
    String sql = "(";
    sql += entries.get(0).filter.sql();

    for (int i = 1; i < entries.size(); i++)
      sql += " "+entries.get(i).operator + " " + entries.get(i).filter.sql();

    sql += ")";

    return(sql);
  }


  @Override
  public BindValue[] getBindValues()
  {
    return(this.bindvals.toArray(new BindValue[0]));
  }


  @Override
  public void parse(JSONObject definition) throws Exception
  {
    JSONArray filters = definition.getJSONArray(Parser.FILTERS);

    for (int i = 0; i < filters.length(); i++)
    {
      String operator = "and";
      JSONObject entry = filters.getJSONObject(i);

      if (entry.has(Parser.FILTER))
      {
        if (entry.has(Parser.OPERATOR))
          operator = entry.getString(Parser.OPERATOR);

        entry = entry.getJSONObject(Parser.FILTER);

        if (entry.has(Parser.CLASS))
        {
          String clazz = entry.getString(Parser.CLASS);

          Filter filter = Filters.get(clazz);
          filter.parse(entry);

          entries.add(new FilterEntry(operator,filter));
        }
      }
      else if (entry.has(Parser.FILTERS))
      {
        WhereClause whcl = new WhereClause();

        whcl.parse(entry);
        entries.add(new FilterEntry(operator,whcl));
      }
    }
  }


  private static class FilterEntry
  {
    final Filter filter;
    final String operator;

    FilterEntry(String operator, Filter filter)
    {
      this.filter = filter;
      this.operator = operator;
    }
  }
}