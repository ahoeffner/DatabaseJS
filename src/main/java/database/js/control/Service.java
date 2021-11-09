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

package database.js.control;

import java.util.logging.Level;
import java.util.logging.Logger;


public class Service extends Thread
{
  private final int heartbeat;
  private final Logger logger;
  private final ILauncher launcher;


  @SuppressWarnings("unused")
  public static void main(String[] args) throws Exception
  {
    Service service = new Service();
  }


  public Service() throws Exception
  {
    this.launcher = Launcher.create();
    this.launcher.setConfig();

    this.logger = launcher.logger();
    this.heartbeat = launcher.heartbeat();

    this.setName("DatabaseJS Service");
    Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));

    this.start();
  }


  @Override
  public void run()
  {
    long started = 0;
    boolean stopped = false;

    try
    {
      logger.info("Starting database.js service");
      launcher.start();

      while(true)
      {
        synchronized(this) {this.wait(heartbeat);}
        if (started == 0) started = System.currentTimeMillis();

        if (launcher.stopped(started))
        {
          stopped = true;
          logger.info("database.js was stopped");
          break;
        }
      }

      if (!stopped)
      {
        logger.info("Stopping database.js service");
        launcher.stop(null);
      }
    }
    catch (Throwable e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
    }
  }


  private static class ShutdownHook extends Thread
  {
    private final Service service;

    ShutdownHook(Service service)
    {this.service = service;}

    @Override
    public void run()
    {
      synchronized(service)
       {service.notify();}
    }
  }
}