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
  private final boolean hot;
  private final short threads;
  private final short servers;
  
  private final int extnds;
  private final String extsize;

  private static final int cores = Runtime.getRuntime().availableProcessors();
  
  
  Topology(JSONObject config) throws Exception
  {
    String type = config.getString("type");
    
    if (type.toLowerCase().equals("micro")) this.type = Type.Micro;
    else                                    this.type = Type.Cluster;
    
    if (this.type == Type.Micro) servers = 0;
    else  servers = (short) config.getInt("servers");
    
    short threads = 0;
    short multi = this.type == Type.Cluster ? servers : 1;
    
    if (!config.isNull("threads"))
      threads = (short) config.getInt("threads");
    
    if (threads > 0) this.threads = threads;
    else             this.threads = (short) (multi * 8 * cores);
    
    this.hot = config.getBoolean("hot-standby");
    
    JSONObject ipc = config.getJSONObject("ipc");
    
    this.extnds = ipc.getInt("extends");
    this.extsize = ipc.get("extsize").toString();
  }


  public Type type()
  {
    return(type);
  }

  public int threads()
  {
    return(threads);
  }

  public short servers()
  {
    return(servers);
  }

  public boolean hotstandby()
  {
    return(hot);
  }

  public int extnds()
  {
    return(extnds);
  }

  public String extsize()
  {
    return(extsize);
  }


  public static enum Type
  {
    Micro,
    Cluster
  }
}
