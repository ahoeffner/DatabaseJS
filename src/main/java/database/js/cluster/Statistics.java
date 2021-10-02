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
  
  private boolean manager;
  private boolean secretary;
  
  private long updated;
  private long started;
  
  private long totmem;
  private long usedmem;

  private static Statistics stats = null;  
  public static final int reclen = 5*Long.BYTES+2;
  
  
  private Statistics()
  {    
  }
  
  
  private Statistics init(Broker broker)
  {
    this.pid = 0; //ProcessHandle.current().pid();
    this.started = broker.started();
    this.updated = System.currentTimeMillis();
    this.totmem = Runtime.getRuntime().totalMemory();
    this.usedmem = (totmem - Runtime.getRuntime().freeMemory());
    return(this);
  }
  
  
  public static void save(Server server)
  {
    try
    {
      Broker broker = server.broker();

      String name = ""+broker.id();
      Resource stat = broker.getResource(name);
      ByteBuffer data = ByteBuffer.allocate(reclen);
      
      if (stats == null)
      {
        stats = new Statistics().init(server.broker());
        stat.acquire();        
      }
      
      stats.updated = System.currentTimeMillis();      
      byte manager = broker.manager() ? (byte) 1 : 0;
      byte secretary = broker.secretary() ? (byte) 1 : 0;
      
      data.putLong(stats.pid);
      data.putLong(stats.started);
      data.putLong(stats.updated);
      data.putLong(stats.totmem);
      data.putLong(stats.usedmem);
      
      data.put(manager);
      data.put(secretary);
      
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
      Guest guest = Cluster.guest(config);      
      Short[] servers = Cluster.getServers(config);
      
      for (short i = 0; i < servers[0] + servers[1]; i++)
      {
        String name = ""+i;
        Statistics stats = new Statistics();
        ByteBuffer data = ByteBuffer.wrap(guest.getResource(name));
        
        stats.id = i;

        if (data.capacity() > 0)
        {
          stats.pid     = data.getLong();
          stats.started = data.getLong();
          stats.updated = data.getLong();
          stats.totmem  = data.getLong();
          stats.usedmem = data.getLong();  
          
          byte manager = data.get();
          byte secretary = data.get();
          
          stats.manager = manager == 1;
          stats.secretary = secretary == 1;
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

  public long started()
  {
    return(started);
  }

  public long updated()
  {
    return(updated);
  }

  public long totmem()
  {
    return(totmem);
  }

  public long usedmem()
  {
    return(usedmem);
  }

  public boolean manager()
  {
    return(manager);
  }

  public boolean secretary()
  {
    return(secretary);
  }
}
