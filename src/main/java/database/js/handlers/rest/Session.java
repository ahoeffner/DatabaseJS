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

import java.sql.ResultSet;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.database.Pool;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import database.js.database.Database;
import database.js.database.BindValue;
import database.js.database.AuthMethod;
import database.js.database.NameValuePair;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;


public class Session
{
  private final Pool pool;
  private final String guid;
  private final String secret;
  private final String username;
  private final AuthMethod method;
  private final boolean dedicated;

  private int shared = 0;
  private long thread = 0;
  private Database database = null;
  private boolean exclusive = false;
  private final Object LOCK = new Object();
  private long touched = System.currentTimeMillis();

  private final ConcurrentHashMap<String,Cursor> cursors =
    new ConcurrentHashMap<String,Cursor>();

  private final static ConcurrentHashMap<String,Session> sessions =
    new ConcurrentHashMap<String,Session>();

  private final static Logger logger = Logger.getLogger("rest");


  public static Session get(String guid)
  {
    return(sessions.get(guid));
  }


  public Session(AuthMethod method, Pool pool, boolean dedicated, String username, String secret) throws Exception
  {
    this.pool = pool;
    this.guid = create();
    this.method = method;
    this.secret = secret;
    this.username = username;
    this.dedicated = dedicated;
  }


  public void touch()
  {
    touched = System.currentTimeMillis();
  }


  public long touched()
  {
    return(touched);
  }


  public String guid()
  {
    return(guid);
  }


  public boolean dedicated()
  {
    return(dedicated);
  }


  public Database database()
  {
    return(database);
  }


  public boolean connected()
  {
    return(database != null);
  }


  public void remove()
  {
    sessions.remove(guid);
  }


  public void disconnect()
  {
    if (database == null)
      return;

    try {database.rollback();}
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
    }

    if (pool != null) database.disconnect();
    else              pool.release(database);

    database = null;
  }


  public synchronized void ensure() throws Exception
  {
    if (!connected())
      connect();
  }


  public void connect() throws Exception
  {
    switch(method)
    {
      case OAuth :
        database = pool.connect();

        database.setAutoCommit(false);
        if (pool.proxy()) database.setProxyUser(username);

        break;

      case Database :
        database.connect(username,secret);
        database.setAutoCommit(false);

        break;

      case PoolToken :
        if (dedicated) database = pool.connect(secret);
        else           database = pool.getConnection(secret);

        database.setAutoCommit(false);
        if (pool.proxy()) database.setProxyUser(username);

        break;
    }
  }


  public Savepoint setSavePoint() throws Exception
  {
    return(database.setSavePoint());
  }


  public boolean releaseSavePoint(Savepoint savepoint)
  {
    return(releaseSavePoint(savepoint,false));
  }


  public boolean releaseSavePoint(Savepoint savepoint, boolean rollback)
  {
    if (savepoint == null)
      return(true);

    try
    {
      database.releaseSavePoint(savepoint,rollback);
      return(true);
    }
    catch (Throwable e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);

      if (!rollback)
      {
        try {database.releaseSavePoint(savepoint,rollback);}
        catch (Throwable rb) {;}
      }

      return(false);
    }
  }


  public int executeUpdate(String sql, ArrayList<BindValue> bindvalues) throws Exception
  {
    PreparedStatement stmt = database.prepare(sql,bindvalues);
    return(database.executeUpdate(stmt));
  }


  public Cursor executeQuery(String name, String sql, ArrayList<BindValue> bindvalues) throws Exception
  {
    PreparedStatement stmt = database.prepare(sql,bindvalues);
    ResultSet         rset = database.executeQuery(stmt);

    Cursor cursor = new Cursor(name,stmt,rset);
    if (name != null) cursors.put(name,cursor);

    return(cursor);
  }


  public ArrayList<NameValuePair<Object>> executeCall(String sql, ArrayList<BindValue> bindvalues, String dateform) throws Exception
  {
    boolean timeconv = false;
    DateTimeFormatter formatter = null;

    if (dateform != null)
    {
      if (dateform.equals("UTC")) timeconv = true;
      else formatter = DateTimeFormatter.ofPattern(dateform);
    }

    CallableStatement stmt = database.prepareCall(sql,bindvalues);
    return(database.execute(stmt,bindvalues,timeconv,formatter));
  }


  public String[] getColumnNames(Cursor cursor) throws Exception
  {
    return(database.getColumNames(cursor.rset));
  }


  public ArrayList<Object[]> fetch(Cursor cursor, int skip) throws Exception
  {
    boolean timeconv = false;
    DateTimeFormatter formatter = null;

    if (cursor.dateformat != null)
    {
      if (cursor.dateformat.equals("UTC")) timeconv = true;
      else formatter = DateTimeFormatter.ofPattern(cursor.dateformat);
    }

    ArrayList<Object[]> table = new ArrayList<Object[]>();

    for (int i = 0; i < skip && cursor.rset.next(); i++)
      database.fetch(cursor.rset,timeconv,formatter);

    for (int i = 0; (cursor.rows <= 0 || i < cursor.rows) && cursor.rset.next(); i++)
      table.add(database.fetch(cursor.rset,timeconv,formatter));

    if (cursor.rows <= 0 || table.size() < cursor.rows)
      closeCursor(cursor);

    return(table);
  }


  public Cursor getCursor(String name)
  {
    return(cursors.get(name));
  }


  public void closeCursor(String name)
  {
    if (name == null)
      return;

    closeCursor(cursors.get(name));
  }


  public void closeCursor(Cursor cursor)
  {
    if (cursor == null)
      return;

    try {cursor.rset.close();}
    catch (Exception e) {;}

    try {cursor.stmt.close();}
    catch (Exception e) {;}

    if (cursor.name != null)
      cursors.remove(cursor.name);

    cursor.closed = true;
  }


  public boolean fatal()
  {
    if (database == null)
      return(true);

    if (!database.validate())
    {
      sessions.remove(guid);
      database.disconnect();
      database = null;
      return(true);
    }
    return(false);
  }


  public void lock(boolean exclusive) throws Exception
  {
    long thread = Thread.currentThread().getId();

    synchronized(LOCK)
    {
      boolean owner = this.thread == thread;

      while(!owner && this.exclusive)
        LOCK.wait();

      if (exclusive)
      {
        while(!owner && this.shared > 0)
          LOCK.wait();

        this.thread = thread;
        this.exclusive = true;
      }
      else
      {
        while(!owner && this.exclusive)
          LOCK.wait();

        this.shared++;
      }
    }
  }


  public void releaseAll(boolean exclusive, int shared) throws Exception
  {
    if (exclusive) release(true,0);
    if (shared > 0) release(false,shared);
  }


  public void release(boolean exclusive) throws Exception
  {
    int shared = 0;
    if (!exclusive) shared = 1;
    this.release(exclusive,shared);
  }


  public void release(boolean exclusive, int shared) throws Exception
  {
    long thread = Thread.currentThread().getId();

    synchronized(LOCK)
    {
      if (exclusive && this.thread != thread)
        throw new Exception("Thread "+thread+" cannot release session lock owned by "+this.thread);

      if (exclusive && !this.exclusive)
        throw new Exception("Cannot release exclusive lock, when only shared obtained");

      if (!exclusive && this.shared < shared)
        throw new Exception("Cannot release "+shared+" shared lock(s) not obtained");

      if (exclusive)
      {
        this.thread = 0;
        this.exclusive = false;
      }
      else
      {
        this.shared -= shared;
      }

      LOCK.notifyAll();
    }
  }


  private synchronized String create()
  {
    String guid = null;

    while(guid == null)
    {
      guid = new Guid().toString();
      if (sessions.get(guid) != null) guid = null;
    }

    sessions.put(guid,this);
    return(guid);
  }
}