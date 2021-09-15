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


public class Topology
{
  private final Type type;
  private final int servers;
  private final boolean hot;
  
  
  Topology(JSONObject config) throws Exception
  {
    String type = config.getString("type");
    
    if (type.toLowerCase().equals("micro")) this.type = Type.Micro;
    else                                    this.type = Type.Cluster;
    
    if (this.type == Type.Micro) servers = 1;
    else  servers = config.getInt("servers");
    
    this.hot = config.getBoolean("hot-standby");
  }


  public Type type()
  {
    return(type);
  }

  public int servers()
  {
    return(servers);
  }

  public boolean hotstandby()
  {
    return(hot);
  }


  public static enum Type
  {
    Micro,
    Cluster
  }
}
