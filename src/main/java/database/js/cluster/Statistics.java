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

import java.util.HashSet;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import database.js.config.Config;
import database.js.servers.Server;


public class Statistics
{
  private long pid;
  private short id;
  
  private boolean online;

  private boolean http;
  private boolean procmgr;

  private long updated;
  private long started;
  
  private long totmem;
  private long usedmem;
  private long freemem;
  
  private long requests;
  
  public static final int reclen = 7*Long.BYTES+2;
  
  
  @SuppressWarnings("cast")
  public static void save(Server server)
  {
    try
    {
      Statistics stats = new Statistics();
      ByteBuffer data = ByteBuffer.allocate(reclen);
      
      stats.id = server.id();
      stats.pid = server.pid();
      stats.started = server.started();
      stats.requests = server.requests(); 
      stats.updated = System.currentTimeMillis();      
      
      stats.totmem = Runtime.getRuntime().maxMemory();
      stats.freemem = Runtime.getRuntime().freeMemory();
      stats.usedmem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());

      byte http = server.http() ? (byte) 1 : 0;
      byte procmgr = server.manager() ? (byte) 1 : 0;
      
      data.putLong(stats.pid);
      data.putLong(stats.started);
      data.putLong(stats.updated);
      data.putLong(stats.totmem);
      data.putLong(stats.usedmem);
      data.putLong(stats.freemem);
      data.putLong(stats.requests);
      
      data.put(http);
      data.put(procmgr);
      
      Cluster.write(server.id(),data.array());
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
      HashSet<Short> running = Cluster.getRunningServers();
      
      for (short i = 0; i < servers[0] + servers[1]; i++)
      {
        Statistics stats = new Statistics();
        ByteBuffer data = ByteBuffer.wrap(Cluster.read(i));
        
        stats.id = i;

        if (data.capacity() > 0)
        {
          stats.pid      = data.getLong();
          stats.started  = data.getLong();
          stats.updated  = data.getLong();
          stats.totmem   = data.getLong();
          stats.usedmem  = data.getLong();  
          stats.freemem  = data.getLong();  
          stats.requests = data.getLong();  
          
          byte http = data.get();
          byte procmgr = data.get();
          
          stats.http = http == 1;
          stats.procmgr = procmgr == 1;
          
          stats.online = running.contains(i);
        }
        
        list.add(stats);
      }
    }
    catch (Exception e) {e.printStackTrace();}
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

  public long freemem()
  {
    return(freemem);
  }

  public long requests()
  {
    return(requests);
  }

  public boolean online()
  {
    return(online);
  }

  public boolean http()
  {
    return(http);
  }

  public boolean procmgr()
  {
    return(procmgr);
  }
}
