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

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.servers.Server;
import database.js.control.Process;

import java.util.HashSet;
import java.util.stream.Collectors;


public class Cluster
{
  public static Process.Type getType(Config config, short id) throws Exception
  {  
    Short[] servers = getServers(config);
    return(id < servers[0] ? Process.Type.http : Process.Type.rest);
  }
  
  
  public static ArrayList<ServerProcess> running(Config config) throws Exception
  {    
    Logger logger = config.getLogger().logger;
    String cname = "database.js.servers.Server";
    String match = ".*java?(\\.exe)?\\s+.*"+cname+".*";
    ArrayList<ServerProcess> running = new ArrayList<ServerProcess>();
    
    Stream<ProcessHandle> stream = ProcessHandle.allProcesses();
    List<ProcessHandle> processes = stream.filter((p) -> p.info().commandLine().isPresent())
                                          .filter((p) -> p.info().commandLine().get().matches(match))
                                          .collect(Collectors.toList());
        
    for(ProcessHandle handle : processes)
    {
      long pid = handle.pid();
      String cmd = handle.info().commandLine().get();

      try
      {
        int end = cmd.indexOf(cname) + cname.length();
        String[] args = cmd.substring(end).trim().split(" ");
        running.add(new ServerProcess(Short.parseShort(args[0]),pid));
      }
      catch(Exception e) 
      {
        logger.warning("Uanable to parse process-handle "+cmd);
      }
    }
    
    return(running);
  }
  
  
  public static boolean isRunning(Config config, short id, long pid) throws Exception
  {
    ArrayList<ServerProcess> running = running(config);
    
    for(ServerProcess p : running)
    {
      if (p.id == id && p.pid != pid)
        return(true);
    }
    
    return(false);
  }


  public static ArrayList<ServerType> notRunning(Server server) throws Exception
  {
    Short[] servers = getServers(server.config());
    ArrayList<ServerType> down = new ArrayList<ServerType>();

    HashSet<Short> running = getRunningServers(server.config());
    
    for (short i = 0; i < servers[0]; i++)
    {
      if (i == server.id()) continue;
      
      if (!running.contains(i))
        down.add(new ServerType(Process.Type.http,i));
    }
    
    for (short i = servers[0]; i < servers[0] + servers[1]; i++)
    {
      if (i == server.id()) continue;
      
      if (!running.contains(i))
        down.add(new ServerType(Process.Type.rest,i));
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
  
  
  private static HashSet<Short> getRunningServers(Config config) throws Exception
  {
    HashSet<Short> sids =new HashSet<Short>();
    ArrayList<ServerProcess> running = running(config);
    for(ServerProcess p : running) sids.add(p.id);
    return(sids);
  }
  
  
  public static class ServerType
  {
    public final short id;
    public final Process.Type type;
    
    ServerType(Process.Type type, short id)
    {
      this.id = id;
      this.type = type;
    }
  }
  
  
  private static class ServerProcess
  {
    final short id;
    final long pid;
    
    ServerProcess(short id, long pid)
    {
      this.id = id;
      this.pid = pid;
    }
  }
}
