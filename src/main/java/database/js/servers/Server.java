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

import ipc.Broker;
import ipc.Message;
import ipc.Listener;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.config.Topology;
import database.js.control.Process;
import database.js.cluster.Cluster;
import database.js.pools.ThreadPool;
import database.js.servers.http.HTTPServer;
import database.js.cluster.Cluster.ServerType;
import database.js.servers.http.HTTPServerType;


/**
 * 
 * The start/stop sequence is rather complicated:
 * 
 * There are 2 roles:
 *   The ipc guarantees that only 1 process holds the given role at any time.
 *   
 *   The secretary role. All processes is eligible for this role
 *   The manager role. Only http processes is eligible for this role
 * 
 * 
 * The secretary is responsible for keeping all other servers alive.
 * The manager is responsible for the http interfaces, including the admin port.
 * 
 * When a server starts, it will check to see if it has become the secretary. in which
 * case it will start all other processes that is not running.
 * 
 * When the manager receives a shutdown command, it will pass it on to the secretary.
 * The secretary will then cease the automatic keep alive, and send a shutdown message
 * to all other processes, and shut itself down.
 * 
 */
public class Server extends Thread implements Listener
{
  private final short id;
  private final int heartbeat;
  private final Broker broker;
  private final Logger logger;
  private final Config config;
  private final boolean embedded;

  private volatile boolean stop = false;
  private volatile boolean shutdown = false;
  
  private final HTTPServer ssl;
  private final HTTPServer plain;
  private final HTTPServer admin;
  
  
  public static void main(String[] args)
  {
    try {new Server(Short.parseShort(args[0]));}
    catch (Exception e) {e.printStackTrace();}
  }
  
  
  Server(short id) throws Exception
  {
    this.id = id;
    this.config = new Config();
    this.setName("Server Main");

    config.getLogger().open(id);
    this.logger = config.getLogger().logger;    
    Process.Type type = Cluster.getType(config,id);
    
    Broker.logger(logger);
    
    try
    {
      if (Cluster.isRunning(config,id))
      {
        logger.severe("Server is already running");
        throw new Exception("Server is already running");            
      }
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
      throw e;
    }
    
    boolean master = type == Process.Type.http;    
    this.heartbeat = config.getIPConfig().heartbeat;
    this.embedded = config.getTopology().servers() > 0;
    this.broker = new Broker(config.getIPConfig(),this,id,master);

    this.ssl = new HTTPServer(this, HTTPServerType.ssl,embedded);
    this.plain = new HTTPServer(this, HTTPServerType.plain,embedded);
    this.admin = new HTTPServer(this, HTTPServerType.admin,embedded);

    if (broker.manager() && type == Process.Type.http) 
      startup();

    this.start();
    this.ensure();
  }
  
  
  private void startup()
  {
    ssl.start();
    plain.start();
    admin.start();
  }
  
  
  private void ensure()
  {
    try 
    {
      synchronized(this)
      {
        if (!shutdown && broker.secretary())
        {
          Process process = new Process(config);
          logger.info("Checking all instances are up");
          
          ArrayList<ServerType> servers = Cluster.notRunning(config);
          
          for(ServerType server : servers)
            process.start(server.type,server.id);
        }        
      }
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
    }
  }
  
  
  public Logger logger()
  {
    return(logger);
  }
  
  
  public short id()
  {
    return(id);
  }
  
  
  public Config config()
  {
    return(config);
  }
  
  
  public Broker broker()
  {
    return(broker);
  }


  @Override
  public void onServerUp(short s)
  {
  }


  @Override
  public void onServerDown(short s)
  {
    ensure();
  }


  @Override
  public void onNewManager(short s)
  {
    if (s == id)
    {
      synchronized(this)
      {
        if (!stop && !shutdown)
        {
          startup();
          logger.info("Switching to http process "+id);
        }
      }
    }
  }


  @Override
  public void onMessage(ArrayList<Message> messages)
  {
    String cmd = null;
    
    for(Message message : messages)
    {
      int pos = 3;
      byte[] msg = message.body();
      String adm = new String(msg,0,pos);
      
      if (adm.equals("ADM"))
      {
        while(msg[pos] != '\r' && msg[pos+1] != '\n') 
            pos++;
          
        cmd = new String(msg,0,pos+2);
        
        if (cmd.startsWith("ADM /shutdown"))
        {
          shutdown = true;
          break;
        }
      }
    }
    
    if (shutdown)
    {
      try
      {
        if (broker.secretary())
        {
          Short[] servers = Cluster.getServers(config);
          logger.info("Broadcasting shutdown");

          // Signal other servers to shutdown
          for (short i = 0; i < servers[0] + servers[1]; i++)
            if (i != id) broker.send(i,cmd.getBytes());

          int tries = 0;
          int down = Cluster.notRunning(config).size();;
                    
          // Wait for other servers to shutdown
          while(servers[0] + servers[1] - down > 1)
          {
            logger.info("waiting for other servers to shutdown");

            if (++tries == 256) 
              throw new Exception("Unable to shutdown servers: "+(servers[0] + servers[1])+", down: "+down);
            
            Thread.sleep(config.getIPConfig().heartbeat);
            down = Cluster.notRunning(config).size();
          }
        }
      }
      catch (Exception e)
      {
        logger.log(Level.SEVERE,e.getMessage(),e);
      }
    }

    synchronized(this)
    {
      stop = true;
      this.notify();
    }

    logger.info("Shutting down");
  }
  
  
  public void shutdown()
  {
    try
    {
      logger.info("Shutdown received id="+broker.id()+" Secretary="+broker.getSecretary());
      byte[] msg = "ADM /shutdown HTTP/1.1\r\n".getBytes();
      broker.send(broker.getSecretary(),msg);
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
        }
      }
    }
    catch (Exception e) {logger.log(Level.SEVERE,e.getMessage(),e);}
    
    ThreadPool.shutdown();
    logger.info("Server stopped");
  }
}