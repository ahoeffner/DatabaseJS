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

import java.util.ArrayList;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import database.js.config.Config;
import database.js.config.DatabaseType;


public abstract class Database
{
  private Connection conn;

  private static Config config = null;
  private static DatabaseType dbtype = null;
  private static ArrayList<String> connstr = null;


  public static void main(String[] args)
  {
    parse("jdbc:postgresql://localhost/test?user=[user]&password=[password]&ssl=true");
  }


  public static void init(Config config) throws Exception
  {
    Database.config = config;
    Database.dbtype = config.getDatabase().type();
    Database.connstr = parse(config.getDatabase().url());
  }


  @SuppressWarnings("unchecked")
  public static Database getInstance() throws Exception
  {
    return((Database) dbtype.clazz.getConstructor().newInstance());
  }


  private static ArrayList<String> parse(String url)
  {
    ArrayList<String> connstr = new ArrayList<String>();
    Pattern pattern = Pattern.compile("\\[(username|password)\\]");
    Matcher matcher = pattern.matcher(url.toLowerCase());

    int pos = 0;
    while(matcher.find())
    {
      int e = matcher.end();
      int b = matcher.start();

      if (b > pos)
        connstr.add(url.substring(pos,b));

      connstr.add(url.substring(b,e).toLowerCase());
      pos = e;
    }

    if (pos < url.length())
      connstr.add(url.substring(pos));

    return(connstr);
  }


  protected Database()
  {
  }


  public void connect(String username, String password) throws Exception
  {
    String url = "";

    for(String part : connstr)
    {
      if (part.equals("[username]")) url += username;
      else if (part.equals("[password]")) url += password;
      else url += part;
    }

    this.conn = DriverManager.getConnection(url);
  }
}