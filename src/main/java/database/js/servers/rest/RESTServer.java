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

import java.util.Arrays;
import java.nio.ByteBuffer;
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
import java.nio.channels.ClosedChannelException;


public class RESTServer implements RESTConnection
{
  private RESTReader reader = null;
  private RESTWriter writer = null;
  private HTTPChannel rchannel = null;
  private HTTPChannel wchannel = null;
  private volatile byte[] httpid = null;

  private ByteBuffer buffer = ByteBuffer.allocate(10);

  private final int port;
  private final short rid;
  private final Server server;
  private final Config config;
  private final Logger logger;
  private final MailBox mailbox;
  private final ThreadPool workers;
  
  
  public static void main(String[] args) throws Exception
  {
    Server.main(new String[] {"3"});
  }

  
  public RESTServer(Server server) throws Exception
  {
    this.server = server;
    this.config = server.config();
    this.logger = config.getLogger().rest;
    this.mailbox = new MailBox(config,server.id());
    
    logger.info("RESTServer starting ...");
    
    int http = 1;
    this.port = config.getHTTP().admin();
    if (config.getTopology().hotstandby()) http++;

    this.rid = (short) (server.id() - http);
    this.workers = new ThreadPool(config.getTopology().workers());
    
    serve();
  }
  
  
  public void serve()
  {
    int tries = 0;    
    
    if (reader == null) logger.info("RESTServer connecting ...");
    else                logger.info("RESTServer reconnecting ...");
    
    while(!connect())
    {
      if (++tries > 256)
      {
        logger.severe("Unable to connect to HTTPServer, bailing out");
        System.exit(-1);
      }
      
      if (tries % 16 == 0)
        logger.info("Unable to connect to HTTPServer");
      
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
      System.exit(-1);
    }
  }
  
  
  private boolean connect()
  {
    try
    {
      SocketChannel rchannel = SocketChannel.open();    
      this.rchannel = new HTTPChannel(server,rchannel,true);

      SocketChannel wchannel = SocketChannel.open();    
      this.wchannel = new HTTPChannel(server,wchannel,true);
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
      logger.severe("Unable to start RESTServer, bailing out");
      System.exit(-1);
    }

    if (!connect(this.rchannel)) 
      return(false);
    
    byte[] readsig = this.httpid;
    // Make sure HTTPServer has not switched
    
    if (!connect(this.wchannel)) 
      return(false);
    
    if (!Arrays.equals(readsig,this.httpid))
      return(false);
    
    logger.info("Connected to HTTPServer");
    return(true);
  }
  
  
  private boolean connect(HTTPChannel channel)
  {
    try
    {
      channel.configureBlocking(false);
      channel.connect(port);

      for (int i = 0; i < 8; i++)
      {
        Thread.sleep(25);
        channel.socket().getOutputStream().flush();
        //Make absolute sure response is flushed
      }
      
      channel.configureBlocking(true);

      HTTPRequest request = new HTTPRequest("localhost","/connect");      
      request.setBody(server.id()+" "+server.started());
      
      logger.finest("Sending connect request to HTTPServer");
      channel.write(request.getPage());   
      HTTPResponse response = new HTTPResponse();

      logger.finest("Waiting for response from HTTPServer");
      while(!response.finished())
      {
        ByteBuffer buf = channel.read();
        
        if (buf == null) 
        {
          logger.warning("Missing reply from HTTPServer");
          return(false);          
        }

        response.add(buf);
      }

      String[] args = new String(response.getBody()).split(" ");
      
      short id = Short.parseShort(args[0]);
      long started = Long.parseLong(args[1]);
      byte[] signature = signature(id,started);
      if (this.httpid == null) this.httpid = signature;
      
      if (!Arrays.equals(signature,this.httpid))
          logger.info("HTTPServer restarted or switched"); 
            
      this.httpid = signature;
    }
    catch (Exception e)
    {
      boolean skip = false;
      String message = e.getMessage();
      if (message == null) message = "Unknown error";

      if (e instanceof ClosedChannelException) skip = true;
      if (message.equals("Connection refused")) skip = true;

      if (!skip) logger.log(Level.WARNING,e.getMessage(),e);
      return(false);
    }
    
    return(true);
  }
  
  
  private byte[] signature(Short id, long started)
  {
    buffer.clear();
    buffer.putShort(id);
    buffer.putLong(started);
    byte[] signature = new byte[10];
    System.arraycopy(buffer.array(),0,signature,0,10);
    return(signature);
  }


  @Override
  public String parent()
  {
    return("RESTServer");
  }


  @Override
  public void failed()
  {
    logger.severe("RESTServer failed, reconnect");
    serve();
  }


  @Override
  public Logger logger()
  {
    return(logger);
  }


  @Override
  public InputStream reader() throws Exception
  {
    return(rchannel.socket().getInputStream());
  }


  @Override
  public OutputStream writer() throws Exception
  {
    return(wchannel.socket().getOutputStream());
  }


  private int incoming = 0;
  @Override
  public void received(ArrayList<RESTComm> calls)
  {
    for(RESTComm http : calls)
    {
      incoming++;
      this.server.request();
      byte[] data = http.data();
      
      if (http.extend >= 0) 
        data = mailbox.read(http.extend,http.size);
      
      if (incoming % 100 == 0)
        logger.info("Received "+incoming);
      
      writer.write(http);      
    }
  }
}
