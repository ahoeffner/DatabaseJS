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
import java.util.HashSet;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import database.json.database.BindValue;
import database.json.database.filters.Like;
import database.json.database.filters.ILike;
import database.json.database.filters.Custom;
import database.json.database.filters.Equals;
import database.json.database.filters.Filter;


public class WhereClause implements Filter
{
  public static String deny(Source source)
  {
    return("The where clause on "+source.id+" is not allowed");
  }

  private final Source source;
  private String filter = null;

  private ArrayList<BindValue> bindvals =
    new ArrayList<BindValue>();

  private ArrayList<FilterEntry> entries =
    new ArrayList<FilterEntry>();


  public WhereClause(Source source)
  {
    this.source = source;
  }


  public boolean isEmpty()
  {
    return(entries.size() == 0 && filter == null);
  }


  public void filter(String filter)
  {
    this.filter = filter;
  }


  public boolean restricts()
  {
    boolean restricts = false;

    for (FilterEntry entry : entries)
    {
      restricts = false;

      if (entry.operator.equals("and"))
      {
        if (!(entry.filter instanceof WhereClause))
        {
          if (entry.filter instanceof Like)
            restricts = ((Like) entry.filter).restricts();
          else if (entry.filter instanceof ILike)
            restricts = ((ILike) entry.filter).restricts();
          else restricts = true;
        }
        else restricts = ((WhereClause) entry.filter).restricts();
      }
    }

    return(restricts);
  }


  public boolean unique(Source source)
  {
    String[] pkey = source.primary;

    if (pkey == null || pkey.length == 0)
      return(false);

    if (entries.size() != pkey.length)
      return(false);

    HashSet<String> keys = new HashSet<String>();
    for (String column : pkey) keys.add(column.toLowerCase());

    for (int i = 0; i < entries.size(); i++)
    {
      Filter eq = entries.get(i).filter;

      if (eq instanceof Equals)
      {
        if (!keys.remove(((Equals) eq).column().toLowerCase()))
          return(false);
      }
    }

    return(keys.size() == 0);
  }


  @Override
  public String sql()
  {
    String sql = "";

    if (entries.size() > 0)
    {
      sql += "(";
      sql += entries.get(0).filter.sql();

      for (int i = 1; i < entries.size(); i++)
        sql += " "+entries.get(i).operator + " " + entries.get(i).filter.sql();

      sql += ")";
    }

    if (filter != null)
    {
      if (sql.length() > 0) sql += " and ";
      sql += "(" + filter + ")";
    }

    return(sql);
  }


  @Override
  public BindValue[] getBindValues()
  {
    return(this.bindvals.toArray(new BindValue[0]));
  }


  public boolean validate(Source source, Check lim) throws Exception
  {
    if (lim == Check.singlerow)
    {
      if (!this.unique(source))
        throw new Exception("The where clause on "+source.id+" is not acceptable");
    }

    else

    if (lim == Check.restricted)
    {
      if (!this.restricts())
        throw new Exception("The where clause on "+source.id+" is not acceptable");
    }

    return(true);
  }


  @Override
  public void parse(JSONObject definition) throws Exception
  {
    if (!definition.has(Parser.FILTERS))
      return;

    JSONArray filters = definition.getJSONArray(Parser.FILTERS);

    for (int i = 0; i < filters.length(); i++)
    {
      String operator = "and";
      JSONObject entry = filters.getJSONObject(i);

      if (entry.has(Parser.OPERATOR))
        operator = entry.getString(Parser.OPERATOR);

      if (entry.has(Parser.FILTER))
      {
        entry = entry.getJSONObject(Parser.FILTER);

        if (entry.has(Parser.CLASS))
        {
          String clazz = entry.getString(Parser.CLASS);
          Filter filter = Filters.get(clazz,entry);

          if (filter instanceof Custom)
            ((Custom) filter).source(source);

          filter.parse(entry);
          this.bindvals.addAll(Arrays.asList(filter.getBindValues()));

          entries.add(new FilterEntry(operator,filter));
        }
      }
      else if (entry.has(Parser.FILTERS))
      {
        WhereClause whcl = new WhereClause(source);

        whcl.parse(entry);
        this.bindvals.addAll(Arrays.asList(whcl.getBindValues()));

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