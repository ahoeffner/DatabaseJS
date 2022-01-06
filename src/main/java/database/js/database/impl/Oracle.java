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

package database.js.database.impl;

import java.sql.Savepoint;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Properties;
import java.sql.PreparedStatement;
import database.js.database.Database;
import database.js.database.BindValue;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.driver.OracleConnection;


public class Oracle extends Database
{
  @Override
  public void setProxyUser(String username) throws Exception
  {
    Properties props = new Properties();
    props.put(OracleConnection.PROXY_USER_NAME, username);

    OracleConnection conn = (OracleConnection) super.connection();
    conn.openProxySession(OracleConnection.PROXYTYPE_USER_NAME,props);
  }


  @Override
  public void releaseProxyUser() throws Exception
  {
    OracleConnection conn = (OracleConnection) super.connection();
    conn.close(OracleConnection.PROXY_SESSION);
  }


  @Override
  public void releaseSavePoint(Savepoint savepoint, boolean rollback) throws Exception
  {
    // Oracle only supports rollback. Savepoints are released when commit/rollback
    if (rollback) super.releaseSavePoint(savepoint,rollback);
  }


  @Override
  public ReturnValueHandle prepareWithReturnValues(String sql, ArrayList<BindValue> bindvalues) throws Exception
  {
    ArrayList<String> columns = new ArrayList<String>();
    OracleConnection conn = (OracleConnection) super.connection();
    OraclePreparedStatement stmt = (OraclePreparedStatement) conn.prepareStatement(sql);

    for (int i = 0; i < bindvalues.size(); i++)
    {
      BindValue b = bindvalues.get(i);

      if (b.InOut())
      {
        columns.add(b.getName());
        stmt.registerReturnParameter(i+1,b.getType());
        if (!b.OutOnly()) stmt.setObject(i+1,b.getValue());
      }
      else
      {
        stmt.setObject(i+1,b.getValue(),b.getType());
      }
    }

    ReturnValueHandle handle = new ReturnValueHandle(stmt,columns.toArray(new String[0]));
    return(handle);
  }


  @Override
  public ResultSet executeUpdateWithReturnValues(PreparedStatement jstmt) throws Exception
  {
    OraclePreparedStatement stmt = (OraclePreparedStatement) jstmt;
    stmt.executeUpdate();
    ResultSet rset = stmt.getReturnResultSet();
    return(rset);
  }
}