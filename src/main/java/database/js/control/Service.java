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

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;


public class Service extends Thread
{
  private final Logger logger;
  private final ILauncher launcher;


  @SuppressWarnings("unused")
  public static void main(String[] args) throws Exception
  {
    if (Launcher.version() < 13)
    {
      System.out.println("database.js requires java version > 13.0");
      System.exit(-1);
    }

    Service service = new Service();
  }


  public Service() throws Exception
  {
    this.launcher = Launcher.create();
    this.launcher.setConfig();
    
    PrintStream out = stdout();
    this.setName("DatabaseJS Service");

    System.setOut(out);
    System.setErr(out);

    this.logger = launcher.logger();
    Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));

    this.start();
  }


  private PrintStream stdout() throws Exception
  {
    String srvout = this.launcher.getControlOut();
    return(new PrintStream(new BufferedOutputStream(new FileOutputStream(srvout)), true));
  }


  @Override
  public void run()
  {
    try
    {
      logger.info("Starting database.js service");
      launcher.start();

      synchronized(this) {this.wait();}

      logger.info("Stopping database.js service");
      launcher.asyncStop();
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
