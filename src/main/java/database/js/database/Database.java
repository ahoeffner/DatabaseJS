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
import database.js.config.Config;
import database.js.config.DatabaseType;


public abstract class Database
{
  private Connection conn;
  private long touched = System.currentTimeMillis();

  private static Config config = null;
  private static DatabaseType dbtype = null;
  private static ArrayList<String> urlparts = null;


  public static void init(Config config) throws Exception
  {
    Database.config = config;
    Database.dbtype = config.getDatabase().type();
    Database.urlparts = config.getDatabase().urlparts();
  }


  @SuppressWarnings("unchecked")
  public static Database getInstance() throws Exception
  {
    return((Database) dbtype.clazz.getConstructor().newInstance());
  }


  protected Database()
  {
  }


  public void touch()
  {
    touched = System.currentTimeMillis();
  }


  public long touched()
  {
    return(touched);
  }


  public void connect(String username, String password) throws Exception
  {
    String url = DatabaseUtils.bind(urlparts,username,password);
    this.conn = DriverManager.getConnection(url);
  }
}