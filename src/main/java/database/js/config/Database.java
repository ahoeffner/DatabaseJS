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

import java.io.File;
import java.util.ArrayList;
import org.json.JSONObject;
import database.js.database.Pool;
import database.js.database.DatabaseUtils;
import database.js.database.NameValuePair;


public class Database
{
  private final String url;
  private final String test;

  private final String repo;

  private final boolean compact;
  private final String dateform;

  private final String rewclass;
  private final String valclass;

  private final Pool proxy;
  private final Pool anonymous;

  private final DatabaseType type;
  private final ArrayList<String> urlparts;

  private final NameValuePair<Boolean>[] savepoints;


  @SuppressWarnings("unchecked")
  Database(JSONObject config) throws Exception
  {
    JSONObject section = config.getJSONObject("database");
    //********************* General Section *********************

    String type = section.getString("type");

    type = Character.toUpperCase(type.charAt(0))
           + type.substring(1).toLowerCase();

    this.url = section.getString("jdbc");
    this.test = section.getString("test");

    this.type = DatabaseType.valueOf(type);
    this.urlparts = DatabaseUtils.parse(url);

    DatabaseUtils.setType(this.type);
    DatabaseUtils.setUrlParts(urlparts);

    section = config.getJSONObject("resultset");
    //*********************  Data Section   *********************

    this.compact = section.getBoolean("compact");

    if (section.isNull("dateformat")) this.dateform = null;
    else   this.dateform = section.getString("dateformat");


    section = config.getJSONObject("repository");
    //*********************  Repos Section  *********************

    String repo = section.getString("path");

    if (repo.startsWith("." + File.separator))
    {
      repo = Paths.apphome + File.separator + repo;
      File appf = new File(repo);
      repo = appf.getCanonicalPath();
    }

    this.repo = repo;

    section = config.getJSONObject("savepoints");
    //******************* Savepoint Section  *******************

    this.savepoints = new NameValuePair[2];
    this.savepoints[0] = new NameValuePair<Boolean>("post",section.getBoolean("post"));
    this.savepoints[1] = new NameValuePair<Boolean>("patch",section.getBoolean("patch"));

    section = config.getJSONObject("interceptors");
    //****************** Interceptors Section ******************

    if (section.isNull("rewrite.class")) this.rewclass = null;
    else   this.rewclass = section.getString("rewrite.class");

    if (section.isNull("validator.class")) this.valclass = null;
    else   this.valclass = section.getString("validator.class");


    section = config.getJSONObject("pools");
    //*********************  Pool Section  *********************

    this.proxy = getPool("proxy",section,true);
    this.anonymous = getPool("anonymous",section,false);
  }


  private Pool getPool(String type, JSONObject config, boolean proxy)
  {
    if (!config.has(type)) return(null);
    JSONObject pconf = config.getJSONObject(type);

    type = Character.toUpperCase(type.charAt(0))
           + type.substring(1).toLowerCase();

    int size = pconf.getInt("pool");
    String usr = pconf.getString("username");
    String pwd = pconf.getString("password");
    String secret = pconf.getString("auth.secret");

    return(new Pool(proxy,secret,DatabaseUtils.bind(usr,pwd),size));
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


  public boolean compact()
  {
    return(compact);
  }


  public String dateformat()
  {
    return(dateform);
  }


  public String rewrite()
  {
    return(rewclass);
  }


  public String validator()
  {
    return(valclass);
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