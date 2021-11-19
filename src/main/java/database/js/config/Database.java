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

package database.js.config;

import org.json.JSONObject;


public class Database
{
  private final String connurl;
  private final String teststmt;
  private final DatabaseType type;


  Database(JSONObject config) throws Exception
  {
    String type = config.getString("type");
    type = Character.toUpperCase(type.charAt(0)) + type.substring(1);

    this.type = DatabaseType.valueOf(type);
    this.connurl = config.getString("url");
    this.teststmt = config.getString("test");
  }


  public DatabaseType type()
  {
    return(type);
  }

  public DatabaseType database()
  {
    return(type);
  }

  public String url()
  {
    return(connurl);
  }

  public String teststmt()
  {
    return(teststmt);
  }
}