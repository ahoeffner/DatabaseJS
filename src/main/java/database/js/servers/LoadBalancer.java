package database.js.servers;

import database.js.config.Config;
import database.js.servers.rest.RESTClient;


class LoadBalancer
{
  private int last = -1;
  private final int htsrvs;
  private final int threads;
  private final int servers;
  private final Config config;
  private final RESTClient[] workers;
  
  
  LoadBalancer(Config config) throws Exception
  {
    this.config = config;
    this.servers = config.getTopology().servers();
    this.threads = config.getTopology().workers();
    
    short htsrvs = 1;
    if (config.getTopology().hotstandby()) htsrvs++;
    
    this.htsrvs = htsrvs;
    this.workers = new RESTClient[servers];
  }


  public RESTClient worker(short id)
  {
    return(workers[id-this.htsrvs]);
  }
  
  
  public RESTClient worker() throws Exception
  {
    int tries = 0;
    int next = next();
    
    while(++tries < 32)
    {
      for (int i = 0; i < workers.length; i++)
      {
        if (workers[next] != null && workers[next].up())
          return(workers[next]); 
        
        next = ++next % workers.length;        
      }
      
      Thread.sleep(250);
    }
    
    throw new Exception("No available RESTEngines, bailing out");
  }
  
  
  public void register(RESTClient client)
  {
    workers[client.id()-this.htsrvs] = client;
  }
  
  
  public void deregister(RESTClient client)
  {
    workers[client.id()-this.htsrvs] = null;
  }
  
  
  private synchronized int next()
  {
    last = (++last) % workers.length;
    return(last);
  }
}
