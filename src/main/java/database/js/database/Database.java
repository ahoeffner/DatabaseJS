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
import java.sql.Connection;
import java.util.ArrayList;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.Savepoint;


public abstract class Database
{
  private Connection conn;


  public Database()
  {
  }


  public boolean connected()
  {
    return(conn != null);
  }


  public boolean disconnect()
  {
    boolean done = true;

    try {conn.close();}
    catch (Exception e)
    {done = false;}

    this.conn = null;
    return(done);
  }


  public void setConnection(Connection conn)
  {
    this.conn = conn;
  }


  public Connection connect(String username, String password) throws Exception
  {
    String url = DatabaseUtils.bind(username,password);
    Connection conn = DriverManager.getConnection(url);
    return(conn);
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


  public ResultSet executeQuery(PreparedStatement stmt) throws Exception
  {
    return(stmt.executeQuery());
  }


  public String[] getColumNames(ResultSet rset) throws Exception
  {
    ResultSetMetaData meta = rset.getMetaData();
    String[] columns = new String[meta.getColumnCount()];

    for (int i = 0; i < columns.length; i++)
      columns[i] = meta.getColumnName(i+1);

    return(columns);
  }


  public Object[] fetch(ResultSet rset) throws Exception
  {
    ResultSetMetaData meta = rset.getMetaData();
    Object[] values = new Object[meta.getColumnCount()];

    for (int i = 0; i < values.length; i++)
      values[i] = rset.getObject(i+1);

    return(values);
  }
}