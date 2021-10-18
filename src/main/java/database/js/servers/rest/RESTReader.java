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
import java.util.logging.Level;
import java.util.logging.Logger;


class RESTReader extends Thread
{
  private final RESTConnection conn;


  RESTReader(RESTConnection conn) throws Exception
  {
    this.conn = conn;    
    this.setDaemon(true);
    this.setName("RESTReader");
  }
  
  
  @Override
  public void run()
  {
    int hsize = RESTComm.HEADER;
    Logger logger = conn.logger();

    try
    {
      SocketReader reader = new SocketReader(conn.reader());
      ArrayList<RESTComm> incoming = new ArrayList<RESTComm>();
      
      while(true)
      {
        byte[] head = reader.read(hsize);
        RESTComm http = new RESTComm(head);
        logger.finest(conn.parent()+" received data");
        
        int need = http.need();
        if (need > 0) http.add(reader.read(need));
        
        incoming.add(http);
        
        if (reader.empty())
        {
          conn.received(incoming);
          incoming = new ArrayList<RESTComm>();
        }
      }
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
      this.conn.failed();
    }
  }
}