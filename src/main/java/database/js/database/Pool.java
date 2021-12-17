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
  private boolean closed = false;

  private final int min;
  private final int max;
  private final int idle;
  private final String token;
  private final boolean proxy;
  private final String username;
  private final String password;
  private final ArrayList<Database> pool;
  private final static Logger logger = Logger.getLogger("rest");


  public Pool(boolean proxy, String token, String username, String password, int min, int max, int idle) throws Exception
  {
    this.min = min;
    this.max = max;
    this.idle = idle;
    this.proxy = proxy;
    this.token = token;
    this.username = username;
    this.password = password;
    this.pool = new ArrayList<Database>();
  }


  public int min()
  {
    return(min);
  }


  public int max()
  {
    return(max);
  }


  public int idle()
  {
    return(idle);
  }


  public boolean proxy()
  {
    return(proxy);
  }


  void init()
  {
    Initiator init = new Initiator(this);
    init.start();
  }


  synchronized void add(Database database)
  {
    pool.add(database);
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


  public synchronized boolean remove(Database database)
  {
    if (!pool.remove(database))
      return(false);

    database.disconnect();
    logger.fine("Pool["+(proxy ? "proxy" : "anonymous")+"] connection closed");

    return(true);
  }


  public Database getConnection() throws Exception
  {
    return(getConnection(token));
  }


  public Database getConnection(String token) throws Exception
  {
    if (closed)
      throw new Exception("Pool closed");

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
        return;
      }
    }

    synchronized(this)
    {
      database.touch();
      pool.add(0,database);
      this.notifyAll();
    }
  }


  public void close()
  {
    synchronized(this)
    {
      closed = true;
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


  ArrayList<Database> connections()
  {
    synchronized(this)
    {return(new ArrayList<Database>(pool));}
  }


  private static class Initiator extends Thread
  {
    private final Pool pool;

    Initiator(Pool pool)
    {
      this.pool = pool;
      this.setDaemon(true);
      this.setName("Pool initiator");
    }

    @Override
    public void run()
    {
      try
      {
        for (int i = 0; i < pool.min; i++)
        {
          Database database = DatabaseUtils.getInstance();
          database.connect(pool.username,pool.password);
          pool.add(database);
        }
      }
      catch (Exception e)
      {
        logger.log(Level.WARNING,e.getMessage(),e);
      }
    }
  }
}