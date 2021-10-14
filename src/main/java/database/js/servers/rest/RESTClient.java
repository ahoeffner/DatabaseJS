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

import java.net.Socket;
import java.nio.ByteBuffer;
import java.io.OutputStream;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.servers.Server;
import database.js.cluster.MailBox;
import database.js.servers.http.HTTPChannel;


public class RESTClient
{
  private short id = -1;
  private long started = -1;
  private OutputStream writer;
  
  private final Config config;
  private final Logger logger;
  private final Server server;
  private final Socket socket;
  private final MailBox mailbox;
  private final HTTPChannel channel;
  private final ByteBuffer buffer = ByteBuffer.allocate(17);
    
  private final static byte STOP = -1;
  private final static byte SHMMEM = 1;
  private final static byte STREAM = 2;



  public RESTClient(HTTPChannel channel, short id) throws Exception
  {
    this.id = id;
    this.channel = channel;
    this.socket = channel.socket();
    this.server = channel.server();
    this.config = channel.config();
    this.mailbox = new MailBox(config,id);
    this.started = System.currentTimeMillis();
    this.logger = server.config().getLogger().rest;
  }
  
  
  public short id()
  {
    return(id);
  }
  
  
  Logger logger()
  {
    return(logger);
  }
  
  
  public long started()
  {
    return(started);
  }
  
  
  public void init() throws Exception
  {
    channel.configureBlocking(true);
    this.writer = socket.getOutputStream();
  }
  
  
  public void send(byte[] data) throws Exception
  {
    long id = thread();
    int extend = mailbox.write(id,data);
    writer.write(message(id,extend,data));
    if (extend == -1) writer.write(data);
  }
  
  
  private byte[] message(long id, int extend, byte[] data)
  {
    byte type = SHMMEM;
    if (extend < 0) type = STREAM;
    
    buffer.clear();
    buffer.put(type);
    buffer.putLong(id);
    buffer.putInt(extend);
    buffer.putInt(data.length);
    logger.info("Sending, data placed in extend "+extend);
    
    return(buffer.array());
  }
  
  
  private long thread()
  {
    return(Thread.currentThread().getId());
  }
}
