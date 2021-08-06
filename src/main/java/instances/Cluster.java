package instances;

import config.Config;
import config.Paths;


public class Cluster
{
  private final int inst;
  private boolean manager;
  private final Config config;
  public final SharedData shareddata;
  
  
  public Cluster(Config config, int inst) throws Exception
  {
    this.inst = inst;
    this.config = config;
    this.shareddata = new SharedData(Paths.sharefile);
  }
  
  
  public void register() throws Exception
  {
    InstanceData data = shareddata.read(true);
    data.setInstance(inst,config.http.admin);
    shareddata.write(data);
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
