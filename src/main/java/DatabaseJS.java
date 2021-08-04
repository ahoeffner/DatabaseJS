import config.Config;
import instances.SharedData;


public class DatabaseJS
{
  private static Config config = null;
  private static SharedData shareddata = null;
  
  
  public static void main(String[] args) throws Exception
  {
    config = new Config(0);
    shareddata = new SharedData(config.lockfile);
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
