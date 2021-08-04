import config.Config;

import instances.InstanceData;
import instances.ShareControl;

import java.util.Date;


public class DatabaseJS
{
  private static Config config = null;


  public static Config config()
  {
    return(config);
  }
  
  
  public static void main(String[] args) throws Exception
  {
    config = new Config(0);
    config.log.logger.fine("fine");
    config.log.logger.warning("warning");
    
    ShareControl ctrl = new ShareControl(config.lockfile);
    InstanceData data = ctrl.read(true);
    
    System.out.println(data);
    
    data.add("test",new Date().getTime());
    ctrl.write(data);
  }
}
