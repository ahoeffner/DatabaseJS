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

package database.js.cluster;

import ipc.Broker;
import ipc.Resource;

import java.nio.ByteBuffer;

public class Statistics
{
  private final long pid;
  private final long upd;
  private final long mem;
  private final long used;
  private static final int statlen = 4*Long.BYTES;
  
  
  Statistics()
  {
    this.upd = System.currentTimeMillis();
    this.pid = 0; //ProcessHandle.current().pid();
    this.mem = Runtime.getRuntime().totalMemory();
    this.used = (mem - Runtime.getRuntime().freeMemory());
  }
  
  
  public void write(Broker broker)
  {
    try
    {
      String id = ""+(broker.id()+1);
      Resource stat = broker.getResource(id);
      ByteBuffer data = ByteBuffer.allocate(statlen);
      
      data.putLong(pid);
      data.putLong(upd);
      data.putLong(mem);
      data.putLong(used);
      
      stat.acquire();
      stat.put(data.array());
    }
    catch (Exception e)
    {
      // TODO: Add catch code
      e.printStackTrace();
    }
  }
}
