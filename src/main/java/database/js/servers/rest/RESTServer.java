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

package database.js.servers.rest;

import java.nio.ByteBuffer;
import database.js.admin.Client;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.servers.Server;
import database.js.pools.ThreadPool;


public class RESTServer extends Thread
{
  private final short rid;
  private final Server server;
  private final Config config;
  private final Logger logger;
  private final Client client;
  private final ThreadPool workers;
  private final ByteBuffer buf = ByteBuffer.allocate(16);
  
  
  public static void main(String[] args) throws Exception
  {
    Server.main(new String[] {"3"});
  }

  
  public RESTServer(Server server) throws Exception
  {
    this.server = server;
    this.config = server.config();
    this.logger = config.getLogger().rest;
    
    int port = config.getHTTP().admin();    
    this.client = new Client("localhost",port,true);
    
    int http = 1;
    if (config.getTopology().hotstandby()) http++;
    
    this.rid = (short) (server.id() - http);
    
    this.setDaemon(true);
    this.setName("RESTServer");
    this.workers = new ThreadPool(config.getTopology().workers());
    
    this.start();
  }
  
  
  @Override
  public void run()
  {
    ServerID server = new ServerID();
    logger.info("Starting RESTServer "+rid);
    
    try
    {
      while(true)
      {
        server = connect(server);
        
        while(true)
        {
          try
          {
            ;
          }
          catch (Exception e)
          {
            // TODO: Add catch code
            e.printStackTrace();
          }
        }
      }
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
    }

    logger.info("RESTServer "+rid+" stopped");
  }
  
  
  private ServerID connect(ServerID id) throws Exception
  {
    ServerID nid = new ServerID();
    
    while(true)
    {
      try
      {
        client.connect();
        byte[] data = client.send("connect");

        buf.clear();
        buf.put(data);
        
        buf.flip();
        nid.prc = buf.getShort();
        nid.started = buf.getLong();
        
        if (id.prc != nid.prc || id.started != nid.started)
          logger.info("Server switched/restarted");
        
        return(nid);
      }
      catch (Exception e) 
      {
        logger.info("Server down");
        sleep(250);
      }
    }        
  }
  
  
  private static class ServerID
  {
    short prc = -1;
    long started = -1;
    
    public String toString()
    {
      return(prc+" "+started);
    }
  }
}
