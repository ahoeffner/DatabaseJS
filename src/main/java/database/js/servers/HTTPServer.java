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
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;


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
    
    HashMap<SelectionKey,HTTPRequest> incomplete =
      new HashMap<SelectionKey,HTTPRequest>();

    ByteBuffer buf = ByteBuffer.allocate(2048);

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
          System.out.println("Key accepted");
          SelectionKey key = iterator.next();
          iterator.remove();
          
          if (key.isAcceptable())
          {
            SocketChannel sac = server.accept();
            sac.configureBlocking(false);
            sac.register(selector,SelectionKey.OP_READ);
            logger.fine("Connection Accepted: "+sac.getLocalAddress());
          }
          
          else if (key.isReadable())
          {
            try
            {
              SocketChannel req = (SocketChannel) key.channel();
              
              buf.rewind();
              int read = req.read(buf);
              
              if (read < 0)
              {
                req.close();
                continue;                
              }
              
              if (read == 0)
                continue;                                
              
              HTTPRequest request = incomplete.remove(key);
              if (request == null) request = new HTTPRequest();
              
              if (!request.add(buf.array(),read)) 
              {
                incomplete.put(key,request);
                continue;
              }
              
              request.parse();
              
              int len = 8;
              
              String EOL = "\r\n";
              String response = "HTTP/1.1 200 OK"+EOL+
                                "Connection: Keep-Alive"+EOL+
                                "Keep-Alive: timeout=5, max=1000"+EOL+
                                "Content-Length: "+len+EOL+
                                "Content-Type: text/html"+EOL+EOL+
                                "Hello II";
              
              buf.rewind();
              buf.put(response.getBytes());
              buf.position(0);
              req.write(buf);
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

    logger.info(type+" HTTPServer stopped");
  }
  
  
  private void cleanout(HashMap<SelectionKey,HTTPRequest> incomplete)
  {
    ArrayList<SelectionKey> cancelled = new ArrayList<SelectionKey>();
    
    for(Map.Entry<SelectionKey,HTTPRequest> entry : incomplete.entrySet())
      if (entry.getValue().cancelled()) cancelled.add(entry.getKey());
    
    for(SelectionKey key : cancelled) incomplete.remove(key);
  }
  
  
  public static enum Type
  {
    SSL,
    Plain,
    Admin
  }
}
