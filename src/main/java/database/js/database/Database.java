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

import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Connection;
import java.util.ArrayList;
import java.sql.DriverManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.time.format.DateTimeFormatter;
import database.js.handlers.rest.DateUtils;
import java.util.concurrent.atomic.AtomicInteger;


public abstract class Database
{
  private final int id;
  private Connection conn;
  private long touched = 0;

  private static String url;
  private static String teststmt;
  private static AtomicInteger next = new AtomicInteger(0);
  private final static Logger logger = Logger.getLogger("rest");


  public static void setUrl(String url)
  {
    Database.url = url;
  }


  public static String getTestSQL()
  {
    return(Database.teststmt);
  }


  public static void setTestSQL(String test)
  {
    Database.teststmt = test;
  }


  public Database()
  {
    id = next.getAndIncrement();
    touched = System.currentTimeMillis();
  }


  public int id()
  {
    return(id);
  }


  public final void touch()
  {
    touched = System.currentTimeMillis();
  }


  public final long touched()
  {
    return(touched);
  }


  public Connection connection()
  {
    return(conn);
  }


  public boolean connected()
  {
    return(conn != null);
  }


  public void disconnect()
  {
    try {conn.close();}
    catch (Exception e) {;}
    finally {this.conn = null;}
  }


  public void setAutoCommit(boolean on) throws Exception
  {
    conn.setAutoCommit(on);
  }


  public void commit() throws Exception
  {
    conn.commit();
  }


  public void rollback() throws Exception
  {
    conn.rollback();
  }


  public void connect(String username, String password) throws Exception
  {
    String url = DatabaseUtils.bind(username,password);
    this.conn = DriverManager.getConnection(url);
  }


  public Savepoint setSavePoint() throws Exception
  {
    return(conn.setSavepoint());
  }


  public void releaseSavePoint(Savepoint savepoint, boolean rollback) throws Exception
  {
    if (rollback) conn.rollback(savepoint);
    else  conn.releaseSavepoint(savepoint);
  }


  public boolean validate()
  {
    try
    {
      String sql = getTestSQL();

      PreparedStatement stmt =
        conn.prepareStatement(sql);

      ResultSet rset =
        stmt.executeQuery();

      rset.next();
      rset.close();
      stmt.close();

      return(true);
    }
    catch (Exception e)
    {
      logger.log(Level.WARNING,e.getMessage(),e);
      return(false);
    }
  }


  public PreparedStatement prepare(String sql, ArrayList<BindValue> bindvalues) throws Exception
  {
    PreparedStatement stmt = conn.prepareStatement(sql);

    for (int i = 0; i < bindvalues.size(); i++)
    {
      BindValue b = bindvalues.get(i);
      stmt.setObject(i+1,b.getValue(),b.getType());
    }

    return(stmt);
  }


  public CallableStatement prepareCall(String sql, ArrayList<BindValue> bindvalues) throws Exception
  {
    CallableStatement stmt = conn.prepareCall(sql);

    for (int i = 0; i < bindvalues.size(); i++)
    {
      BindValue b = bindvalues.get(i);

      if (b.InOut())
      {
        stmt.registerOutParameter(i+1,b.getType());
        if (!b.OutOnly()) stmt.setObject(i+1,b.getValue());
      }
      else
      {
        stmt.setObject(i+1,b.getValue(),b.getType());
      }
    }

    return(stmt);
  }


  public ResultSet executeQuery(PreparedStatement stmt) throws Exception
  {
    return(stmt.executeQuery());
  }


  public int executeUpdate(PreparedStatement stmt) throws Exception
  {
    return(stmt.executeUpdate());
  }


  public boolean execute(String sql) throws Exception
  {
    Statement stmt = conn.createStatement();
    return(stmt.execute(sql));
  }


  public ArrayList<NameValuePair<Object>> execute(CallableStatement stmt, ArrayList<BindValue> bindvalues, boolean timeconv, DateTimeFormatter formatter) throws Exception
  {
    boolean conv = timeconv || formatter != null;

    ArrayList<NameValuePair<Object>> values =
      new ArrayList<NameValuePair<Object>>();

    stmt.executeUpdate();

    for (int i = 0; i < bindvalues.size(); i++)
    {
      BindValue b = bindvalues.get(i);

      if (b.InOut())
      {
        Object value = stmt.getObject(i+1);

        if (conv && DateUtils.isDate(value))
        {
          if (timeconv) value = DateUtils.getTime(value);
          else value = DateUtils.format(formatter,value);
        }

        values.add(new NameValuePair<Object>(b.getName(),value));
      }
    }

    return(values);
  }


  public String[] getColumNames(ResultSet rset) throws Exception
  {
    ResultSetMetaData meta = rset.getMetaData();
    String[] columns = new String[meta.getColumnCount()];

    for (int i = 0; i < columns.length; i++)
      columns[i] = meta.getColumnName(i+1);

    return(columns);
  }


  public Object[] fetch(ResultSet rset, boolean timeconv, DateTimeFormatter formatter) throws Exception
  {
    boolean conv = timeconv || formatter != null;
    ResultSetMetaData meta = rset.getMetaData();
    Object[] values = new Object[meta.getColumnCount()];

    for (int i = 0; i < values.length; i++)
    {
      values[i] = rset.getObject(i+1);

      if (conv && DateUtils.isDate(values[i]))
      {
        if (timeconv) values[i] = DateUtils.getTime(values[i]);
        else values[i] = DateUtils.format(formatter,values[i]);
      }
    }

    return(values);
  }


  public abstract void releaseProxyUser() throws Exception;
  public abstract void setProxyUser(String username) throws Exception;
}