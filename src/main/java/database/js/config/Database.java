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

import java.util.ArrayList;
import org.json.JSONObject;
import database.js.database.DatabaseUtils;


public class Database
{
  private final String url;
  private final String test;
  private final DatabaseType type;
  private final ArrayList<String> urlparts;


  Database(JSONObject config) throws Exception
  {
    String type = config.getString("type");

    type = Character.toUpperCase(type.charAt(0))
           + type.substring(1).toLowerCase();

    this.url = config.getString("jdbc");
    this.test = config.getString("test");
    this.type = DatabaseType.valueOf(type);
    this.urlparts = DatabaseUtils.parse(url);
  }


  public DatabaseType type()
  {
    return(type);
  }

  public String url()
  {
    return(url);
  }

  public ArrayList<String> urlparts()
  {
    return(urlparts);
  }

  public String test()
  {
    return(test);
  }
}