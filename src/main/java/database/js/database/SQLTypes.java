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

import java.sql.Types;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;


public class SQLTypes
{
  private final static Logger logger =
    Logger.getLogger("rest");

  public static final ConcurrentHashMap<String,Integer> types =
    new ConcurrentHashMap<String,Integer>();

  static
  {
    types.put("INT",Types.TINYINT);
    types.put("INTEGER",Types.INTEGER);
    types.put("SMALLINT",Types.SMALLINT);

    types.put("FLOAT",Types.FLOAT);
    types.put("DOUBLE",Types.DOUBLE);
    types.put("NUMBER",Types.DECIMAL);
    types.put("NUMERIC",Types.DECIMAL);
    types.put("DECIMAL",Types.DECIMAL);

    types.put("DATE",Types.DATE);
    types.put("DATETIME",Types.TIMESTAMP);
    types.put("TIMESTAMP",Types.TIMESTAMP);

    types.put("STRING",Types.VARCHAR);
    types.put("VARCHAR",Types.VARCHAR);
    types.put("VARCHAR2",Types.VARCHAR);
    types.put("TEXT",Types.LONGNVARCHAR);

    types.put("BOOLEAN",Types.BOOLEAN);
  }


  public static Integer getType(String type)
  {
    Integer sqlt = type == null ? null : types.get(type.toUpperCase());

    if (sqlt == null)
    {
      logger.warning("Unknow sqltype "+type);
      sqlt = Types.VARCHAR;
    }

    return(sqlt);
  }

  public static boolean isDate(int type)
  {
    if (type == Types.DATE || type == Types.TIMESTAMP)
      return(true);

    return(false);
  }
}