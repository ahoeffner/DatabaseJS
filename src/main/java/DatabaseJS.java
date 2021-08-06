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
    
    config = new Config(cmd.inst,cmd.config);
   
    switch(action)
    {
      case start: start(cmd); break;
      default: usage();
    }
  }


  private static void start(Command cmd) throws Exception
  {
    if (cmd.args.size() > 1)
      usage();
    
    cluster = new Cluster();
    Runtime.getRuntime().addShutdownHook(new ShutdownHook(cmd.inst));

    SharedData shareddata = cluster.shareddata;
    InstanceData data = shareddata.read(true);
    data.setInstance(cmd.inst,config.http.admin);
    shareddata.write(data);
    
    config.log.logger.info("starting "+cmd.inst);
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
    System.out.println("\t-m | --message");
    System.out.println("\t-i | --instance <inst> (internal use only)");
    System.out.println("\t-c | --config <config> specifies configuration, default is server");
    System.out.println();
    System.out.println("cmd:");
    System.out.println("\tstart       : starts all servers in cluster.");
    System.out.println("\tstop        : stops all servers in cluster.");
    System.out.println("\tstatus      : prints cluster status.");
    System.out.println("\tadd [n]     : adds <n> servers to the cluster.");
    System.out.println("\tremove [n]  : removes <n> servers from the cluster.");
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


  private static class ShutdownHook extends Thread
  {
    private final int inst;



    ShutdownHook(int inst)
    {
      this.inst = inst;
    }


    public void run()
    {
      try
      {
        SharedData shareddata = cluster.shareddata;
        InstanceData data = shareddata.read(true,true);
        data.removeInstance(inst);
        shareddata.write(data);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
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
}
