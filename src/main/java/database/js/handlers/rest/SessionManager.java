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
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.database.Pool;
import database.js.servers.Server;
import database.js.cluster.PreAuthTable;
import database.js.cluster.PreAuthRecord;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;


public class SessionManager
{
  private final Server server;
  private final Config config;
  private final SSOReaper ssoreaper;
  private final SessionReaper sesreaper;
  private final static Logger logger = Logger.getLogger("rest");

  private final static ConcurrentHashMap<String,PreAuthRecord> preauth =
    new ConcurrentHashMap<String,PreAuthRecord>();

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


  public static synchronized PreAuthRecord preauth(String username)
  {
    String guid = null;

    while(guid == null)
    {
      guid = new Guid().toString();
      if (preauth.get(guid) != null) guid = null;
    }

    PreAuthRecord rec = new PreAuthRecord(guid,username);
    preauth.put(guid,rec);

    return(rec);
  }


  public static void refresh(PreAuthTable.Reader reader)
  {
    if (reader != null)
    {
      ArrayList<PreAuthRecord> records = reader.refresh();
      for(PreAuthRecord rec : records) preauth.put(rec.guid,rec);
    }
  }


  public static PreAuthRecord validate(String guid)
  {
    if (guid == null) return(null);
    else return(preauth.remove(guid));
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
    sessions.remove(guid);
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
    this.ssoreaper = new SSOReaper(server);
    this.sesreaper = new SessionReaper(server);
    if (start) startSessionManager();
  }


  public void startSessionManager()
  {
    sesreaper.start();
  }


  public void startSSOManager()
  {
    ssoreaper.start();
  }


  private static class SSOReaper extends Thread
  {
    private final Server server;
    private final Config config;

    SSOReaper(Server server)
    {
      this.server = server;
      this.config = server.config();

      this.setDaemon(true);
      this.setName("SSOReaper");
    }


    @Override
    public void run()
    {
      logger.info("SSOReaper started");
      ArrayList<String> remove = new ArrayList<String>();

      try
      {
        int timeout = config.getREST().ssotimeout * 1000;

        while(true)
        {
          Thread.sleep(timeout/4);
          long time = System.currentTimeMillis();

          for(Map.Entry<String,PreAuthRecord> entry : preauth.entrySet())
          {
            PreAuthRecord sso = entry.getValue();

            if (time - sso.time > timeout)
            {
              remove.add(sso.guid);
              logger.fine("SSO: "+sso.guid+" timed out");
            }
          }
        }
      }
      catch (Exception e)
      {
        logger.log(Level.SEVERE,e.getMessage(),e);
      }

      for(String guid : remove)
        preauth.remove(guid);
    }
  }


  private static class SessionReaper extends Thread
  {
    private final Server server;
    private final Config config;

    SessionReaper(Server server)
    {
      this.server = server;
      this.config = server.config();

      this.setDaemon(true);
      this.setName("SessionReaper");
    }


    @Override
    public void run()
    {
      logger.info("SessionReaper started");

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

            Pool pp = config.getDatabase().proxy;
            Pool ap = config.getDatabase().anonymous;

            if (pp != null) logger.info(pp.toString());
            if (ap != null) logger.info(ap.toString());

            last = System.currentTimeMillis();
          }

          for(Map.Entry<String,Session> entry : sessions.entrySet())
          {
            Session session = entry.getValue();

            if (time - session.touched() > timeout)
            {
              System.err.println((new Date())+" cleanout "+session.guid()+" "+session.clients());
              session.disconnect(true);
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
}