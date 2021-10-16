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

import java.util.ArrayList;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.servers.Server;
import database.js.cluster.MailBox;
import database.js.pools.ThreadPool;
import database.js.client.HTTPRequest;
import database.js.client.HTTPResponse;
import java.nio.channels.SocketChannel;
import database.js.servers.http.HTTPChannel;


public class RESTServer implements RESTConnection
{
  private short id = -1;
  private long started = -1;

  private RESTReader reader = null;
  private RESTWriter writer = null;

  private final int port;
  private final short rid;
  private final Server server;
  private final Config config;
  private final Logger logger;
  private final MailBox mailbox;
  private final ThreadPool workers;
  private final HTTPChannel channel;
  
  
  public static void main(String[] args) throws Exception
  {
    Server.main(new String[] {"3"});
  }

  
  public RESTServer(Server server) throws Exception
  {
    boolean ssl = true;

    this.server = server;
    this.config = server.config();
    this.logger = config.getLogger().rest;
    this.mailbox = new MailBox(config,server.id());
    
    int http = 1;
    this.port = config.getHTTP().admin();
    if (config.getTopology().hotstandby()) http++;

    SocketChannel channel = SocketChannel.open();    
    this.channel = new HTTPChannel(server,channel,ssl);

    this.rid = (short) (server.id() - http);
    this.workers = new ThreadPool(config.getTopology().workers());
    
    accept();
  }
  
  
  public void accept()
  {
    int tries = 0;
    
    while(!connect())
    {
      if (++tries > 16)
      {
        logger.severe("Unable to connect to HTTPServer, bailing out");
        server.shutdown();
      }
      
      try {Thread.sleep(250);}
      catch (Exception e) {logger.log(Level.SEVERE,e.getMessage(),e);}
    }
    
    try
    {
      reader = new RESTReader(this);
      writer = new RESTWriter(this);
      
      reader.start();
      writer.start();
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
      logger.severe("Unable to start RESTServer, bailing out");
      server.shutdown();
    }
  }
  
  
  private boolean connect()
  {
    try
    {
      this.channel.connect(port);
      this.channel.configureBlocking(true);

      HTTPRequest request = new HTTPRequest("localhost","/connect",""+id);      
      request.setBody(server.id()+" "+server.started());
      
      channel.write(request.getPage());   
      HTTPResponse response = new HTTPResponse();

      while(!response.finished())
        response.add(channel.read());

      String[] args = new String(response.getBody()).split(" ");
      
      short id = Short.parseShort(args[0]);
      long started = Long.parseLong(args[1]);
      
      logger.info("Connected to HTTPServer, id="+id+" started="+started);
      
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
      logger.log(Level.WARNING,e.getMessage(),e);
      return(false);
    }
    
    return(true);
  }


  @Override
  public String parent()
  {
    return("RESTServer");
  }


  @Override
  public void failed()
  {
    logger.severe("RESTServer failed, bailing out");
  }


  @Override
  public Logger logger()
  {
    return(logger);
  }


  @Override
  public InputStream reader() throws Exception
  {
    return(channel.socket().getInputStream());
  }


  @Override
  public OutputStream writer() throws Exception
  {
    return(channel.socket().getOutputStream());
  }


  @Override
  public void received(ArrayList<RESTComm> calls)
  {
    logger.info("Received request");
    
    for(RESTComm http : calls)
      writer.write(http);
  }
}
