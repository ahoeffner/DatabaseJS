import config.Config;

import config.Paths;
import java.util.ArrayList;
import instances.SharedData;
import instances.InstanceData;
import instances.InstanceCtrl;


public class DatabaseJS
{
  private static Config config = null;
  private static SharedData shareddata = null;


  public static void main(String[] args) throws Exception
  {
    Command cmd = options(args);
    config = new Config(cmd.inst,cmd.config);
    
    InstanceCtrl cl = new InstanceCtrl();
    if (cmd.inst == 0) cl.start(cmd.inst+5,cmd.config);
    
    System.out.println("Starting inst "+cmd.inst);
    config.log.logger.warning("starting "+cmd.inst);

    //start(cmd);
  }


  private static void start(Command cmd) throws Exception
  {
    shareddata = new SharedData(Paths.sharefile);
    Runtime.getRuntime().addShutdownHook(new ShutdownHook(cmd.inst,shareddata));

    InstanceData data = shareddata.read(true);
    data.setInstance(cmd.inst,config.http.admin);
    shareddata.write(data);
  }


  public static Config config()
  {
    return(config);
  }


  public static SharedData shareddata()
  {
    return(shareddata);
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

    Command cmd = new Command(inst,config,cargs);
    return(cmd);
  }


  private static void usage()
  {
    System.out.println();
    System.out.println();
    System.out.println("Usage: database.js [options] [start | stop | status]");
    System.out.println("options:");
    System.out.println("\t-i | --instance <inst> (internal use)");
    System.out.println("\t-c | --config <config> specifies configuration, default is server");
    System.out.println();
    System.exit(0);
  }


  private static class Command
  {
    public final int inst;
    public final String config;
    public final ArrayList<String> cmd;


    Command(int inst, String config, ArrayList<String> cmd)
    {
      this.cmd = cmd;
      this.inst = inst;
      this.config = config;
    }
    
    
    @Override
    public String toString()
    {
      String str = "database.js --instance "+inst;
      if (config != null) str += " --config "+config;
      for(String arg : cmd) str += " "+arg;
      return(str);
    }
  }


  private static class ShutdownHook extends Thread
  {
    private final int inst;
    private final SharedData shareddata;



    ShutdownHook(int inst, SharedData shareddata)
    {
      this.inst = inst;
      this.shareddata = shareddata;
    }


    public void run()
    {
      try
      {
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
}
