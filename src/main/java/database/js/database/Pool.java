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
import java.util.logging.Level;
import java.util.logging.Logger;


public class Pool
{
  private int size = 0;

  private final int max;
  private final String token;
  private final boolean proxy;
  private final String username;
  private final String password;
  private final ArrayList<Database> pool;
  private final static Logger logger = Logger.getLogger("rest");


  public Pool(boolean proxy, String token, String username, String password, int size) throws Exception
  {
    this.max = size;
    this.proxy = proxy;
    this.token = token;
    this.username = username;
    this.password = password;
    this.pool = new ArrayList<Database>();
  }


  public boolean proxy()
  {
    return(proxy);
  }


  public Database connect(String token) throws Exception
  {
    if (this.token != null)
    {
      if (token == null || !this.token.equals(token))
        throw new Exception("Invalid connect token");
    }

    Database database = DatabaseUtils.getInstance();
    database.connect(username,password);

    return(database);
  }


  public Database getConnection(String token) throws Exception
  {
    if (this.token != null)
    {
      if (token == null || !this.token.equals(token))
        throw new Exception("Invalid connect token");
    }

    Database database = null;

    synchronized(this)
    {
      while(pool.size() == 0 && size == max)
        this.wait();

      if (pool.size() == 0) database = connect();
      else                  database = pool.remove(0);
    }

    return(database);
  }


  public void release(Database database)
  {
    if (proxy)
    {
      try
      {
        database.releaseProxyUser();
      }
      catch (Exception e)
      {
        logger.log(Level.SEVERE,e.getMessage(),e);
      }
    }

    synchronized(this)
    {
      pool.add(database);
      this.notifyAll();
    }
  }


  public void close()
  {
    synchronized(this)
    {
      int size = this.pool.size();

      for (int i = 0; i < size; i++)
      {
        Database database = this.pool.remove(0);

        try {database.disconnect();}
        catch(Exception e) {;}
      }
    }
  }


  public void validate()
  {
    synchronized(this)
    {
      int size = this.pool.size();

      for (int i = 0; i < size; i++)
      {
        Database database = this.pool.remove(0);
        if (database.validate()) this.pool.add(database);
      }
    }
  }


  public Database connect() throws Exception
  {
    return(connect(this.token));
  }
}