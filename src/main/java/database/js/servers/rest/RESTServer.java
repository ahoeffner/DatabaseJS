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


public class RESTServer extends Thread
{
  private short id = -1;
  private long started = -1;
  private boolean connected = false;
  private SocketReader reader = null;
  private final ByteBuffer buf = ByteBuffer.allocate(8);

  private final int port;
  private final short rid;
  private final Server server;
  private final Config config;
  private final Logger logger;
  private final MailBox mailbox;
  private final ThreadPool workers;
  private final HTTPChannel channel;
  
  private final static byte STOP = -1;
  private final static byte SHMMEM = 1;
  private final static byte STREAM = 2;
  
  
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
    
    this.setDaemon(true);
    this.setName("RESTServer");

    this.start();
  }
  
  
  @Override
  public void run()
  {
    logger.info("Starting RESTServer");

    long id = 0;
    int ext = 0;
    int size = 0;
    boolean stop = false;
    byte[] request = null;

    try
    {
      while(!stop)
      {
        while(!connected)
        {
          connected = connect();
          if (!connected) sleep(250);        
          reader = new SocketReader(channel.socket().getInputStream());
        }        
        
        byte type = reader.read();
        
        switch(type)
        {
          case STOP : 
            stop = true;
            break;
          
          case STREAM :
            id = getLong();
            ext = getInt();
            size = getInt();
            request = reader.read(size);
            break;
          
          case SHMMEM :
            id = getLong();
            ext = getInt();
            size = getInt();
            request = mailbox.read(ext,size);
        }
        
        logger.info("thread="+id+" extend="+ext+" size="+size+" <"+new String(request)+">");
      }
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
    }    

    logger.info("RESTServer stopped");
  }
  
  
  private int getInt() throws Exception
  {
    buf.clear();
    buf.put(reader.read(4));
    buf.flip();
    return(buf.getInt());
  }
  
  
  private long getLong() throws Exception
  {
    buf.clear();
    buf.put(reader.read(8));
    buf.flip();
    return(buf.getLong());
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
      e.printStackTrace();
      return(false);
    }
    
    return(true);
  }
}
