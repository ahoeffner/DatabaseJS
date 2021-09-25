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

import ipc.Broker;
import ipc.Resource;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import database.js.servers.Server;


public class Statistics
{
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
  
  
  public static Statistics get(Server server, short id)
  {
    try
    {
      String name = ""+(id+1);
      Broker broker = server.broker();
      Statistics stats = new Statistics();
      Resource stat = broker.getResource(name);      
      ByteBuffer data = ByteBuffer.wrap(stat.get());

      if (data.capacity() > 0)
      {
        stats.pid = data.getLong();
        stats.upd = data.getLong();
        stats.mem = data.getLong();
        stats.used = data.getLong();        
      }
      
      return(stats);
    }
    catch (Exception e)
    {
      server.logger().log(Level.SEVERE,e.getMessage(),e);
      return(new Statistics());
    }
  }
}