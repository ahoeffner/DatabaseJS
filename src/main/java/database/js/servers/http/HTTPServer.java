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
  private final Selector selector;
  private final ThreadPool workers;
  private final HTTPServerType type;
  private final SSLHandshakeQueue queue;

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
    this.selector = Selector.open();
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

    if (!ssl) queue = null;
    else      queue = new SSLHandshakeQueue(selector);
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


  ThreadPool workers()
  {
    return(workers);
  }


  HTTPRequest getIncomplete(SelectionKey key)
  {
    return(incomplete.remove(key));
  }


  void setIncomplete(SelectionKey key, HTTPRequest request)
  {
    incomplete.put(key,request);
  }


  SSLHandshakeQueue queue()
  {
    return(queue);
  }


  private int select() throws Exception
  {
    int ready = 0;

    if (queue == null)
      return(selector.select());

    while(ready == 0)
    {
      //if (!queue.isWaiting()) ready = selector.select();
      //else                    ready = selector.select(5);

      ready = selector.select();
      System.out.println("running "+ready);

      while(queue.next())
      {
        SSLHandshake hndshk = queue.getSession();

        HTTPChannel helper = hndshk.helper();
        SocketChannel channel = hndshk.channel();

        //selector.wakeup();
        channel.register(selector,SelectionKey.OP_READ,helper);
        logger.fine("Connection Accepted: "+channel.getLocalAddress());
      }
    }

    return(ready);
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
      long lmsg = System.currentTimeMillis();
      ServerSocketChannel server = ServerSocketChannel.open();

      server.configureBlocking(false);
      server.bind(new InetSocketAddress(port));
      server.register(selector,SelectionKey.OP_ACCEPT);

      while(true)
      {
        if (select() <= 0)
          continue;

        Set<SelectionKey> selected = selector.selectedKeys();
        Iterator<SelectionKey> iterator = selected.iterator();

        if (++requests % 1024 == 0 && incomplete.size() > 0)
          cleanout();

        if (workers.full() && (System.currentTimeMillis() - lmsg) > 5000)
        {
          lmsg = System.currentTimeMillis();
          logger.info("clients="+selector.keys().size()+" threads="+workers.threads()+" queue="+workers.size());
        }

        while(iterator.hasNext())
        {
          SelectionKey key = iterator.next();
          iterator.remove();

          if (key.isAcceptable())
          {
            SocketChannel channel = server.accept();
            channel.configureBlocking(false);

            if (ssl)
            {
              // Don't block while handshaking
              SSLHandshake ses = new SSLHandshake(this,key,channel,admin);
              queue.register(ses);
              workers.submit(ses);
            }
            else
            {
              // Overkill to use threadpool
              HTTPChannel helper = new HTTPChannel(config,channel,ssl,admin);
              boolean accept = helper.accept();

              if (accept)
              {
                buffers.done();
                channel.register(selector,SelectionKey.OP_READ,helper);
                logger.fine("Connection Accepted: "+channel.getLocalAddress());
              }
            }
          }

          else if (key.isReadable())
          {
            try
            {
              HTTPChannel helper = (HTTPChannel) key.attachment();
              SocketChannel channel = (SocketChannel) key.channel();

              ByteBuffer buf = helper.read();

              if (buf == null)
              {
                channel.close();
                getIncomplete(key);
                continue;
              }

              int read = buf.remaining();

              if (read > 0)
              {
                HTTPRequest request = getIncomplete(key);
                if (request == null) request = new HTTPRequest(helper);
                workers.submit(new HTTPWorker(this,key,request.add(buf.array(),read)));
              }
            }
            catch (Exception e)
            {
              HTTPRequest request = getIncomplete(key);

              if (request != null)
                logger.severe("Could not read from socket trying to complete request");

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
