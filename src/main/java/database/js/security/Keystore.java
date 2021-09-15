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

package database.js.security;

public class Keystore
{
  public final String file;
  public final String type;
  public final String alias;
  public final String password;


  public Keystore(String file, String type, String alias, String password)
  {
    this.file = file;
    this.type = type;
    this.alias = alias;
    this.password = password;
  }


  @Override
  public String toString()
  {
    return(file+" "+type+" "+(alias == null ? "" : alias));
  }
}
