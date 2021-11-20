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

import java.util.logging.Level;
import java.util.logging.Logger;


public class HTTPReaper extends Thread
{
  private final int timeout;
  private final Logger logger;
  private final HTTPWaiterPool waiters;
  private static HTTPReaper reaper = null;
  
  
  synchronized static void start(Logger logger, HTTPWaiterPool waiters, int timeout) throws Exception
  {
    if (reaper == null)
      reaper = new HTTPReaper(logger,waiters,timeout);
  }
  
  
  private HTTPReaper(Logger logger, HTTPWaiterPool waiters, int timeout)
  {
    this.logger = logger;
    this.waiters = waiters;
    this.timeout = timeout;

    this.setDaemon(true);
    this.setName("HTTPReaper");
    
    this.start();
  }
  
  
  public void run()
  {
    HTTPWaiter[] waiters =
      this.waiters.getWaiters();

    try
    {
      while(true)
      {
        sleep(timeout);
        
        for(HTTPWaiter waiter : waiters)
          waiter.cleanout();
      }
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
    }
  }
}
