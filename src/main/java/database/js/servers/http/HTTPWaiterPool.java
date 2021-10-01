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
}
