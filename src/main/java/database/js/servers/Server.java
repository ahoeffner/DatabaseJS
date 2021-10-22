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
import database.js.cluster.ProcessMonitor;
import database.js.servers.rest.RESTServer;
import database.js.servers.rest.RESTClient;
import database.js.servers.http.HTTPServer;
import database.js.cluster.Cluster.ServerType;
import database.js.cluster.Statistics;
import database.js.servers.http.HTTPServerType;


/**
 *
 *
 */
public class Server extends Thread
{
  private final short id;
  private final long pid;
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
  private final LoadBalancer loadblcr;

  private volatile boolean sowner = false;
  private volatile boolean powner = false;

  
  public static void main(String[] args)
  {
    // args[0] is instance name
    try {new Server(Short.parseShort(args[1]));}
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
    
    Cluster.init(this);
    ProcessMonitor.init(this);
    
    if (Cluster.isRunning(id,pid))
    {
      logger.warning("Server "+id+" is already running. Bailing out");
      System.exit(-1);
    }
    
    this.servers = config.getTopology().servers();
    Process.Type type = Cluster.getType(id);

    this.heartbeat = config.getTopology().heartbeat();
    
    if (type == Process.Type.rest)
    {
      this.ssl = null;
      this.plain = null;
      this.admin = null;
      this.loadblcr = null;
      this.embedded = true;
      this.rest = new RESTServer(this);
    }
    else
    {
      this.rest = null;
      this.embedded = servers <= 0;
      
      if (this.embedded) this.loadblcr = null;
      else this.loadblcr = new LoadBalancer(config);

      this.ssl = new HTTPServer(this,HTTPServerType.ssl,embedded);
      this.plain = new HTTPServer(this,HTTPServerType.plain,embedded);
      this.admin = new HTTPServer(this,HTTPServerType.admin,embedded);

      this.startup();
    }

    this.start();
    
    if (!sowner)
      powner = ProcessMonitor.aquireManagerLock();
    
    if (powner || ProcessMonitor.noManager())
      this.ensure();
    
    if (powner)
      ProcessMonitor.watchHTTP();
    
    if (!powner && !sowner)
      ProcessMonitor.watchManager();
    
    logger.info("Instance startet"+System.lineSeparator());
  }
  
  
  private boolean startup()
  {
    if (!open())
    {
      logger.info("Address already in use");
      return(false);
    }
    
    logger.info("Open http sockets");

    ssl.start();
    plain.start();
    admin.start();
    
    this.sowner = true;   
    ProcessMonitor.aquireHTTPLock();
    
    return(true);
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
  
  
  public boolean http()
  {
    return(sowner);
  }
  
  
  public boolean manager()
  {
    return(powner);
  }
  
  
  public boolean isHttpType()
  {
    return(this.rest == null);
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
  
  
  public void setManager()
  {
    if (ProcessMonitor.aquireManagerLock())
    {
      this.powner = true;
      ProcessMonitor.watchHTTP();      
    }
  }
  
  
  public void setHTTP()
  {
    if (powner)
      ProcessMonitor.releaseManagerLock();
    this.startup();
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
    Cluster.stop();
    this.shutdown = true;
    logger.info("Server "+id+" shutting down");
        
    synchronized(this)
    {
      stop = true;
      this.notify();
    }
  }


  public RESTClient worker(short id)
  {
    return(loadblcr.worker(id));
  }
  

  public RESTClient worker() throws Exception
  {
    return(loadblcr.worker());
  }
  
  
  public void register(RESTClient client)
  {
    loadblcr.register(client);
  }
  
  
  public void deregister(RESTClient client)
  {
    loadblcr.deregister(client);
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
  
  
  public void ensure()
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
          
          this.wait(this.heartbeat);
          //if (powner) checkCluster();
          
          if (Cluster.stop(this))
            stop = true;
        }
      }
      
      sleep(100);
    }
    catch (Exception e) {logger.log(Level.SEVERE,e.getMessage(),e);}
    
    ThreadPool.shutdown();
    logger.info("Server stopped");
  }
  
  
  private void checkCluster()
  {
    boolean ensure = false;
    ArrayList<Statistics> stats = Cluster.getStatistics();
    
    for(Statistics stat : stats)
    {
      if (stat.id() == this.id)
        continue;
      
      if (stat.procmgr())
        logger.severe("Process "+stat.id()+" claims to be manager. Check = "+ProcessMonitor.isManager());
      
      long alive = System.currentTimeMillis() - stat.updated();
      if (1.0 * alive > 1.25 * this.heartbeat)
      {
        ensure = true;
        logger.warning("Process "+stat.id()+" is dead");
      }
    }
    
    if (ensure && !Cluster.stop(this))
      ensure();
  }
  
  
  private PrintStream stdout() throws Exception
  {
    String srvout = config.getLogger().getServerOut(id);
    return(new PrintStream(new BufferedOutputStream(new FileOutputStream(srvout)), true));
  }
}