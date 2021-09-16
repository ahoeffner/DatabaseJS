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

import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Config;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;


public class HTTPServer extends Thread
{
  private final int port;
  private final Type type;
  private final Logger logger;
  private final boolean embedded;
  private final boolean redirect;
  
  
  public HTTPServer(Config config, Type type, boolean embedded) throws Exception
  {
    this.type = type;
    this.redirect = false;
    this.embedded = embedded;
    this.logger = config.getLogger().logger;
    
    switch(type)
    {
      case SSL    : port = config.getHTTP().ssl();    break;
      case Plain  : port = config.getHTTP().plain();  break;
      case Admin  : port = config.getHTTP().admin();  break;      
      default     : port = -1;
    }
  }
  
  
  public void run()
  {
    if (port <= 0) 
      return;

    try
    {
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
        
        while(iterator.hasNext())
        {          
          SelectionKey key = iterator.next();
          iterator.remove();
          
          System.out.println(key+" acc "+key.isAcceptable()+" conn "+key.isConnectable()+" valid "+key.isValid()+" read "+key.isReadable()+" write "+key.isWritable());
          
          if (key.isAcceptable())
          {
            SocketChannel sc = server.accept();
            sc.configureBlocking(false);
            sc.register(selector,SelectionKey.OP_READ);
            System.out.println("Connection Accepted: "+sc.getLocalAddress());
          }
          
          else if (key.isReadable())
          {
            try
            {
              ByteBuffer buf = ByteBuffer.allocate(2048);
              SocketChannel sc = (SocketChannel) key.channel();
              
              int read = sc.read(buf);
              
              if (read <= 0)
              {
                sc.register(selector,0);
                continue;
              }
              
              System.out.println("<"+new String(buf.array()).trim()+">");
              
              String newl = "\r\n";
              String response = "HTTP/1.1 200 OK"+newl+
                                "Content-Length: 8"+newl+
                                "Content-Type: text/plain"+newl+newl+
                                "Hello II";

              buf = ByteBuffer.allocate(1024);
              buf.put(response.getBytes());
              buf.position(0);
              sc.write(buf);
            }
            catch (Exception e)
            {
              ;
            }
          }
        }
      }
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
      e.printStackTrace();
    }

    logger.info(type+" Server stopped");
  }
  
  
  public static enum Type
  {
    SSL,
    Plain,
    Admin
  }
}
