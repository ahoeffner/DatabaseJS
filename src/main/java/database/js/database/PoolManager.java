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
import database.js.config.Config;
import database.js.servers.Server;


public class PoolManager extends Thread
{
  private final Server server;
  private final Config config;
  private final static Logger logger = Logger.getLogger("rest");


  public PoolManager(Server server)
  {
    this(server,false);
  }


  public PoolManager(Server server, boolean start)
  {
    this.server = server;
    this.config = server.config();

    this.setDaemon(true);
    this.setName("PoolManager");

    if (start) this.start();
  }


  @Override
  public void run()
  {
    logger.info("PoolManager started");

    try
    {
      ArrayList<Database> conns = null;

      Pool pp = config.getDatabase().proxy;
      Pool ap = config.getDatabase().anonymous;

      if (ap == null && pp == null)
        return;

      int pidle = (pp == null) ? 3600000 : pp.idle();
      int aidle = (ap == null) ? 3600000 : ap.idle();
      int sleep = (pidle < aidle) ? pidle * 1000/4 : aidle * 1000/4;

      while(true)
      {
        Thread.sleep(sleep);

        if (ap != null)
          cleanout(ap);

        if (pp != null)
          cleanout(ap);
      }
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
    }
  }
  
  
  private void cleanout(Pool pool)
  {
    long time = System.currentTimeMillis();
    ArrayList<Database> conns = pool.connections();
    
    int min = pool.min();
    int size = conns.size();
    long idle = pool.idle() * 1000;
    
    for (int i = conns.size() - 1; i >= 0 && size > min; i--)
    {
      Database conn = conns.get(i);
      if (time - conn.touched() > idle)
      {
        size--;
        pool.remove(conn);
      }
    }
  }
}