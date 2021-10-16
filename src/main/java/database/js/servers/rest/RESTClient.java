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

import java.util.ArrayList;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.cluster.MailBox;
import database.js.servers.Server;
import database.js.servers.http.HTTPChannel;
import java.util.concurrent.ConcurrentHashMap;


public class RESTClient implements RESTConnection
{
  private final short id;
  private final long started;
  
  private HTTPChannel rchannel;
  private HTTPChannel wchannel;
  private volatile boolean up = false;
  
  private final Config config;
  private final Logger logger;
  private final Server server;
  private final MailBox mailbox;
  private final RESTWriter writer;
  private final RESTReader reader;
  private final ConcurrentHashMap<Long,RESTComm> incoming;



  public RESTClient(Server server, short id, long started) throws Exception
  {
    this.id = id;
    this.started = started;
    
    this.server = server;
    this.config = server.config();
    this.writer = new RESTWriter(this);
    this.reader = new RESTReader(this);
    this.mailbox = new MailBox(config,id);
    this.logger = config.getLogger().rest;
    this.incoming = new ConcurrentHashMap<Long,RESTComm>();
  }
  
  
  public void init(HTTPChannel channel) throws Exception
  {
    channel.configureBlocking(true);
    
    if (this.wchannel == null) this.wchannel = channel;
    else                       this.rchannel = channel;
    
    if (this.rchannel != null)
    {
      this.up = true;
      this.writer.start();
      this.reader.start();
      logger.info("RESTClient ready");
    }
  }
  
  
  public byte[] send(byte[] data) throws Exception
  {
    long id = thread();
    int extend = mailbox.write(id,data);
    writer.write(new RESTComm(id,extend,data));
    
    RESTComm resp = null;
    
    synchronized(this)
    {
      while(true)
      {
        resp = incoming.get(id);
        logger.info("checking incoming id="+id+" resp="+resp);

        if (resp != null) break;
        if (!up) throw new Exception("Lost connection to RESTServer");
        
        this.wait();
      }
    }
    
    if (resp.extend < 0) data = resp.data();
    else data = mailbox.read(extend,resp.size);

    if (extend >= 0) mailbox.clear(extend);
    return(data);
  }
  
  
  private long thread()
  {
    return(Thread.currentThread().getId());
  }
  
  
  public short id()
  {
    return(id);
  }
  
  
  public long started()
  {
    return(started);
  }
  
  
  public boolean up()
  {
    return(up);
  }


  @Override
  public String parent()
  {
    return("RESTClient");
  }


  @Override
  public void failed()
  {
    this.up = false;
    server.deregister(this);
    logger.severe("RESTClient failed, bailing out");
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

  @Override
  public void received(ArrayList<RESTComm> calls)
  {
    logger.info("Client Received response");
    for(RESTComm call : calls)
      incoming.put(call.id,call);
    synchronized(this) {this.notifyAll();}
  }
}
