package instances;

import config.Config;
import config.Paths;

import instances.InstanceData.Instance;

import java.util.Date;
import java.util.Hashtable;
import java.util.Map;


public class Cluster extends Thread
{
  private final int inst;
  private boolean manager;
  private final Config config;
  private final SharedData shareddata;
  private final static String nl = System.lineSeparator();


  public Cluster(Config config, int inst) throws Exception
  {
    this.inst = inst;
    this.config = config;
    this.shareddata = new SharedData(Paths.sharefile);
    
    this.setDaemon(true);
  }
  
  
  public boolean manager()
  {
    return(manager);
  }


  public void register() throws Exception
  {
    long time = new Date().getTime();
    InstanceData data = shareddata.read(true);
    long pid = ProcessHandle.current().pid();
    data.setInstance(pid,time,inst,config.http.plain,config.http.ssl,config.http.admin,0,0);
    this.manager = data.manageCluster(inst, config.cluster.instances);
    shareddata.write(data);
    
    if (manager) config.clslog();
    this.start();
  }
  
  
  @Override
  public void run()
  {
    while(true)
    {
      try
      {
        maintain();
        sleep(config.cluster.check);
      }
      catch (Exception e)
      {
        config.log.exception(e);
      }
    }
  }


  public void deregister() throws Exception
  {
    InstanceData data = shareddata.read(true,true);
    data.removeInstance(inst);
    shareddata.write(data);
  }
  
  
  public void maintain() throws Exception
  {
    long time = new Date().getTime();
    int check = config.cluster.check;
    InstanceCtrl ctrl = new InstanceCtrl();
    
    InstanceData data = shareddata.read(false);
    Hashtable<Integer,Instance> instances = data.getInstances(false);

    int servers = data.servers();
    if (servers < 0) servers = config.cluster.instances;
    
    if (manager)
    {
      for (int i = 0; i < servers; i++)
      {
        boolean start = true;
        if (i == this.inst) continue;
        
        Instance inst = instances.get(i);
        
        if (inst != null)
        {
          int age = (int) (time - inst.time);
          if (age > 2 * check) System.out.println(i+" dead ?");
          if (age <= check) start = false;
        }
        
        if (start) ctrl.start(i,config);
      }
    }
  }


  public String status() throws Exception
  {
    String str = "";
    long time = new Date().getTime();

    InstanceData data = shareddata.read(false);
    Hashtable<Integer,Instance> instances = data.getInstances(false);

    str += nl+nl+"\tCluster manager : "+data.manager()+ ", servers : " + data.servers() +nl;
    str += "----------------------------------------------------------"+nl;
    str += "|    Pid   |  Id | port |  ssl |  adm |  ses |  max | age |"+nl;
    str += "----------------------------------------------------------"+nl;

    for(Map.Entry<Integer,Instance> entry : instances.entrySet())
    {
      Instance instance = entry.getValue();

      String pid = String.format("%8s",instance.pid);
      String ssl = String.format("%4s",instance.ssl);
      String ses = String.format("%4s",instance.ses);
      String max = String.format("%4s",instance.max);
      String port = String.format("%4s",instance.port);
      String admin = String.format("%4s",instance.admin);

      String age = String.format("%3s",(time-instance.time)/1000);
      String inst = String.format("%3s",entry.getKey());

      str += "| " + pid + " | " + inst + " | " + port + " | " + ssl + " | " + admin + " | " + ses + " | " + max + " | " + age + " | "+nl;
    }

    str += "----------------------------------------------------------"+nl;
    return(str);
  }
}
