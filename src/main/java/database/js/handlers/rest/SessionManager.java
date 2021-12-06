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

package database.js.handlers.rest;

import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.servers.Server;
import java.util.concurrent.ConcurrentHashMap;


public class SessionManager extends Thread
{
  private final Server server;
  private final Config config;
  private final static Logger logger = Logger.getLogger("rest");

  private final static ConcurrentHashMap<String,Session> sessions =
    new ConcurrentHashMap<String,Session>();
  
  
  public static synchronized String register(Session session)
  {
    String guid = null;

    while(guid == null)
    {
      guid = new Guid().toString();
      if (sessions.get(guid) != null) guid = null;
    }

    sessions.put(guid,session);
    return(guid);
  }
  
  
  public static Session get(String guid)
  {
    return(sessions.get(guid));
  }
  
  
  public static Session remove(String guid)
  {
    return(sessions.remove(guid));
  }



  public SessionManager(Server server)
  {
    this(server,false);
  }


  public SessionManager(Server server, boolean start)
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
    logger.info("SessionManager started");
    
    try
    {
      while(true)
      {
        Thread.sleep(10000);
      }
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
    }
  }
}