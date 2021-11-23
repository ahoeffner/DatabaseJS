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

package database.js.config;


public enum DatabaseType
{
  Oracle("database.js.database.impl.Oracle"),
  Postgres("database.js.database.impl.Postgres"),
  Generic("database.js.database.impl.Generic");


  public final Class clazz;

  private DatabaseType(String clazz)
  {
    Class cl = null;
    try {cl = Class.forName(clazz);}
    catch (Throwable e) {e.printStackTrace();}
    this.clazz = cl;
  }
}