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

package database.js.pools;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;


public class ThreadPool
{
  private final int threads;
  private static ExecutorService workers = null;

  private static int queue = 0;
  private static final Object LOCK = new Object();


  public ThreadPool(int threads)
  {
    init(threads);
    this.threads = threads;
  }


  public int threads()
  {
    return(threads);
  }


  public void done()
  {
    synchronized(LOCK)
     {queue--;}
  }


  public boolean full()
  {
    return(queue > threads);
  }


  public int size()
  {
    synchronized(LOCK)
    {return(queue);}
  }


  private synchronized void init(int threads)
  {
    if (workers == null)
      workers = Executors.newFixedThreadPool(threads);
  }


  public static void shutdown()
  {
    if (workers != null)
      workers.shutdownNow();
  }


  public void submit(Runnable task)
  {
    synchronized(LOCK)
      {queue++;}

    workers.submit(task);
  }
}