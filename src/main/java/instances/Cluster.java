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
  private final static String nl = System.lineSeparator();
  
  
  public Cluster(Config config, int inst) throws Exception
  {
    this.inst = inst;
    this.config = config;
    this.shareddata = new SharedData(Paths.sharefile);
  }
  
  
  public void register() throws Exception
  {
    InstanceData data = shareddata.read(true);
    data.setInstance(inst,config.http.plain,config.http.ssl,config.http.admin,0,0);
    this.manager = data.manageCluster(inst);
    shareddata.write(data);
  }
  
  
  public String status() throws Exception
  {
    String str = "";
    
    InstanceData data = shareddata.read(false);
    Hashtable<Integer,Instance> instances = data.getInstances(false);

    str += "Cluster Manager Instance : "+data.manager()+nl+nl;
    str += "Id ";

    for(Map.Entry<Integer,Instance> entry : instances.entrySet())
    {
      Instance instance = entry.getValue();
      
      String inst = String.format("%-4s",entry.getKey());
      str += nl + inst + " : " + entry.getValue() + "\n";      
    }

    return(str);
  }
}
