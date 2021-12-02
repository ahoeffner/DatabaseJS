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
import java.lang.reflect.Constructor;
import database.js.custom.SQLRewriter;
import database.js.custom.SQLValidator;
import database.js.database.DatabaseUtils;
import database.js.database.NameValuePair;


public class Database
{
  public final String url;
  public final String test;

  public final String repository;

  public final boolean compact;
  public final String dateformat;

  public final SQLRewriter rewriter;
  public final SQLValidator validator;

  public final Pool proxy;
  public final Pool anonymous;

  public final DatabaseType type;
  public final ArrayList<String> urlparts;

  private final NameValuePair<Boolean>[] savepoints;


  @SuppressWarnings("unchecked")
  Database(JSONObject config) throws Exception
  {
    JSONObject section = Config.getSection(config,"database");
    //********************* General Section *********************

    String type = Config.get(section,"type");

    type = Character.toUpperCase(type.charAt(0))
           + type.substring(1).toLowerCase();

    this.url = Config.get(section,"jdbc");
    this.test = Config.get(section,"test");

    this.type = DatabaseType.valueOf(type);
    this.urlparts = DatabaseUtils.parse(url);

    DatabaseUtils.setType(this.type);
    DatabaseUtils.setUrlParts(urlparts);

    section = Config.getSection(config,"resultset");
    //*********************  Data Section   *********************

    this.compact = Config.get(section,"compact");
    this.dateformat = Config.get(section,"dateformat",null);


    section = Config.getSection(config,"repository");
    //*********************  Repos Section  *********************

    String repo = Config.get(section,"path");
    this.repository = Config.getPath(repo,Paths.apphome);


    section = Config.getSection(config,"savepoints");
    //******************* Savepoint Section  *******************

    this.savepoints = new NameValuePair[2];
    this.savepoints[0] = new NameValuePair<Boolean>("post",Config.get(section,"post"));
    this.savepoints[1] = new NameValuePair<Boolean>("patch",Config.get(section,"patch"));


    section = Config.getSection(config,"interceptors");
    //****************** Interceptors Section ******************

    String rewclass = Config.get(section,"rewrite.class",null);
    String valclass = Config.get(section,"validator.class",null);

    if (rewclass == null) this.rewriter = null;
    else
    {
      Constructor contructor = Class.forName(rewclass).getDeclaredConstructor();
      this.rewriter = (SQLRewriter) contructor.newInstance();
    }

    if (valclass == null) this.validator = null;
    else
    {
      Constructor contructor = Class.forName(valclass).getDeclaredConstructor();
      this.validator = (SQLValidator) contructor.newInstance();
    }

    section = Config.getSection(config,"pools");
    //*********************  Pool Section  *********************

    this.proxy = getPool("proxy",section,true);
    this.anonymous = getPool("anonymous",section,false);
  }


  private Pool getPool(String type, JSONObject config, boolean proxy) throws Exception
  {
    if (!config.has(type)) return(null);
    JSONObject pconf = Config.getSection(config,type);

    type = Character.toUpperCase(type.charAt(0))
           + type.substring(1).toLowerCase();

    int size = Config.get(pconf,"pool");
    String usr = Config.get(pconf,"username");
    String pwd = Config.get(pconf,"password");
    String secret = Config.get(pconf,"auth.secret");

    return(new Pool(proxy,secret,usr,pwd,size));
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