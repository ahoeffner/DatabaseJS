package instances;

import config.Paths;


public class Cluster
{
  private boolean manager = false;
  public final SharedData shareddata;
  
  
  public Cluster() throws Exception
  {
    this.shareddata = new SharedData(Paths.sharefile);
  }


  public synchronized void manager(boolean manager)
  {
    this.manager = manager;
  }


  public synchronized boolean manager()
  {
    return(manager);
  }
}
