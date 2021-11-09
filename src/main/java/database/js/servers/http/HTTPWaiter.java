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

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.servers.Server;
import java.nio.channels.Selector;
import database.js.pools.ThreadPool;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;


class HTTPWaiter extends Thread
{
  private final int id;
  private final Server server;
  private final Config config;
  private final Logger logger;
  private final boolean embedded;
  private final Selector selector;
  private final ThreadPool workers;

  private final ArrayList<HTTPChannel> queue =
    new ArrayList<HTTPChannel>();

  private final ConcurrentHashMap<SelectionKey,HTTPRequest> incomplete =
    new ConcurrentHashMap<SelectionKey,HTTPRequest>();


  HTTPWaiter(Server server, int id, boolean embedded) throws Exception
  {
    this.id = id;
    this.server = server;
    this.embedded = embedded;
    this.config = server.config();
    this.selector = Selector.open();
    this.logger = config.getLogger().http;

    this.setDaemon(true);
    this.setName("HTTPWaiter("+id+")");
    this.workers = new ThreadPool(config.getTopology().workers());

    this.start();
  }


  Server server()
  {
    return(server);
  }


  void unlist(SelectionKey key)
  {
    key.cancel();
  }


  void addClient(HTTPChannel client) throws Exception
  {
    synchronized(this)
    {queue.add(client);}
    selector.wakeup();
  }


  private void select() throws Exception
  {
    int ready = 0;

    while(ready == 0)
    {
      boolean add = false;

      synchronized(this)
      {
        for(HTTPChannel client : queue)
        {
          add = true;
          client.channel().register(selector,SelectionKey.OP_READ,client);
        }

        queue.clear();
      }

      if (!add) ready = selector.select();
      else      ready = selector.selectNow();
    }
  }


  @Override
  public void run()
  {
    int requests = 0;
    long lmsg = System.currentTimeMillis();

    while(true)
    {
      try
      {
        select();

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

          if (key.isReadable())
          {
            HTTPChannel client = (HTTPChannel) key.attachment();
            SocketChannel channel = (SocketChannel) key.channel();

            ByteBuffer buf = client.read();

            if (buf == null)
            {
              key.cancel();
              channel.close();
              continue;
            }

            int read = buf.remaining();

            if (read > 0)
            {
              boolean done = false;
              HTTPRequest request = incomplete.remove(key);
              if (request == null) request = new HTTPRequest(this,client,key);

              try
              {
                if (!request.add(buf)) incomplete.put(key,request);
                else done = true;
              }
              catch (Exception e)
              {
                logger.log(Level.SEVERE,e.getMessage(),e);
                error(channel,400,false);
                continue;
              }

              try
              {
                if (done)
                  workers.submit(new HTTPWorker(workers,request));
              }
              catch (Exception e)
              {
                logger.log(Level.SEVERE,e.getMessage(),e);
                error(channel,500,false);
                continue;
              }
            }
          }
          else
          {
            logger.warning("Key is not readable");
          }
        }
      }
      catch (Exception e)
      {
        logger.log(Level.SEVERE,e.getMessage(),e);
      }
    }
  }


  private void cleanout()
  {
    ArrayList<SelectionKey> cancelled = new ArrayList<SelectionKey>();

    for(Map.Entry<SelectionKey,HTTPRequest> entry : incomplete.entrySet())
      if (entry.getValue().cancelled()) cancelled.add(entry.getKey());

    for(SelectionKey key : cancelled)
    {
      incomplete.remove(key);
      logger.info("Removing incomplete request");

      try
      {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        SocketChannel rsp = (SocketChannel) key.channel();
        buf.put(err400(false));
        buf.position(0);
        rsp.write(buf);
        rsp.close();
      }
      catch (Exception e) {;}
    }
  }


  public static final String EOL = "\r\n";


  public static void error(SocketChannel channel, int code, boolean rest)
  {
    ByteBuffer buf = ByteBuffer.allocate(1024);

    switch(code)
    {
      case 400:
        buf.put(err400(rest));
        break;

      case 500:
        buf.put(err500(rest));
        break;

      default:
        buf.put(err500(rest));
        break;
    }

    try
    {
      buf.position(0);
      channel.write(buf);
      channel.close();
    }
    catch (Exception e)
    {
      try {channel.close();}
      catch (Exception c) {;}
    }
  }


  private static byte[] err400(boolean rest)
  {
    String msg = "<b>Bad Request</b>";
    if (rest) msg = "{\"status\": \"failed\", \"message\": \"Bad Request\"}";

    String page = "HTTP/1.1 400 Bad Request" + EOL +
                  "Content-Type: text/html" + EOL +
                  "Content-Length: "+msg.length() + EOL + EOL + msg;

    return(page.getBytes());
  }


  private static byte[] err500(boolean rest)
  {
    String msg = "<b>Internal Server Error</b>";
    if (rest) msg = "{\"status\": \"failed\", \"message\": \"Internal Server Error\"}";

    String page = "HTTP/1.1 500 Internal Server Error" + EOL +
                  "Content-Type: text/html" + EOL +
                  "Content-Length: "+msg.length() + EOL + EOL + msg;

    return(page.getBytes());
  }
}