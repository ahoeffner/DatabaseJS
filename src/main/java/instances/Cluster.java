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
    long pid = ProcessHandle.current().pid();
    data.setInstance(pid,inst,config.http.plain,config.http.ssl,config.http.admin,0,0);
    this.manager = data.manageCluster(inst);
    shareddata.write(data);
  }
  
  
  public String status() throws Exception
  {
    String str = "";
    
    InstanceData data = shareddata.read(false);
    Hashtable<Integer,Instance> instances = data.getInstances(false);

    str += nl+nl+"Cluster manager : "+data.manager()+nl;
    str += "-----------------------------------------------------"+nl;
    str += "| Pid      | Id   | port | ssl  | adm  | ses  | max  |"+nl;
    str += "-----------------------------------------------------"+nl;

    for(Map.Entry<Integer,Instance> entry : instances.entrySet())
    {
      Instance instance = entry.getValue();

      String pid = String.format("%-8s",instance.pid);
      String ssl = String.format("%-4s",instance.ssl);
      String ses = String.format("%-4s",instance.ses);
      String max = String.format("%-4s",instance.max);
      String port = String.format("%-4s",instance.port);
      String admin = String.format("%-4s",instance.admin);
      
      String inst = String.format("%-4s",entry.getKey());
      
      str += "| " + pid + " | " + inst + " | " + port + " | " + ssl + " | " + admin + " | " + ses + " | " + max + " | "+nl;      
    }

    str += "-----------------------------------------------------"+nl;
    return(str);
  }
}
