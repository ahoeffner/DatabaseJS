package instances;

import config.Paths;
import config.Config;
import server.Server;
import java.util.Map;
import java.util.Date;
import java.util.Hashtable;
import instances.InstanceData.Instance;


public class Cluster extends Thread
{
  private final int inst;
  private boolean manager;
  private final Config config;
  private final Server server;
  private final SharedData shareddata;
  private final static String nl = System.lineSeparator();


  public Cluster(Config config, Server server, int inst) throws Exception
  {
    this.inst = inst;
    this.config = config;
    this.server = server;
    this.shareddata = new SharedData(Paths.sharefile);
    
    this.setDaemon(true);
  }
  
  
  public boolean manager()
  {
    return(manager);
  }


  public void register() throws Exception
  {
    int pssl = config.http.ssl + this.inst;
    int pplain = config.http.plain + this.inst;
    int padmin = config.http.admin + this.inst;
    
    Stats stats = new Stats(0,0);

    long time = new Date().getTime();
    InstanceData data = shareddata.read(true);
    long pid = ProcessHandle.current().pid();
    data.setInstance(pid,time,inst,pplain,pssl,padmin,stats);
    this.manager = data.cluster(inst,config.cluster.instances,config.http.version);
    shareddata.write(data);
    
    if (manager) config.clslog();
    this.start();
  }
  
  
  @Override
  public void run()
  {
    int wt = config.cluster.check;
    
    while(true)
    {
      try
      {
        maintain();
        sleep(wt);
        update();
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
  
  
  private void update() throws Exception
  {
    int pssl = config.http.ssl + this.inst;
    int pplain = config.http.plain + this.inst;
    int padmin = config.http.admin + this.inst;
    
    Stats stats = new Stats(0,0);
    long time = new Date().getTime();
    long pid = ProcessHandle.current().pid();
    InstanceData data = shareddata.read(true);
    
    Instance inst = data.getInstances(true).get(this.inst);
    if (inst == null) data.setInstance(pid,time,this.inst,pplain,pssl,padmin,stats);
    else              data.setInstance(pid,time,this.inst,inst.port,inst.ssl,inst.admin,stats);
    
    shareddata.write(data);
  }
  
  
  private void maintain() throws Exception
  {
    Control ctrl = new Control();
    long time = new Date().getTime();
    int check = config.cluster.check;
    
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
        
        if (inst != null && (time - inst.time) <= check)
          start = false;
        
        if (start) ctrl.start(i,config);
      }
    }
    else
    {
      Instance inst = instances.get(data.manager());

      if (inst == null || (time - inst.time) > 1.5 * check)
         if (rewire()) maintain();
    }
  }
  
  
  private boolean rewire() throws Exception
  {
    boolean changed = false;
    long time = new Date().getTime();
    int check = config.cluster.check;
    
    InstanceData data = shareddata.read(true);
    Hashtable<Integer,Instance> instances = data.getInstances(true);

    Instance inst = instances.get(data.manager());    

    if (inst == null || (time - inst.time) > 2 * check)
    {
      config.clslog();
      changed = true;
      this.manager = true;
      data.setManager(this.inst);
      config.log.cluster.info("Manager changed to "+this.inst);
    }

    shareddata.write(data);
    
    return(changed);
  }


  public String status() throws Exception
  {
    String str = "";
    int mb = 1024 * 1024;
    long time = new Date().getTime();

    InstanceData data = shareddata.read(false);
    Hashtable<Integer,Instance> instances = data.getInstances(false);

    str += nl+nl+"Cluster manager : "+data.manager()+ ", servers : " + data.servers() + ", version : " + data.version() +nl;
    str += "-------------------------------------------------------------------------"+nl;
    str += "|    Pid   |  Id | port |  ssl |  adm |sessions c/m | memory t/u  | age |"+nl;
    str += "-------------------------------------------------------------------------"+nl;

    for(Map.Entry<Integer,Instance> entry : instances.entrySet())
    {
      Instance instance = entry.getValue();
      
      Stats stats = instance.stats;

      String pid = String.format("%8s",instance.pid);
      String ssl = String.format("%4s",instance.ssl);
      String ses = String.format("%4s",instance.stats.ses);
      String max = String.format("%-4s",instance.stats.max);
      String mem = String.format("%4s",instance.stats.mem/mb);
      String used = String.format("%-4s",instance.stats.used/mb);

      String port = String.format("%4s",instance.port);
      String admin = String.format("%4s",instance.admin);

      String age = String.format("%3s",(time-instance.time)/1000);
      String inst = String.format("%3s",entry.getKey());

      str += "| " + pid + " | " + inst + " | " + port + " | " + ssl + " | " + admin + " | " + ses + " - " + max + " | " + mem + " - " + used + " | " + age + " | "+nl;
    }

    str += "-------------------------------------------------------------------------"+nl;
    return(str);
  }
}
