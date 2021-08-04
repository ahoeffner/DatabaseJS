import config.Config;

import instances.InstanceData;
import instances.SharedData;


public class DatabaseJS
{
  private static Config config = null;
  private static SharedData shareddata = null;
  
  
  public static void main(String[] args) throws Exception
  {
    config = new Config(0);
    shareddata = new SharedData(config.lockfile);
    
    long time = System.nanoTime();
    
    InstanceData data = shareddata.read(true);
    System.out.println(data);
    data.setFile("test",124L);
    data.setInstance(10);
    shareddata.write(data);
    
    time = System.nanoTime() - time;
    System.out.println("elapsed: "+1.0*time/1000000000);
  }


  public static Config config()
  {
    return(config);
  }


  public static SharedData shareddata()
  {
    return(shareddata);
  }
}
