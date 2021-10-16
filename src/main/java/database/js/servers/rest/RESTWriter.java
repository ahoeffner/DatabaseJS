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

import java.util.Map;
import java.util.ArrayList;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentHashMap;


class RESTWriter extends Thread
{
  private final RESTConnection conn;

  private final ConcurrentHashMap<Long,RESTComm> outgoing =
    new ConcurrentHashMap<Long,RESTComm>();


  RESTWriter(RESTConnection conn) throws Exception
  {
    this.conn = conn;    
    this.setDaemon(true);
    this.setName("RESTWriter");
  }
  
  
  void write(RESTComm call)
  {
    outgoing.put(call.id,call);
    synchronized (this) {this.notify();}
  }
  
  
  @Override
  public void run()
  {
    Logger logger = conn.logger();

    try
    {
      OutputStream writer = conn.writer();
      ArrayList<Long> sent = new ArrayList<Long>();
      
      while(true)
      {
        synchronized(this)
        {
          while(outgoing.size() == 0)
            this.wait();
        }
        
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        
        for(Map.Entry<Long,RESTComm> entry : outgoing.entrySet())
        {
          sent.add(entry.getKey());
          buffer.write(entry.getValue().request());
        }
        
        for(Long id : sent)
          outgoing.remove(id);
        
        byte[] data = buffer.toByteArray();
        
        logger.finest(conn.parent()+" sending data "+data.length);
        writer.write(data);
      }      
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
      this.conn.failed();
    }
  }
}
