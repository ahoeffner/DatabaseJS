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

package database.js.control;

import ipc.Broker;
import ipc.Message;
import ipc.Listener;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.config.Topology;
import database.js.servers.HTTPServer;
import static database.js.config.Config.*;


public class Server extends Thread implements Listener
{
  private final short id;
  private final Broker broker;
  private final Logger logger;
  private final Config config;
  private final Config.Type type;
  private final boolean embedded;
  
  private final HTTPServer ssl;
  private final HTTPServer plain;
  private final HTTPServer admin;
  
  
  public static void main(String[] args)
  {
    try
    {
      new Server(Short.parseShort(args[0]));
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  
  Server(short id) throws Exception
  {
    this.id = id;
    this.config = new Config();
    this.setName("Server Main");

    config.getLogger().open(id);
    this.logger = config.getLogger().logger;
    
    logger.info("Preparing instance");

    int http = 1;
    if (config.getTopology().hotstandby()) http++;
    
    if (id < http) type = Type.http;
    else           type = Type.rest;

    Broker.logger(logger);    
    boolean master = type == Type.http;
    
    this.broker = new Broker(config.getIPConfig(),this,id,master);
    this.embedded = config.getTopology().type() == Topology.Type.Micro;

    this.ssl = new HTTPServer(this,HTTPServer.Type.ssl,embedded);
    this.plain = new HTTPServer(this,HTTPServer.Type.plain,embedded);
    this.admin = new HTTPServer(this,HTTPServer.Type.admin,embedded);

    this.start();
    
    if (broker.manager())
      startup();
  }
  
  
  private void startup()
  {    
    ssl.start();
    plain.start();
    admin.start();
  }
  
  
  public void shutdown()
  {
    synchronized(this)
     {this.notify();}
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
  }

  @Override
  public void onNewManager(short s)
  {
  }

  @Override
  public void onMessage(ArrayList<Message> arrayList)
  {
  }
  
  
  @Override
  public void run()
  {
    try {synchronized(this) {this.wait();}}
    catch (Exception e) {logger.log(Level.SEVERE,e.getMessage(),e);}
    logger.info("Server stopped");
  }
}
