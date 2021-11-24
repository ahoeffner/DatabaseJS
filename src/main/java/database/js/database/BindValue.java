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


public class BindValue
{
  final int type;
  final String name;
  final Object value;

  public BindValue(String name, String type)
  {
    this(name,type,null);
  }

  public BindValue(String name, String type, Object value)
  {
    this.name = name;
    this.value = value;
    this.type = SQLTypes.getType(type);
  }


  public Copy copy(boolean out)
  {
    return(new Copy(this,out));
  }


  public static class Copy
  {
    final boolean out;
    final BindValue bindvalue;

    private Copy(BindValue bindvalue, boolean out)
    {
      this.out = out;
      this.bindvalue = bindvalue;
    }
  }
}