import config.Config;

import config.Paths;
import instances.SharedData;
import instances.InstanceData;


public class DatabaseJS
{
  private static Config config = null;
  private static SharedData shareddata = null;
  
  
  public static void main(String[] args) throws Exception
  {
    int inst = 0;
    config = new Config(0,null);

    // Start

    //new Environment();
    
    shareddata = new SharedData(Paths.sharefile);
    Runtime.getRuntime().addShutdownHook(new ShutdownHook(inst,shareddata));
    
    InstanceData data = shareddata.read(true);
    data.setInstance(inst);
    shareddata.write(data);    
  }


  public static Config config()
  {
    return(config);
  }


  public static SharedData shareddata()
  {
    return(shareddata);
  }
  
  
  private static class ShutdownHook extends Thread
  {
    private final int inst;
    private final SharedData shareddata;

    
    
    ShutdownHook(int inst, SharedData shareddata)
    {
      this.inst = inst;
      this.shareddata = shareddata;
    }
    
    
    public void run()
    {
      try
      {
        InstanceData data = shareddata.read(true);
        data.removeInstance(inst);
        shareddata.write(data);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }
}
