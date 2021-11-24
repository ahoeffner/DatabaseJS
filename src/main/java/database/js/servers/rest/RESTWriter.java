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
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.ByteArrayOutputStream;


class RESTWriter extends Thread
{
  private final RESTConnection conn;

  private ArrayList<RESTComm> outgoing =
    new ArrayList<RESTComm>();


  RESTWriter(RESTConnection conn) throws Exception
  {
    this.conn = conn;
    this.setDaemon(true);
    this.setName("RESTWriter");
  }


  void write(RESTComm call)
  {
    synchronized (this)
    {
      outgoing.add(call);
      this.notify();
    }
  }


  @Override
  public void run()
  {
    Logger logger = conn.logger();
    ArrayList<RESTComm> outgoing = null;

    try
    {
      OutputStream writer = conn.writer();

      while(true)
      {
        synchronized(this)
        {
          while(this.outgoing.size() == 0)
            this.wait();

          outgoing = this.outgoing;
          this.outgoing = new ArrayList<RESTComm>();
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream(4192);

        for(RESTComm entry : outgoing)
          buffer.write(entry.bytes());

        byte[] data = buffer.toByteArray();

        logger.finest(conn.parent()+" sending "+data.length+" bytes");

        writer.write(data);
        writer.flush();
      }
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
      this.conn.failed();
    }
  }
}