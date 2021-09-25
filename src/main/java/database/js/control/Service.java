package database.js.control;

import ipc.Broker;
import ipc.Message;
import ipc.Listener;
import java.util.ArrayList;
import database.js.config.Config;


public class Service extends Thread implements Listener
{
  private final Config config;
  private final Broker broker;


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
    this.broker = new Broker(config.getIPConfig(),this);
    Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
  }


  @Override
  public void onServerUp(short id)
  {
    System.out.println("Server "+id+" started");
  }

  @Override
  public void onServerDown(short id)
  {
    System.out.println("Server "+id+" stopped");
  }

  @Override
  public void onNewManager(short id)
  {
    System.out.println("HTTPServer switched");
  }

  @Override
  public void onMessage(ArrayList<Message> arrayList)
  {
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
