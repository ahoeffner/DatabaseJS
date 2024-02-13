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

import database.rest.database.BindValue;
import database.rest.database.filters.Filter;

import java.util.ArrayList;


public class WhereClause implements SQLObject, Filter
{
  private String operator = "and";

  private ArrayList<BindValue> bindvals =
    new ArrayList<BindValue>();

  private ArrayList<FilterEntry> entries =
    new ArrayList<FilterEntry>();


  @Override
  public String sql()
  {
    return(null);
  }


  @Override
  public BindValue[] getBindValues()
  {
    return(this.bindvals.toArray(new BindValue[0]));
  }


  @Override
  public void parse(JSONObject definition) throws Exception
  {
    throw new UnsupportedOperationException("Unimplemented method 'parse'");
  }


  public WhereClause(JSONArray filters) throws Exception
  {
    this(filters,null);
  }


  public WhereClause(JSONArray filters, String prefix) throws Exception
  {
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
          entries.add(new FilterEntry(operator,Filters.get(clazz)));
        }
      }
      else if (entry.has(Parser.FILTERS))
      {
        String next = prefix != null ? prefix+"" : "";
        WhereClause whcl = new WhereClause(entry.getJSONArray(Parser.FILTERS),next);
        if (entry.has(Parser.OPERATOR)) whcl.operator = entry.getString(Parser.OPERATOR);
        entries.add(new FilterEntry(whcl));
      }
    }
  }

  private static class FilterEntry
  {
    final Filter filter;
    final String operator;

    FilterEntry(WhereClause whcl)
    {
      this.filter = whcl;
      this.operator = whcl.operator;
    }

    FilterEntry(String operator, Filter filter)
    {
      this.filter = filter;
      this.operator = operator;
    }
  }
}