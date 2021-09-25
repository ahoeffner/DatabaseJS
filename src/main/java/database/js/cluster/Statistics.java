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

package database.js.cluster;

import ipc.Guest;
import ipc.Broker;
import ipc.Resource;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import database.js.config.Config;
import database.js.servers.Server;


public class Statistics
{
  private short id;
  private long pid;
  private long upd;
  private long mem;
  private long used;
  private static final int statlen = 4*Long.BYTES;
  
  
  private Statistics()
  {    
  }
  
  
  private Statistics init()
  {
    this.upd = System.currentTimeMillis();
    this.pid = 0; //ProcessHandle.current().pid();
    this.mem = Runtime.getRuntime().totalMemory();
    this.used = (mem - Runtime.getRuntime().freeMemory());
    return(this);
  }
  
  
  public static void save(Server server)
  {
    try
    {
      Broker broker = server.broker();
      String name = ""+(broker.id()+1);
      Resource stat = broker.getResource(name);
      Statistics stats = new Statistics().init();
      ByteBuffer data = ByteBuffer.allocate(statlen);
      
      data.putLong(stats.pid);
      data.putLong(stats.upd);
      data.putLong(stats.mem);
      data.putLong(stats.used);
      
      stat.acquire();
      stat.put(data.array());
    }
    catch (Exception e)
    {
      server.logger().log(Level.SEVERE,e.getMessage(),e);
    }
  }
  
  
  public static ArrayList<Statistics> get(Config config)
  {
    ArrayList<Statistics> list =
      new ArrayList<Statistics>();
    
    try
    {
      Short[] servers = Cluster.getServers(config);
      
      for (short i = 0; i < servers[0] + servers[1]; i++)
      {
        String name = ""+(i+1);
        Statistics stats = new Statistics();
        Guest guest = new Guest(config.getIPConfig());
        ByteBuffer data = ByteBuffer.wrap(guest.getResource(name));
        
        stats.id = i;

        if (data.capacity() > 0)
        {
          stats.pid = data.getLong();
          stats.upd = data.getLong();
          stats.mem = data.getLong();
          stats.used = data.getLong();        
        }
        
        list.add(stats);
      }
    }
    catch (Exception e) {;}
    return(list);
  }

  public short id()
  {
    return(id);
  }

  public long pid()
  {
    return(pid);
  }

  public long updated()
  {
    return(upd);
  }

  public long memory()
  {
    return(mem);
  }

  public long used()
  {
    return(used);
  }
}
