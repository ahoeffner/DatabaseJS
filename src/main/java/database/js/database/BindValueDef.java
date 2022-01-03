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

import java.util.Date;


public class BindValueDef
{
  final int type;
  final String name;
  final Object value;
  final boolean outval;

  public BindValueDef(String name, Object value)
  {
    this.name = name;
    this.value = value;
    this.outval = false;
    this.type = SQLTypes.getType(value);
  }

  public BindValueDef(String name, String type, boolean outval)
  {
    this.name = name;
    this.value = null;
    this.outval = outval;
    this.type = SQLTypes.getType(type);
  }

  public BindValueDef(String name, String type, boolean outval, Object value)
  {
    this.name = name;
    this.outval = outval;
    this.value = convert(type,value);
    this.type = SQLTypes.getType(type);
  }

  public boolean isDate()
  {
    return(SQLTypes.isDate(type));
  }

  public BindValue copy(boolean out)
  {
    return(new BindValue(this,out));
  }

  private Object convert(String type, Object value)
  {
    if (value == null || !(value instanceof Long))
      return(value);

    if (SQLTypes.isDate(type))
        value = new Date((Long) value);

    return(value);
  }
}