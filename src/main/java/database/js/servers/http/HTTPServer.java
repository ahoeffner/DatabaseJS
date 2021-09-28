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

package database.js.servers.http;

import ipc.Broker;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.servers.Server;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import database.js.pools.ThreadPool;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ConcurrentHashMap;


public class HTTPServer extends Thread
{
  private final int port;
  private final boolean ssl;
  private final int threads;
  private final Server server;
  private final Config config;
  private final Broker broker;
  private final Logger logger;
  private final boolean admin;
  private final boolean embedded;
  private final boolean redirect;
  private final ThreadPool workers;
  private final HTTPServerType type;

  private final ConcurrentHashMap<SelectionKey,HTTPRequest> incomplete =
    new ConcurrentHashMap<SelectionKey,HTTPRequest>();


  public HTTPServer(Server server, HTTPServerType type, boolean embedded) throws Exception
  {
    this.type = type;
    this.server = server;
    this.redirect = false;
    this.embedded = embedded;
    this.broker = server.broker();
    this.config = server.config();
    this.logger = config.getLogger().http;
    this.threads = config.getTopology().threads();

    switch(type)
    {
      case ssl    : this.port = config.getHTTP().ssl();   ssl = true;  admin = false; break;
      case plain  : this.port = config.getHTTP().plain(); ssl = false; admin = false; break;
      case admin  : this.port = config.getHTTP().admin(); ssl = true;  admin = true;  break;
      default: port = -1; ssl = false; admin = false;
    }

    this.setDaemon(true);
    this.setName("HTTPServer("+type+")");
    this.workers = new ThreadPool(threads);
  }
  
  
  public Server server()
  {
    return(server);
  }
  
  
  public Logger logger()
  {
    return(logger);
  }


  public Config config()
  {
    return(config);
  }


  public Broker broker()
  {
    return(broker);
  }


  public boolean admin()
  {
    return(type == HTTPServerType.admin);
  }


  public boolean embedded()
  {
    return(embedded);
  }


  public HTTPRequest getIncomplete(SelectionKey key)
  {
    return(incomplete.remove(key));
  }
  
  
  public void setIncomplete(SelectionKey key, HTTPRequest request)
  {
    incomplete.put(key,request);
  }


  public void run()
  {
    if (port <= 0)
      return;

    HTTPBuffers buffers = new HTTPBuffers();
    logger.info("Starting HTTPServer("+type+")");

    try
    {
      int requests = 0;
      Selector selector = Selector.open();

      ServerSocketChannel server = ServerSocketChannel.open();

      server.configureBlocking(false);
      server.bind(new InetSocketAddress(port));

      server.register(selector,SelectionKey.OP_ACCEPT);

      while(true)
      {
        if (selector.select() <= 0)
          continue;

        Set<SelectionKey> selected = selector.selectedKeys();
        Iterator<SelectionKey> iterator = selected.iterator();

        if (++requests % 64 == 0 && incomplete.size() > 0)
          cleanout();

        while(iterator.hasNext())
        {
          SelectionKey key = iterator.next();
          iterator.remove();
          
          if (key.isAcceptable())
          {
            SocketChannel sch = server.accept();
            sch.configureBlocking(false);
            
            HTTPChannel helper = new HTTPChannel(config,buffers,sch,ssl,admin);
            boolean accept = helper.accept();
            
            if (accept)
            {
              buffers.done();
              sch.register(selector,SelectionKey.OP_READ,helper);
              logger.fine("Connection Accepted: "+sch.getLocalAddress());
            }
          }

          else if (key.isReadable())
          {
            try
            {
              SocketChannel sch = (SocketChannel) key.channel();
              HTTPChannel helper = (HTTPChannel) key.attachment();

              ByteBuffer buf = helper.read();

              if (buf == null)
              {
                sch.close();
                continue;
              }

              if (buf.remaining() == 0)
                continue;

              int read = buf.remaining();

              HTTPRequest request = getIncomplete(key);
              if (request == null) request = new HTTPRequest(helper);

              if (!request.add(buf.array(),read))
              {
                setIncomplete(key,request);
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

    logger.info("HTTPServer("+type+") stopped");
  }


  private void cleanout()
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
}
