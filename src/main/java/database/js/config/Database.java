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
import database.js.handlers.rest.Rest;
import database.js.database.DatabaseUtils;
import database.js.database.NameValuePair;


public class Database
{
  private final String url;
  private final String test;
  private final String repo;

  private final Pool proxy;
  private final Pool anonymous;

  private final DatabaseType type;
  private final ArrayList<String> urlparts;

  private final NameValuePair<Boolean>[] savepoints;


  @SuppressWarnings("unchecked")
  Database(JSONObject config) throws Exception
  {
    String type = config.getString("type");

    type = Character.toUpperCase(type.charAt(0))
           + type.substring(1).toLowerCase();

    this.url = config.getString("jdbc");
    this.test = config.getString("test");
    this.type = DatabaseType.valueOf(type);
    this.urlparts = DatabaseUtils.parse(url);

    DatabaseUtils.setType(this.type);
    DatabaseUtils.setUrlParts(urlparts);

    this.repo = config.getString("repository");

    this.savepoints = new NameValuePair[2];
    JSONObject savep = config.getJSONObject("savepoint.defaults");

    this.savepoints[0] = new NameValuePair<Boolean>("post",savep.getBoolean("post"));
    this.savepoints[1] = new NameValuePair<Boolean>("patch",savep.getBoolean("patch"));
    Rest.setDefaultSavepoint(this.savepoints[0].getValue(),this.savepoints[1].getValue());

    this.proxy = getPool("proxy",config);
    this.anonymous = getPool("anonymous",config);
  }


  private Pool getPool(String type, JSONObject config)
  {
    if (!config.has(type)) return(null);
    JSONObject pconf = config.getJSONObject(type);

    type = Character.toUpperCase(type.charAt(0))
           + type.substring(1).toLowerCase();

    int size = pconf.getInt("pool");
    String usr = pconf.getString("username");
    String pwd = pconf.getString("password");
    String secret = pconf.getString("auth.secret");

    return(new Pool(secret,DatabaseUtils.bind(usr,pwd),size));
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

  public String repository()
  {
    return(repo);
  }

  public Pool proxy()
  {
    return(proxy);
  }

  public Pool anonymous()
  {
    return(anonymous);
  }


  public boolean savepoint(String type)
  {
    for(NameValuePair<Boolean> sp : this.savepoints)
    {
      if (sp.getName().equals(type))
        return(sp.getValue());
    }
    return(false);
  }
}