package instances;

import config.Config;
import config.Paths;

import instances.InstanceData.Instance;

import java.util.Hashtable;
import java.util.Map;


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
    this.manager = data.manageCluster(inst);
    shareddata.write(data);
  }
  
  
  public String status() throws Exception
  {
    String str = "";
    
    InstanceData data = shareddata.read(false);
    Hashtable<Integer,Instance> instances = data.getInstances(false);

    str += "Cluster Manager Instance : "+data.manager();

    for(Map.Entry<Integer,Instance> entry : instances.entrySet())
      str += "Instance" + entry.getKey() + " : " + entry.getValue() + "\n";

    return(str);
  }
}
