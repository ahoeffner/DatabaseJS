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
    PrintStream out = stdout();
    this.setName("Server Main");

    System.setOut(out);
    System.setErr(out);

    config.getLogger().open(id);
    this.logger = config.getLogger().logger;    
    Process.Type type = Cluster.getType(config,id);
    
    Broker.logger(logger);
    logger.info("Starting up");
    
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
    
    Thread.sleep(100);
    logger.info("Instance startet"+System.lineSeparator());

    this.ensure();
  }
  
  
  private void startup()
  {
    if (!test())
    {
      logger.info("Address already in use");
      return;
    }
    
    logger.info("Open http sockets");

    ssl.start();
    plain.start();
    admin.start();
  }
  
  
  private boolean test()
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
        if (!shutdown && broker.secretary())
        {
          Process process = new Process(config);
          logger.fine("Checking all instances are up");
          
          ArrayList<ServerType> servers = Cluster.notRunning(this);
          
          for(ServerType server : servers)
          {
            logger.info("Starting instance "+server.id);
            broker.forceUpdate();
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
  public void onServerUp(short id)
  {
    logger.info("Instance "+id+" is online");
  }


  @Override
  public void onServerDown(short id)
  {
    logger.info("Instance "+id+" is down");
    ensure();
  }


  @Override
  public void onNewManager(short id)
  {
    logger.info("Switching manager to "+id);

    if (id == this.id)
    {
      synchronized(this)
      {
        if (!stop && !shutdown)
        {
          try
          {
            startup();
          }
          catch (Exception e)
          {
            logger.log(Level.SEVERE,e.getMessage(),e);
          }
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
          int down = Cluster.notRunning(this).size();;
          logger.info("Online servers: "+(servers[0] + servers[1] - down));
                    
          // Wait for other servers to shutdown
          while(servers[0] + servers[1] - down > 1)
          {
            if (++tries == 16) 
              throw new Exception("Unable to shutdown servers: "+(servers[0] + servers[1])+", down: "+down);
            
            Thread.sleep(config.getIPConfig().heartbeat);
            down = Cluster.notRunning(this).size();
          }
        }
      }
      catch (Exception e)
      {
        logger.log(Level.SEVERE,e.getMessage(),e);
      }
    }

    logger.info("Shutting down");

    synchronized(this)
    {
      stop = true;
      this.notify();
    }
  }
  
  
  public void shutdown()
  {
    this.shutdown = true;
    boolean delivered = false;
    
    try
    {
      String nl = System.lineSeparator();
      if (broker.secretary()) logger.info(nl+nl+"Shutdown command received"+nl);
      else logger.info(nl+nl+"Shutdown command received, passing on to secretary "+broker.getSecretary()+nl);
      
      byte[] msg = "ADM /shutdown HTTP/1.1\r\n".getBytes();
      
      Message message = broker.send(broker.getSecretary(),msg);
      delivered = message.delivered(100);
      
      if (!delivered) 
        logger.warning("Could not pass on shutdown command to secretary");
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
    }
    
    if (!delivered)
    {
      synchronized(this)
      {
        stop = true;
        this.notify();
      }
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