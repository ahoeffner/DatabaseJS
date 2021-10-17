package database.js.servers;

import database.js.config.Config;
import database.js.servers.rest.RESTClient;


class LoadBalancer
{
  private int last = 0;
  private int requests = 0;

  private final int htsrvs;
  private final int threads;
  private final int servers;
  private final int persist;
  private final Config config;
  private final RESTClient[] workers;
  
  
  LoadBalancer(Config config) throws Exception
  {
    this.config = config;
    this.servers = config.getTopology().servers();
    this.threads = config.getTopology().workers();
    
    short htsrvs = 1;
    if (config.getTopology().hotstandby()) htsrvs++;
    
    int persist = threads/8;
    if (persist < 1) persist = 1;
    if (persist > 16) persist = 16;

    this.htsrvs = htsrvs;
    this.persist = persist;
    
    this.workers = new RESTClient[servers];
  }


  public RESTClient worker(short id)
  {
    return(workers[id-this.htsrvs]);
  }
  
  
  public RESTClient worker() throws Exception
  {
    if (++requests < persist)
    {
      if (workers[last] != null && workers[last].up())
        return(workers[last]);
    }
    
    requests = 0;
    int tries = 0;
    
    while(++tries < 32)
    {
      for (int i = 0; i < workers.length; i++)
      {
        last = (++last) % workers.length;
        
        if (workers[last] != null && workers[last].up())
          return(workers[last]);          
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
}
