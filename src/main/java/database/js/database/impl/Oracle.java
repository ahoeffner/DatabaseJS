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
import java.sql.Connection;
import java.util.Properties;
import database.js.database.Database;
import oracle.jdbc.driver.OracleConnection;


public class Oracle extends Database
{
  @Override
  public Connection setProxyUser(String username) throws Exception
  {
    Properties props = new Properties();
    props.put(OracleConnection.PROXY_USER_NAME, username);

    OracleConnection conn = (OracleConnection) super.connection();
    conn.openProxySession(OracleConnection.PROXYTYPE_USER_NAME,props);

    return(conn);
  }


  @Override
  public Connection releaseProxyUser(Connection conn) throws Exception
  {
    throw new Exception("Feature not supported");
  }


  @Override
  public void releaseSavePoint(Savepoint savepoint, boolean rollback) throws Exception
  {
    // Oracle only supports rollback. Savepoints are released when commit/rollback
    if (rollback) super.releaseSavePoint(savepoint,rollback);
  }
}