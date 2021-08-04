package handlers;

import config.Config;


public class Test extends Thread
{
  private Config config;
  
  
  public Test(Config config)
  {
    this.config = config;
    this.setDaemon(true);
  }
  
  
  @Override
  public void run()
  {
  }
}
