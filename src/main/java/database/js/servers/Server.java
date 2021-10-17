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

package database.js.servers;

import java.io.PrintStream;
import java.util.ArrayList;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.FileOutputStream;
import database.js.config.Config;
import database.js.control.Process;
import database.js.cluster.Cluster;
import database.js.pools.ThreadPool;
import java.io.BufferedOutputStream;
import database.js.servers.rest.RESTServer;
import database.js.servers.rest.RESTClient;
import database.js.servers.http.HTTPServer;
import database.js.cluster.Cluster.ServerType;
import database.js.servers.http.HTTPServerType;


/**
 *
 *
 */
public class Server extends Thread
{
  private final short id;
  private final long pid;
  private final short htsrvs;
  private final long started;
  private final short servers;
  private final int heartbeat;
  private final Logger logger;
  private final Config config;
  private final boolean embedded;

  private long requests = 0;
  private volatile boolean stop = false;
  private volatile boolean shutdown = false;
  
  private final HTTPServer ssl;
  private final HTTPServer plain;
  private final HTTPServer admin;

  private final RESTServer rest;
  private final RESTClient[] workers;

  
  public static void main(String[] args)
  {
    try {new Server(Short.parseShort(args[0]));}
    catch (Exception e) {e.printStackTrace();}
  }
  
  
  Server(short id) throws Exception
  {
    this.id = id;
    this.config = new Config();
    
    PrintStream out = stdout();
    this.setName("Server Main");

    System.setOut(out);
    System.setErr(out);

    config.getLogger().open(id);
    this.logger = config.getLogger().logger;    

    this.pid = ProcessHandle.current().pid();
    this.started = System.currentTimeMillis();
    
    if (Cluster.isRunning(config,id,pid))
    {
      logger.warning("Server "+id+" is already running. Bailing out");
      System.exit(-1);
    }
    
    short htsrvs = 1;
    if (config.getTopology().hotstandby()) htsrvs++;
    
    this.htsrvs = htsrvs;
    this.servers = config.getTopology().servers();
    Process.Type type = Cluster.getType(config,id);

    this.embedded = servers <= 0;
    this.workers = new RESTClient[servers];
    this.heartbeat = config.getTopology().heartbeat();
    
    if (type == Process.Type.rest)
    {
      this.ssl = null;
      this.plain = null;
      this.admin = null;
      this.rest = new RESTServer(this);
    }
    else
    {
      this.rest = null;
      this.ssl = new HTTPServer(this,HTTPServerType.ssl,embedded);
      this.plain = new HTTPServer(this,HTTPServerType.plain,embedded);
      this.admin = new HTTPServer(this,HTTPServerType.admin,embedded);

      this.startup();
    }

    this.start();
    this.ensure();
    
    logger.info("Instance startet"+System.lineSeparator());
  }
  
  
  private void startup()
  {
    if (!open())
    {
      logger.info("Address already in use");
      return;
    }
    
    logger.info("Open http sockets");

    ssl.start();
    plain.start();
    admin.start();
  }
  
  
  public short id()
  {
    return(id);
  }
  
  
  public long started()
  {
    return(started);
  }
  
  
  public boolean embedded()
  {
    return(embedded);
  }
  
  
  public Config config()
  {
    return(config);
  }
  
  
  public Logger logger()
  {
    return(logger);
  }
  
  
  public synchronized void request()
  {
    requests++;
  }
  
  
  public synchronized long requests()
  {
    return(requests);
  }
  
  
  public void shutdown()
  {
    this.shutdown = true;
        
    synchronized(this)
    {
      stop = true;
      this.notify();
    }
  }


  public RESTClient worker(short id)
  {
    return(workers[id-this.htsrvs-1]);
  }
  
  
  int worker = 0;
  public RESTClient worker() throws Exception
  {
    int tries = 0;
    
    while(++tries < 32)
    {
      for (int i = 0; i < workers.length; i++)
      {
        int pos = worker++ % workers.length;
        
        if (workers[pos] != null && workers[pos].up())
          return(workers[pos]);          
      }
      
      sleep(250);
    }
    
    throw new Exception("No available RESTEngines, bailing out");
  }
  
  
  public void register(RESTClient client)
  {
    workers[client.id()-this.htsrvs-1] = client;
  }
  
  
  public void deregister(RESTClient client)
  {
    workers[client.id()-this.htsrvs-1] = null;
  }
  
  
  private boolean open()
  {
    try
    {
      ServerSocket socket = null;
      
      socket = new ServerSocket(ssl.port());
      socket.close();
      
      socket = new ServerSocket(plain.port());
      socket.close();
      
      socket = new ServerSocket(admin.port());
      socket.close();
      
      return(true);
    }
    catch (Exception e)
    {
      return(false);
    }
  }
  
  
  private void ensure()
  {
    try 
    {
      synchronized(this)
      {
        if (!shutdown)
        {
          Process process = new Process(config);
          logger.fine("Checking all instances are up");
          
          ArrayList<ServerType> servers = Cluster.notRunning(this);
          
          for(ServerType server : servers)
          {
            logger.info("Starting instance "+server.id);
            process.start(server.type,server.id);
          }
        }        
      }
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
    }
  }
  
  
  @Override
  public void run()
  {
    try 
    {
      synchronized(this)
      {
        while(!stop)
        {
          Cluster.setStatistics(this);
          this.wait(4*this.heartbeat);
        }
      }
      
      sleep(100);
    }
    catch (Exception e) {logger.log(Level.SEVERE,e.getMessage(),e);}
    
    ThreadPool.shutdown();
    logger.info("Server stopped");
  }
  
  
  private PrintStream stdout() throws Exception
  {
    String srvout = config.getLogger().getServerOut(id);
    return(new PrintStream(new BufferedOutputStream(new FileOutputStream(srvout)), true));
  }
}