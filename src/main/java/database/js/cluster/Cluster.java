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

import java.io.File;
import java.util.List;
import java.util.HashSet;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;
import database.js.config.Paths;
import java.util.logging.Logger;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.MappedByteBuffer;
import database.js.config.Config;
import database.js.servers.Server;
import database.js.control.Process;
import java.util.stream.Collectors;
import java.nio.channels.FileChannel;
import static java.nio.file.StandardOpenOption.*;


public class Cluster
{
  private final Logger logger;
  private final Config config;
  private final MappedByteBuffer shmmem;
  
  private static Cluster cluster = null;
  
  
  Cluster(Config config) throws Exception
  {
    this.config = config;
    String filename = getFileName();
    this.logger = config.getLogger().logger;
    FileSystem fs = FileSystems.getDefault();
    
    Short[] servers = getServers(config);
    int processes = servers[0] + servers[1];
    int size = processes * Statistics.reclen + Long.BYTES;
    
    Path path = fs.getPath(filename);
    FileChannel fc = FileChannel.open(path,CREATE,READ,WRITE);
    this.shmmem = fc.map(FileChannel.MapMode.READ_WRITE,0,size);
  }
  
  
  private void setStopped()
  {
    byte cs = (byte) (shmmem.get(0) + 1);
    long stop = System.currentTimeMillis();

    shmmem.put(0,cs);
    shmmem.putLong(1,stop);
    shmmem.put(Long.BYTES+1,cs);
  }
  
  
  private long getStopped()
  {
    byte cs1 = 0;
    byte cs2 = 1;
    long time = 0;

    for (int i = 0; cs1 != cs2 && i < 32768; i++)
    {
      cs2  = shmmem.get(Long.BYTES+1);
      time = shmmem.getLong(1);
      cs1  = shmmem.get(0);

      if (cs1 != cs2) Thread.yield();
    }
    
    return(time);
  }
  
  
  private byte[] readdata(short id)
  {
    byte[] data = new byte[Statistics.reclen];
    int offset = 2 + Long.BYTES + id * Statistics.reclen;
    this.shmmem.get(offset,data);
    return(data);
  }
  
  
  private void writedata(short id, byte[] data)
  {
    int offset = 2 + Long.BYTES + id * Statistics.reclen;
    this.shmmem.put(offset,data);
  }


  private String getFileName()
  {
    return(Paths.ipcdir + File.separator + "cluster.dat");
  }


  private String getLockFileName()
  {
    return(Paths.ipcdir + File.separator + "cluster.dat");
  }
  
  
  public static void stop()
  {
    cluster.setStopped();    
  }
  
  
  public static long getStopTime()
  {
    return(cluster.getStopped());
  }
  
  
  public static void init(Config config) throws Exception
  {
    if (cluster != null) return;
    cluster = new Cluster(config);
  }
  

  public static Process.Type getType(short id) throws Exception
  {  
    Config config = cluster.config;
    Short[] servers = getServers(config);
    return(id < servers[0] ? Process.Type.http : Process.Type.rest);
  }
  
  
  public static ArrayList<ServerProcess> running() throws Exception
  {    
    Config config = cluster.config;
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
        logger.warning("Unable to parse process-handle "+cmd);
      }
    }
    
    return(running);
  }
  
  
  public static boolean isRunning(short id, long pid) throws Exception
  {
    ArrayList<ServerProcess> running = running();
    
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

    HashSet<Short> running = getRunningServers();
    
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


  public static ArrayList<Statistics> getStatistics()
  {
    Config config = cluster.config;
    return(Statistics.get(config));
  }


  public static ArrayList<Statistics> getStatistics(Config config) throws Exception
  {
    init(config);
    return(Statistics.get(config));
  }
  
  
  static byte[] read(short id)
  {
    return(cluster.readdata(id));
  }
  
  
  static void write(short id, byte[] data)
  {
    cluster.writedata(id,data);
  }
  
  
  public static Short[] getServers(Config config) throws Exception
  {
    short http = 1;
    if (config.getTopology().hotstandby()) http++;
    return(new Short[] {http,config.getTopology().servers()});
  }
  
  
  public static HashSet<Short> getRunningServers() throws Exception
  {
    HashSet<Short> sids =new HashSet<Short>();
    ArrayList<ServerProcess> running = running();
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
