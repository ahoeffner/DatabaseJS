import config.Config;


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
  }
}
