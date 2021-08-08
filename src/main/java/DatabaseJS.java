import config.Config;

import instances.Cluster;
import java.util.ArrayList;
import instances.SharedData;
import instances.InstanceData;

import java.net.InetAddress;

import server.Listener;
import server.PKIContext;


public class DatabaseJS
{
  private static Config config = null;
  private static Cluster cluster = null;


  public static void main(String[] args) throws Exception
  {
    long time = System.nanoTime();
    
    Action action = null;
    Command cmd = options(args);
    
    try {action = Action.valueOf(cmd.args.get(0));}
    catch (Exception e) {usage();}
    
    boolean logging = (action != Action.status);
    config = new Config(cmd.inst,cmd.config,logging);
   
    switch(action)
    {
      case start:   start(cmd,time)   ; break;
      case status:  status()          ; break;
      
      default: usage();
    }
    
    if (action == Action.start) 
    {
      config.log.logger.info("instance["+cmd.inst+"] started, elapsed: "+elapsed(time));
      
      if (cluster.manager())
        config.log.cluster.info("Cluster Started, elapsed: "+elapsed(time));      
    }
  }


  private static void status() throws Exception
  {
    cluster = new Cluster(config,0);
    System.out.println(cluster.status());
  }


  private static void start(Command cmd, long time) throws Exception
  {
    config.log.logger.info("instance["+cmd.inst+"] starting");

    String host = hostname();
    if (cmd.args.size() > 1) usage();
    
    Listener ssl = null;
    Listener plain = null;
    Listener admin = null;
    PKIContext pki = null;
    
    if (config.http.requiressl)
    {
      pki = new PKIContext(config.security.identity,config.security.trust);

      ssl = new Listener(config,pki,host,config.http.ssl,false);
      ssl.start();
      
      config.log.logger.info("Listening on port "+config.http.ssl+", elapsed: "+elapsed(time));
      
      plain = new Listener(config,null,host,config.http.plain,false);
      plain.start();      

      config.log.logger.info("Listening on port "+config.http.plain+", elapsed: "+elapsed(time));
    }
    else
    {
      plain = new Listener(config,null,host,config.http.plain,false);
      plain.start();

      config.log.logger.info("Listening on port "+config.http.plain+", elapsed: "+elapsed(time));
      
      pki = new PKIContext(config.security.identity,config.security.trust);

      ssl = new Listener(config,pki,host,config.http.ssl,false);
      ssl.start();
      
      config.log.logger.info("Listening on port "+config.http.ssl+", elapsed: "+elapsed(time));
    }

    admin = new Listener(config,pki,host,config.http.admin,true);
    admin.start();
    
    config.log.logger.info("Listening on port "+config.http.admin+", elapsed: "+elapsed(time));
    
    cluster = new Cluster(config,cmd.inst);
    Runtime.getRuntime().addShutdownHook(new ShutdownHook());    

    cluster.register();
  }


  public static Config config()
  {
    return(config);
  }


  private static Command options(String[] args)
  {
    int inst = 0;
    String config = null;

    ArrayList<String> cargs = new ArrayList<String>();

    for (int i = 0; i < args.length; i++)
    {
      if (args[i].startsWith("-"))
      {
        if (args[i].equals("-i") || args[i].equals("--instance"))
        {
          try {inst = Integer.parseInt(args[++i]);}
          catch (Exception e) {usage();}
        }
        else if (args[i].equals("-c") || args[i].equals("--config"))
        {
          try {config = args[++i];}
          catch (Exception e) {usage();}
        }
        else usage();
      }
      else cargs.add(args[i]);
    }
    
    if (cargs.size() == 0)
      usage();

    Command cmd = new Command(inst,config,cargs);
    return(cmd);
  }
  
  
  private static String hostname()
  {
    String host = "localhost";
    
    try
    {
      InetAddress ip = InetAddress.getLocalHost();
      host = ip.getHostName();
    }
    catch (Exception e)
    {
      config.log.exception(e);
    }
    
    return(host);
  }
  
  
  private static String elapsed(long time)
  {
    double elapsed = (System.nanoTime()-time)/1000000000.0;
    return(String.format("%.3f",elapsed)+" secs");
  }


  private static void usage()
  {
    System.out.println();
    System.out.println();
    System.out.println("Usage: database.js [options] [cmd] [args]");
    System.out.println();
    System.out.println("options:");
    System.out.println("\t-m | --message <msg>");
    System.out.println("\t-i | --instance <inst> (internal use only)");
    System.out.println("\t-c | --config <config> specifies configuration, default is server");
    System.out.println();
    System.out.println("cmd:");
    System.out.println("\tstart           : starts all servers in cluster.");
    System.out.println("\tstop <secs>     : stops all servers in cluster.");
    System.out.println("\tstatus          : prints cluster status.");
    System.out.println("\tservers <n>     : change number of servers.");
    System.out.println("\tversion <vers>  : change app version.");
    System.out.println();
    System.exit(0);
  }


  private static class Command
  {
    public final int inst;
    public final String config;
    public final ArrayList<String> args;


    Command(int inst, String config, ArrayList<String> args)
    {
      this.args = args;
      this.inst = inst;
      this.config = config;
    }
    
    
    @Override
    public String toString()
    {
      String str = "database.js --instance "+inst;
      if (config != null) str += " --config "+config;
      for(String arg : args) str += " "+arg;
      return(str);
    }
  }
  
  
  private static enum Action
  {
    add,
    stop,
    start,
    status,
    remove
  }


  private static class ShutdownHook extends Thread
  {
    public void run()
    {
      try
      {
        cluster.deregister();
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }
}
