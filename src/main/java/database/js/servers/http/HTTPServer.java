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
import java.util.Iterator;
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
  private final HTTPWaiter[] waiters;


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
    
    this.waiters = new HTTPWaiter[] {new HTTPWaiter(server,0,embedded)};

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


  Server server()
  {
    return(server);
  }


  Logger logger()
  {
    return(logger);
  }


  Config config()
  {
    return(config);
  }


  boolean admin()
  {
    return(type == HTTPServerType.admin);
  }


  boolean embedded()
  {
    return(embedded);
  }


  ThreadPool workers()
  {
    return(workers);
  }
  

  // Assign a waiter for the client  
  void assign(SelectionKey key, HTTPChannel client)
  {
    key.cancel();
    System.out.println("Assign waiter");
    HTTPWaiter waiter = waiters[0];    
    try {waiter.addClient(client);}
    catch (Exception e) {logger.log(Level.SEVERE,e.getMessage(),e);}
  }


  private void select() throws Exception
  {
    while(selector.select() == 0)
      logger.warning("selector woke up empty handed");
  }


  public void run()
  {
    if (port <= 0)
      return;

    logger.info("Starting HTTPServer("+type+")");

    try
    {
      ServerSocketChannel server = ServerSocketChannel.open();

      server.configureBlocking(false);
      server.bind(new InetSocketAddress(port));
      server.register(selector,SelectionKey.OP_ACCEPT);

      while(true)
      {
        select();

        Set<SelectionKey> selected = selector.selectedKeys();
        Iterator<SelectionKey> iterator = selected.iterator();

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
              workers.submit(ses);
            }
            else
            {
              // Overkill to use threadpool
              key.cancel();
              HTTPChannel client = new HTTPChannel(this.server,workers,channel,ssl,admin);
              if (client.accept()) this.assign(key,client);
            }
          }
          else
          {
            logger.warning("Key is not acceptable");
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
}
