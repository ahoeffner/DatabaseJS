package database.js.control;

import database.js.config.Config;


public class Service extends Thread
{
  private final Config config;


  @SuppressWarnings("unused")
  public static void main(String[] args) throws Exception
  {
    Service service = new Service();
  }


  public Service() throws Exception
  {
    this.start();
    this.config = new Config();
    Launcher.main(new String[] {"-s","start"});
    Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
  }
  
  
  @Override
  public void run()
  {
    try 
    {
      synchronized(this) {this.wait();}
      Launcher.main(new String[] {"-s","stop"});
    }
    catch (Exception e) {;}
  }
  
  
  private static class ShutdownHook extends Thread
  {
    private final Service service;
    
    ShutdownHook(Service service)
    {this.service = service;}
    
    @Override
    public void run()
    {
      synchronized(service)
       {service.notify();}
    }
  }
}
