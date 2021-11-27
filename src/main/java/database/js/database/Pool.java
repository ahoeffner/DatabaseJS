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

import java.sql.Connection;
import java.util.ArrayList;
import java.sql.DriverManager;


public class Pool
{
  private int size = 0;

  private final int max;
  private final String url;
  private final String token;
  private final boolean proxy;
  private final ArrayList<Connection> pool;


  public Pool(boolean proxy, String token, String url, int size)
  {
    this.url = url;
    this.max = size;
    this.proxy = proxy;
    this.token = token;
    this.pool = new ArrayList<Connection>();
  }


  public boolean proxy()
  {
    return(proxy);
  }


  public Connection connect(String token) throws Exception
  {
    if (this.token != null)
    {
      if (token == null || !this.token.equals(token))
        throw new Exception("Invalid connect token");
    }

    Connection conn = DriverManager.getConnection(url);
    conn.setAutoCommit(false);

    return(conn);
  }


  public Connection getConnection(String token) throws Exception
  {
    if (this.token != null)
    {
      if (token == null || !this.token.equals(token))
        throw new Exception("Invalid connect token");
    }

    Connection conn = null;

    synchronized(this)
    {
      while(pool.size() == 0 && size == max)
        this.wait();

      if (pool.size() == 0) conn = connect();
      else                  conn = pool.remove(0);
    }

    return(conn);
  }


  public void release(Connection conn)
  {
    synchronized(this)
    {
      pool.add(conn);
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
        Connection conn = this.pool.remove(0);

        try {conn.close();}
        catch(Exception e) {;}
      }
    }
  }


  public void testAll(String sql)
  {
    synchronized(this)
    {
      int size = this.pool.size();

      for (int i = 0; i < size; i++)
      {
        Connection conn = this.pool.remove(0);
        if (test(conn,sql)) this.pool.add(conn);
      }
    }
  }


  public boolean test(Connection conn, String sql)
  {
    try {conn.createStatement().execute(sql);}
    catch(Exception e) {return(false);}
    return(true);
  }


  public Connection connect() throws Exception
  {
    return(connect(this.token));
  }
}