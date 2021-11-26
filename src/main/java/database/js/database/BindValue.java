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
  private final boolean out;
  private final BindValueDef bindvalue;

  public BindValue(BindValueDef bindvalue, boolean out)
  {
    this.out = out;
    this.bindvalue = bindvalue;
  }

  public boolean InOut()
  {
    return(out);
  }

  public boolean OutOnly()
  {
    return(bindvalue.outval);
  }

  public String getName()
  {
    return(bindvalue.name);
  }

  public int getType()
  {
    return(bindvalue.type);
  }

  public Object getValue()
  {
    return(bindvalue.value);
  }
  
  
  @Override
  public String toString()
  {
    String inout = "in";
    if (bindvalue.outval) inout = "out";
    else if (out)         inout = "inout";
    
    return(bindvalue.name+" "+bindvalue.type+" "+bindvalue.value+" ("+inout+")");
  }
}