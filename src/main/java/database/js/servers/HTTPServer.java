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
import java.util.HashMap;
import java.util.Iterator;
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

import java.util.ArrayList;
import java.util.Map;


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
    
    HashMap<SelectionKey,Request> incomplete =
      new HashMap<SelectionKey,Request>();

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
              
              Request request = incomplete.remove(key);
              if (request == null) request = new Request();
              
              if (!request.add(buf.array(),read)) 
              {
                incomplete.put(key,request);
                continue;
              }
              
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
  
  
  private void cleanout(HashMap<SelectionKey,Request> incomplete)
  {
    ArrayList<SelectionKey> cancelled = new ArrayList<SelectionKey>();
    
    for(Map.Entry<SelectionKey,Request> entry : incomplete.entrySet())
      if (entry.getValue().cancelled()) cancelled.add(entry.getKey());
    
    for(SelectionKey key : cancelled) incomplete.remove(key);
  }
  
  
  public static enum Type
  {
    SSL,
    Plain,
    Admin
  }
  
  
  private static class Request
  {
    int length = -1;
    int header = -1;
    String method = null;
    byte[] request = new byte[0];
    long started = System.currentTimeMillis();
    
    
    boolean done()
    {
      return(length >= 0);
    }
    
    
    boolean cancelled()
    {
      return(System.currentTimeMillis() - started > 30000);
    }
    
    
    boolean add(byte[] data, int len) throws Exception
    {
      int last = request.length;
      byte[] request = new byte[this.request.length+len];
      
      System.arraycopy(data,0,request,last,len);
      System.arraycopy(this.request,0,request,0,this.request.length);
      
      this.request = request;
      
      if (method == null && request.length > 2)
        method = new String(request,0,3);
      
      if (header < 0)
      {
        if (method == null || method.equals("GET")) this.bckward(last);          
        else                                        this.forward(last);
      }
      
      if (header >= 0 && length < 0)
      {
        String headers = new String(request,0,header);
        int clpos = headers.indexOf("Content-Length");
        
        if (clpos < 0) 
        {
          length = 0;
        }
        else
        {
          int b = clpos;
          int e = clpos;
          
          for (int c = clpos; c < header; c++)
          {
            if (request[c] == ':')
              b = c + 1;
              
            if (request[c] == '\r' && request[c+1] == '\n')
            {
              e = c;
              break;
            }
          }
          
          String cl = new String(request,b,e-b);
          length = Integer.parseInt(cl.trim());
        }
      }
      
      return(length >= 0);
    }
    
    
    void forward(int last)
    {
      int start = 0;
      if (last > 3) start = last - 3;
      
      for (int h = start; h < request.length-3; h++)
      {
        if (request[h] == '\r' && request[h+1] == '\n' && request[h+2] == '\r' && request[h+3] == '\n')
        {
          header = h - 1;
          return;
        }
      }
    }
    
    
    void bckward(int last)
    {
      for (int h = request.length-1; h >= 3 && h >= last-3; h--)
      {
        if (request[h-3] == '\r' && request[h-2] == '\n' && request[h-1] == '\r' && request[h] == '\n')
        {
          header = h - 4;
          return;
        }
      }
    }
    
    
    @Override
    public String toString()
    {
      return(new String(request));
    }
  }
}
