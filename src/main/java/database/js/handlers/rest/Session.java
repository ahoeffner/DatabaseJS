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
import database.js.database.DatabaseUtils;
import database.js.database.NameValuePair;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class Session
{
  private final Pool pool;
  private final String guid;
  private final Scope scope;
  private final String secret;
  private final String username;
  private final SessionLock lock;
  private final AuthMethod method;

  private int clients = 0;
  private Database database = null;
  private long touched = System.currentTimeMillis();

  private final ConcurrentHashMap<String,Cursor> cursors =
    new ConcurrentHashMap<String,Cursor>();

  private final static Logger logger = Logger.getLogger("rest");


  public Session(AuthMethod method, Pool pool, String scope, String username, String secret) throws Exception
  {
    this.pool = pool;
    this.method = method;
    this.secret = secret;
    this.username = username;
    this.scope = getScope(scope);
    this.lock = new SessionLock();
    this.guid = SessionManager.register(this);
  }


  public synchronized int share()
  {
    return(++clients);
  }


  public synchronized int clients()
  {
    return(clients);
  }


  public synchronized boolean release(boolean failed)
  {
    clients--;
    boolean reuse = true;

    if (failed && !database.validate())
      reuse = false;

    disconnect(reuse);
    System.out.println("released: "+this);
    return(!reuse);
  }


  public synchronized void touch()
  {
    touched = System.currentTimeMillis();
  }


  public synchronized long touched()
  {
    return(touched);
  }


  public String guid()
  {
    return(guid);
  }


  public boolean dedicated()
  {
    return(scope != Scope.None);
  }


  public synchronized void disconnect()
  {
    if (disconnect(true))
      SessionManager.remove(guid);
  }


  public synchronized void ensure() throws Exception
  {
    touch();

    if (database == null)
      connect(true);
  }


  public void connect(boolean keep) throws Exception
  {
    switch(method)
    {
      case OAuth :
        database = pool.connect();

        database.setAutoCommit(false);
        if (pool.proxy()) database.setProxyUser(username);

        break;

      case Database :
        database = DatabaseUtils.getInstance();

        database.connect(username,secret);
        database.setAutoCommit(false);

        break;

      case PoolToken :
        if (scope == Scope.Dedicated) database = pool.connect(secret);
        else                          database = pool.getConnection(secret);

        database.setAutoCommit(false);
        if (pool.proxy()) database.setProxyUser(username);

        break;
    }

    if (!keep && !dedicated())
      disconnect(true);
  }


  private synchronized boolean disconnect(boolean reuse)
  {
    System.out.println("disconnect database="+database+" clients="+clients);
    if (database == null)
    {
      logger.severe("Releasing allready released connection");
      return(false);
    }

    if (clients > 1)
    {
      logger.severe("Releasing connection while other clients connected, clients: "+clients);
      return(false);
    }

    if (reuse)
    {
      if (pool == null) database.disconnect();
      else              pool.release(database);
    }

    database = null;
    return(true);
  }


  public void commit() throws Exception
  {
    database.commit();
    if (scope == Scope.Transaction) disconnect(true);
  }


  public void rollback() throws Exception
  {
    database.rollback();
    if (scope == Scope.Transaction) disconnect(true);
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


  public boolean execute(String sql) throws Exception
  {
    return(database.execute(sql));
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


  public void closeAllCursors()
  {
    for(Map.Entry<String,Cursor> entry : cursors.entrySet())
      closeCursor(entry.getValue());
  }


  public SessionLock lock()
  {
    return(lock);
  }


  private Scope getScope(String scope)
  {
    if (scope == null)
      return(Scope.None);

    scope = Character.toUpperCase(scope.charAt(0))
           + scope.substring(1).toLowerCase();

    return(Scope.valueOf(scope));
  }


  @Override
  public String toString()
  {
    String str = "";

    str += "Scope: " + scope + " connected: " + (database != null);

    if (pool == null) str += " " + username;
    else str += " pooled["+(pool.proxy() ? username : "----")+"]";

    str += " "+lock;

    return(str);
  }


  private static enum Scope
  {
    None,
    Transaction,
    Dedicated
  }
}