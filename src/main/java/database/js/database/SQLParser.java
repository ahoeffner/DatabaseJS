/*
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.

 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

package database.js.database;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SQLParser
{
  private final String sql;
  private final boolean func;
  private final ArrayList<BindValue> bindings;
  private final HashMap<String,BindValueDef> bindvalues;

  private final static Pattern procedure = Pattern.compile("(\\w*\\.)?\\w*\\s*\\(.*\\)");
  private final static Pattern function = Pattern.compile("\\w*\\s*=\\s*(\\w*\\.)?\\w*\\s*\\(.*\\)");


  public static boolean function(String stmt)
  {
    if (stmt == null) return(false);
    Matcher matcher = function.matcher(stmt.trim());
    return(matcher.matches());
  }


  public static boolean procedure(String stmt)
  {
    if (stmt == null) return(false);
    Matcher matcher = procedure.matcher(stmt.trim());
    return(matcher.matches());
  }


  public SQLParser(HashMap<String,BindValueDef> bindvalues, String stmt)
  {
    this(bindvalues,stmt,false);
  }


  public String sql()
  {
    return(sql);
  }


  public ArrayList<BindValue> bindvalues()
  {
    return(bindings);
  }


  public SQLParser(HashMap<String,BindValueDef> bindvalues, String stmt, boolean procedure)
  {
    stmt = stmt.trim();
    this.bindvalues = bindvalues;
    StringBuffer nsql = new StringBuffer();
    this.bindings = new ArrayList<BindValue>();

    if (!procedure) func = false;
    else   func = function(stmt);

    if (func && !stmt.startsWith("&"))
    {
      if (!stmt.startsWith(":")) stmt = "&" + stmt;
      else                       stmt = "&" + stmt.substring(1);
    }

    StringBuffer sql = new StringBuffer(stmt);

    for (int i = 0; i < sql.length(); i++)
    {
      char c = sql.charAt(i);

      if (c == ':' || c == '&')
      {
        int len = extract(sql,i);
        BindValueDef bindv = validate(sql,i,len);

        if (bindv != null)
        {
          i += len;
          nsql.append('?');
          bindings.add(bindv.copy((i == 0 || c == '&')));
        }
        else
        {
          nsql.append(c);
        }
      }
      else
      {
        nsql.append(c);
      }
    }

    if (procedure)
    {
      String proc = nsql.toString();
      if (!func) this.sql = "call "+proc;
      else this.sql = "{" + proc.replace("=","= call") + "}";
    }
    else
    {
      this.sql = nsql.toString();
    }
  }


  private BindValueDef validate(StringBuffer sql, int pos, int len)
  {
    if (len < 0) return(null);
    String name = sql.substring(pos+1,pos+1+len);
    return(this.bindvalues.get(name));
  }


  private int extract(StringBuffer sql, int pos)
  {
    int start = pos;

    if (pos > 0)
    {
      char pre = sql.charAt(pos-1);
      if (wordCharacter(pre)) return(-1);
      if (pre == ':' || pre == '&') return(-1);
    }

    pos++;

    while(pos < sql.length() && wordCharacter(sql.charAt(pos)))
      pos++;

    return(pos-1-start);
  }


  private boolean wordCharacter(char c)
  {
    if (c == '_') return(true);

    if (c >= '0' && c <= '9') return(true);
    if (c >= 'a' && c <= 'z') return(true);
    if (c >= 'A' && c <= 'Z') return(true);

    return(false);
  }
}