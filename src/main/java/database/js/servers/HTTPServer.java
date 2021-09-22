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
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.control.Server;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;


public class HTTPServer extends Thread
{
  private final int port;
  private final boolean ssl;
  private final int threads;
  private final Config config;
  private final Broker broker;
  private final Logger logger;
  private final boolean admin;
  private final boolean embedded;
  private final boolean redirect;
  private final ThreadPool workers;
  
  
  public HTTPServer(Server server, Type type, boolean embedded) throws Exception
  {
    this.redirect = false;
    this.embedded = embedded;
    this.broker = server.broker();
    this.config = server.config();
    this.logger = config.getLogger().logger;
    this.threads = config.getTopology().threads(); 

    switch(type)
    {
      case ssl    : this.port = config.getHTTP().ssl();   ssl = true;  admin = false; break;
      case plain  : this.port = config.getHTTP().plain(); ssl = false; admin = false; break;
      case admin  : this.port = config.getHTTP().admin(); ssl = true;  admin = true;  break;
      default: port = -1; ssl = false; admin = false;
    }
    
    System.out.println(type+" "+port);
    
    this.setDaemon(true);
    this.setName("HTTPServer("+type+")");
    this.workers = new ThreadPool(threads);
  }
  
  
  public Config config()
  {
    return(config);
  }
  
  
  public Broker broker()
  {
    return(broker);
  }
  
  
  public boolean embedded()
  {
    return(embedded);
  }
  
  
  public void run()
  {
    if (port <= 0) 
      return;
    
    HTTPBuffers buffers = new HTTPBuffers();
    
    HashMap<SelectionKey,HTTPRequest> incomplete =
      new HashMap<SelectionKey,HTTPRequest>();

    try
    {
      int requests = 0;
      Selector selector = Selector.open();
      
      InetAddress ip = InetAddress.getByName("localhost");
      ServerSocketChannel server = ServerSocketChannel.open();
      
      server.configureBlocking(false);
      server.bind(new InetSocketAddress(ip,port));
      
      server.register(selector,SelectionKey.OP_ACCEPT);
      
      while(true)
      {
        if (selector.select() <= 0)
          continue;         
        
        Set<SelectionKey> selected = selector.selectedKeys();
        Iterator<SelectionKey> iterator = selected.iterator();
        
        if (++requests % 64 == 0 && incomplete.size() > 0)
          cleanout(incomplete);
        
        while(iterator.hasNext())
        {
          SelectionKey key = iterator.next();
          iterator.remove();
          
          if (key.isAcceptable())
          {
            SocketChannel sac = server.accept();
            sac.configureBlocking(false);
            
            HTTPChannel hcl = new HTTPChannel(config,buffers,sac,ssl,admin);
            boolean accept = hcl.accept();

            if (accept)
            {
              sac.register(selector,SelectionKey.OP_READ,hcl);
              logger.fine("Connection Accepted: "+sac.getLocalAddress());
            }
          }
          
          else if (key.isReadable())
          {
            try
            {
              HTTPChannel hcl = (HTTPChannel) key.attachment();
              SocketChannel req = (SocketChannel) key.channel();
              
              ByteBuffer buf = hcl.read();
                            
              if (buf == null)
              {
                req.close();
                continue;
              }
              
              int read = buf.remaining();
              System.out.println("Read "+read);
              
              HTTPRequest request = incomplete.remove(key);
              if (request == null) request = new HTTPRequest(hcl);
              
              if (!request.add(buf.array(),read)) 
              {
                incomplete.put(key,request);
                continue;
              }

              workers.submit(new HTTPWorker(this,request));
            }
            catch (Exception e)
            {
              logger.log(Level.SEVERE,e.getMessage(),e);
            }
          }
        }
      }
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
    }

    logger.info("HTTPServer stopped");
  }
  
  
  private void cleanout(HashMap<SelectionKey,HTTPRequest> incomplete)
  {
    ArrayList<SelectionKey> cancelled = new ArrayList<SelectionKey>();
    
    for(Map.Entry<SelectionKey,HTTPRequest> entry : incomplete.entrySet())
      if (entry.getValue().cancelled()) cancelled.add(entry.getKey());
    
    for(SelectionKey key : cancelled) 
    {
      incomplete.remove(key);
      
      try
      {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        SocketChannel rsp = (SocketChannel) key.channel();
        buf.put(err400());
        buf.position(0);
        rsp.write(buf);
        rsp.close();
      }
      catch (Exception e) {;}
    }
  }
  
  
  private String EOL = "\r\n";
  
  private byte[] err400()
  {
    String msg = "<b>Bad Request</b>";
    
    String page = "HTTP/1.1 200 Bad Request" + EOL +
                  "Content-Type: text/html" + EOL +
                  "Content-Length: "+msg.length() + EOL + EOL + msg;
    
    return(page.getBytes());
  }
  
  
  public static enum Type
  {
    ssl,
    plain,
    admin
  }
}
