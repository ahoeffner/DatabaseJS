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

import java.util.Map;
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
    if (guid == null) return(null);
    Session session = sessions.get(guid);
    if (session != null) session.share();
    return(session);
  }


  public static boolean remove(String guid)
  {
    Session session = sessions.remove(guid);

    synchronized(session)
    {
      if (session.clients() != 0)
      {
        sessions.put(session.guid(),session);
        return(false);
      }
    }

    return(true);
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
      int timeout = config.getREST().timeout * 1000;

      while(true)
      {
        Thread.sleep(timeout/4);
        long time = System.currentTimeMillis();

        for(Map.Entry<String,Session> entry : sessions.entrySet())
        {
          Session session = entry.getValue();

          if (time - session.touched() > timeout)
          {
            System.out.println("session 1 : "+session);
            session.share();
            session.disconnect();
            System.out.println("session 2: "+session);
            logger.fine("Session: "+session.guid()+" timed out");
          }
        }
      }
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
    }
  }
}