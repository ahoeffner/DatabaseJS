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

import database.js.servers.Server;


public class HTTPWaiterPool
{
  private static HTTPWaiter[] waiters;
  private static volatile short next = -1;
  private static final Object LOCK = new Object();


  public HTTPWaiterPool(Server server, boolean embedded, short threads) throws Exception
  {
    init(server,embedded,threads);
  }


  private static synchronized void init(Server server, boolean embedded, short threads) throws Exception
  {
    if (waiters == null)
    {
      waiters = new HTTPWaiter[threads];

      for (int i = 0; i < threads; i++)
        waiters[i] = new HTTPWaiter(server,i,embedded);
    }
  }


  public HTTPWaiter getWaiter()
  {
    synchronized(LOCK)
    {
      next = (short) (++next % waiters.length);
      return(waiters[next]);
    }
  }
  
  
  public HTTPWaiter[] getWaiters()
  {
    return(waiters);
  }
}