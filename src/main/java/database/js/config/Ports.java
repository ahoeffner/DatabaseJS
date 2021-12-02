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

import org.json.JSONObject;


public class Ports
{
  public final int ssl;
  public final int admin;
  public final int plain;
  public final boolean sslrequired;

  public Ports(JSONObject config)
  {
    this.ssl = Config.get(config,"ssl");
    this.plain = Config.get(config,"plain");
    this.admin = Config.get(config,"admin");
    this.sslrequired = plain == 0;
  }
}