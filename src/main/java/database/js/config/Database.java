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
import database.js.database.Pool;
import database.js.database.PoolType;
import database.js.database.DatabaseUtils;


public class Database
{
  private final String url;
  private final String test;

  private final Pool proxy;
  private final Pool anonymous;

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

    this.proxy = getPool("proxy",config);
    this.anonymous = getPool("anonymous",config);
  }


  private Pool getPool(String type, JSONObject config)
  {
    if (!config.has(type)) return(null);
    JSONObject pconf = config.getJSONObject(type);

    type = Character.toUpperCase(type.charAt(0))
           + type.substring(1).toLowerCase();

    int size = pconf.getInt("size");
    PoolType ptype = PoolType.valueOf(type);
    String usr = pconf.getString("username");
    String pwd = pconf.getString("password");
    String secret = pconf.getString("auth.secret");

    return(new Pool(ptype,secret,DatabaseUtils.bind(urlparts,usr,pwd),size));
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

  public Pool proxy()
  {
    return(proxy);
  }

  public Pool anonymous()
  {
    return(anonymous);
  }
}