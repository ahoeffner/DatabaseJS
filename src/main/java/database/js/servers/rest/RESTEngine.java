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

import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.servers.Server;
import database.js.client.HTTPRequest;
import database.js.client.HTTPResponse;
import java.nio.channels.SocketChannel;
import database.js.servers.http.HTTPChannel;


public class RESTEngine extends Thread
{
  private short id = -1;
  private long started = -1;
  private boolean connected = false;
  
  private final int port;
  private final Logger logger;
  private final Server server;
  private final boolean client;
  private final HTTPChannel channel;
  
  
  public RESTEngine(HTTPChannel channel, short id) throws Exception
  {
    this.id = id;
    this.port = 0;
    this.client = true;
    this.connected = true;
    this.channel = channel;
    this.server = channel.server();
    this.started = System.currentTimeMillis();
    this.logger = server.config().getLogger().rest;
  }
  
  
  public RESTEngine(Server server, int port, boolean ssl) throws Exception
  {
    this.port = port;
    this.client = false;
    this.server = server;
    SocketChannel channel = SocketChannel.open();    
    this.logger = server.config().getLogger().rest;
    this.channel = new HTTPChannel(server,channel,ssl);

    this.setDaemon(true);
    this.setName("RESTEngine");
  }
  
  
  public short id()
  {
    return(id);
  }
  
  
  public long started()
  {
    return(started);
  }
  
  
  @Override
  public void run()
  {
    logger.info("Starting RESTEngine");

    try
    {
      while(!connected)
      {
        connected = connect();
        if (!connected) sleep(250);        
      }
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
    }    

    logger.info("RESTEngine stopped");
  }
  
  
  private boolean connect()
  {
    try
    {
      this.channel.connect(port);
      HTTPRequest request = new HTTPRequest("localhost","/connect",""+server.id());

      channel.write(request.getPage());      
      HTTPResponse response = new HTTPResponse();

      while(!response.finished())
        response.add(channel.read());        

      String[] args = new String(response.getBody()).split(" ");
      
      short id = Short.parseShort(args[0]);
      long started = Long.parseLong(args[1]);
      
      if (this.id >= 0)
      {
        if (id != this.id || started != this.started)
          logger.info("HTTPServer switched");          
      }
      
      this.id = id;
      this.started = started;
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return(false);
    }
    
    return(true);
  }
  
  
  private static class RESTEngineClient extends Thread
  {
    private final RESTEngine engine;
    
    
    RESTEngineClient(RESTEngine engine)
    {
      this.engine = engine;
    }
  }
  
  
  private static class RESTEngineServer extends Thread
  {
    private final RESTEngine engine;
    
    
    RESTEngineServer(RESTEngine engine)
    {
      this.engine = engine;
    }
  }
}
