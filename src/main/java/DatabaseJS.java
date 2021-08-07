import config.Config;

import instances.Cluster;
import java.util.ArrayList;
import instances.SharedData;
import instances.InstanceData;


public class DatabaseJS
{
  private static Config config = null;
  private static Cluster cluster = null;


  public static void main(String[] args) throws Exception
  {
    Action action = null;
    Command cmd = options(args);
    
    try {action = Action.valueOf(cmd.args.get(0));}
    catch (Exception e) {usage();}
    
    boolean logging = (action != Action.status);
    config = new Config(cmd.inst,cmd.config,logging);
   
    switch(action)
    {
      case start:   start(cmd)  ; break;
      case status:  status()    ; break;
      
      default: usage();
    }
  }


  private static void status() throws Exception
  {
    cluster = new Cluster(config,0);
    System.out.println(cluster.status());
  }


  private static void start(Command cmd) throws Exception
  {
    if (cmd.args.size() > 1) usage();
    
    // Start listeners
    
    cluster = new Cluster(config,cmd.inst);
    Runtime.getRuntime().addShutdownHook(new ShutdownHook());    

    cluster.register();
    config.log.logger.info("instance["+cmd.inst+"] starting");
    
    if (cluster.manager()) 
      config.log.cluster.info("starting instance["+cmd.inst+"]");
        
    if (cluster.manager()) Thread.sleep(60000);
    else Thread.sleep(30000);
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


  private static void usage()
  {
    System.out.println();
    System.out.println();
    System.out.println("Usage: database.js [options] [cmd] [args]");
    System.out.println();
    System.out.println("options:");
    System.out.println("\t-f | --force");
    System.out.println("\t-m | --message <msg>");
    System.out.println("\t-i | --instance <inst> (internal use only)");
    System.out.println("\t-c | --config <config> specifies configuration, default is server");
    System.out.println();
    System.out.println("cmd:");
    System.out.println("\tstart           : starts all servers in cluster.");
    System.out.println("\tstop            : stops all servers in cluster.");
    System.out.println("\tstatus          : prints cluster status.");
    System.out.println("\tadd [n]         : adds <n> servers to the cluster.");
    System.out.println("\tremove [n]      : removes <n> servers from the cluster.");
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
