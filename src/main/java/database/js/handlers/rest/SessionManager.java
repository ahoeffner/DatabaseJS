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

  private final static ConcurrentHashMap<String,String> preauth =
    new ConcurrentHashMap<String,String>();

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


  public static synchronized String preauth(String username)
  {
    String guid = null;

    while(guid == null)
    {
      guid = new Guid().toString();
      if (preauth.get(guid) != null) guid = null;
    }

    preauth.put(guid,username);
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
      int dump = config.getREST().dump * 1000;
      int timeout = config.getREST().timeout * 1000;

      int sleep = timeout/4;
      if (sleep > dump) sleep = dump;

      long last = System.currentTimeMillis();

      while(true)
      {
        Thread.sleep(sleep);
        long time = System.currentTimeMillis();

        if (dump > 0 && time - last >= dump && sessions.size() > 0)
        {
          String dmp = "\n";
          dmp += "--------------------------------------------------------------------------\n";
          dmp += "                              Sessions\n";
          dmp += "--------------------------------------------------------------------------\n";

          for(Map.Entry<String,Session> entry : sessions.entrySet())
            dmp += entry.getValue()+"\n";

          dmp += "--------------------------------------------------------------------------\n";

          logger.info(dmp);
          last = System.currentTimeMillis();
        }

        for(Map.Entry<String,Session> entry : sessions.entrySet())
        {
          Session session = entry.getValue();

          if (time - session.touched() > timeout)
          {
            session.share();
            session.disconnect();
            logger.fine("Session: timed out "+session.guid());
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