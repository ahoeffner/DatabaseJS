package database.js.pools;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;


public class ThreadPool
{
  private int queue = 0;
  private final int threads;
  private static ExecutorService workers = null;


  public ThreadPool(int threads)
  {
    init(threads);
    this.threads = threads;
  }


  public int threads()
  {
    return(threads);
  }


  public synchronized void done()
  {
    queue--;
  }


  public boolean full()
  {
    return(queue > threads);
  }


  public synchronized int size()
  {
    return(queue);
  }


  private synchronized void init(int threads)
  {
    if (workers == null)
      workers = Executors.newFixedThreadPool(threads);
  }


  public static void shutdown()
  {
    if (workers != null)
      workers.shutdown();
  }


  public synchronized void submit(Runnable task)
  {
    queue++;
    workers.submit(task);
  }
}
