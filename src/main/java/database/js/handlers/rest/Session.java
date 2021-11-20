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

import database.js.database.Database;
import java.util.concurrent.ConcurrentHashMap;


public class Session
{
  private final String guid;
  private final Database database;

  private int shared = 0;
  private long thread = 0;
  private boolean exclusive = false;
  private final Object LOCK = new Object();

  private final static ConcurrentHashMap<String,Session> sessions =
    new ConcurrentHashMap<String,Session>();


  public static Session get(String guid)
  {
    return(sessions.get(guid));
  }


  public Session(boolean tmp) throws Exception
  {
    if (tmp) this.guid = null;
    else this.guid = create();
    this.database = Database.getInstance();
  }


  private synchronized String create()
  {
    String guid = null;

    while(guid == null)
    {
      guid = new Guid().toString();
      if (sessions.get(guid) != null) guid = null;
    }

    sessions.put(guid,this);
    return(guid);
  }


  public Database database()
  {
    return(database);
  }


  public void lock(boolean exclusive) throws Exception
  {
    if (guid == null) return;
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
    if (guid == null) return;
    if (exclusive) release(true,0);
    if (shared > 0) release(false,shared);
  }


  public void release(boolean exclusive) throws Exception
  {
    int shared = 0;
    if (guid == null) return;
    if (!exclusive) shared = 1;
    this.release(exclusive,shared);
  }


  public void release(boolean exclusive, int shared) throws Exception
  {
    if (guid == null) return;
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
        exclusive = false;
      }
      else
      {
        this.shared -= shared;
      }

      LOCK.notifyAll();
    }
  }
}