package database.js.pools;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;


public class ThreadPool
{
  private static ExecutorService workers = null;
  
  
  public ThreadPool(int threads)
  {
    init(threads);
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
  
  
  public void submit(Runnable task)
  {
    workers.submit(task);
  }
}
