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

package database.js.handlers.rest;

import java.util.logging.Logger;


public class SessionLock
{
  private int shared = 0;
  private long thread = 0;
  private boolean exclusive = false;
  private final Object LOCK = new Object();
  private final static Logger logger = Logger.getLogger("rest");


  public SessionLock()
  {
  }


  public void lock(boolean exclusive) throws Exception
  {
    long thread = Thread.currentThread().getId();

    synchronized(LOCK)
    {
      boolean owner = this.thread == thread;

      while(!owner && this.exclusive)
        LOCK.wait();

      if (exclusive)
      {
        while(!owner && this.shared > 0)
          LOCK.wait();

        this.thread = thread;
        this.exclusive = true;
      }
      else
      {
        while(!owner && this.exclusive)
          LOCK.wait();

        this.shared++;
      }
    }
  }


  public void releaseAll(boolean exclusive, int shared) throws Exception
  {
    if (exclusive) release(true,0);
    if (shared > 0) release(false,shared);
  }


  public void release(boolean exclusive) throws Exception
  {
    int shared = 0;
    if (!exclusive) shared = 1;
    this.release(exclusive,shared);
  }


  public void release(boolean exclusive, int shared) throws Exception
  {
    long thread = Thread.currentThread().getId();

    synchronized(LOCK)
    {
      if (exclusive && this.thread != thread)
        throw new Exception("Thread "+thread+" cannot release session lock owned by "+this.thread);

      if (exclusive && !this.exclusive)
        throw new Exception("Cannot release exclusive lock, when only shared obtained");

      if (!exclusive && this.shared < shared)
        throw new Exception("Cannot release "+shared+" shared lock(s) not obtained");

      if (exclusive)
      {
        this.thread = 0;
        this.exclusive = false;
      }
      else
      {
        this.shared -= shared;
      }

      LOCK.notifyAll();
    }
  }
}