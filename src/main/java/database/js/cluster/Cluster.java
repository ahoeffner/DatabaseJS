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
import java.util.ArrayList;
import database.js.config.Config;
import database.js.servers.Server;


public class Cluster
{
  public static Config.Type getType(Config config, short id) throws Exception
  {  
    Short[] servers = getServers(config);
    return(id < servers[0] ? Config.Type.http : Config.Type.rest);
  }
  
  
  public static boolean isRunning(Config config, short id) throws Exception
  {
    boolean running = true;
    
    Guest guest = new Guest(config.getIPConfig());    
    int heartbeat = config.getIPConfig().heartbeat;
    
    for (int i = 0; i < 8 && running; i++)
    {
      running = guest.online(id);
      if (running) Thread.sleep(heartbeat/4);      
    }

    return(running);
  }


  public static ArrayList<ServerType> notRunning(Config config) throws Exception
  {
    Short[] servers = getServers(config);
    ArrayList<ServerType> down = new ArrayList<ServerType>();    
    
    for (short i = 0; i < servers[0]; i++)
    {
      if (!isRunning(config,i))
        down.add(new ServerType(Config.Type.http,i));
    }
    
    for (short i = 0; i < servers[1]; i++)
    {
      int id = i + servers[0];
      if (!isRunning(config,(short) id))
        down.add(new ServerType(Config.Type.rest,i));
    }

    return(down);
  }
  
  
  public static void setStatistics(Server server)
  {
    Statistics.save(server);
  }


  public static ArrayList<Statistics> getStatistics(Config config)
  {
    return(Statistics.get(config));
  }
  
  
  public static Short[] getServers(Config config) throws Exception
  {
    short http = 1;
    if (config.getTopology().hotstandby()) http++;
    return(new Short[] {http,config.getTopology().servers()});
  }
  
  
  public static class ServerType
  {
    public final short id;
    public final Config.Type type;
    
    ServerType(Config.Type type, short id)
    {
      this.id = id;
      this.type = type;
    }
  }
}
