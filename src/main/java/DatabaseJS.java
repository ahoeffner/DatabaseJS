import config.Config;

import server.Server;
import instances.Cluster;
import java.util.ArrayList;

public class DatabaseJS
{
  public static void main(String[] args) throws Exception
  {
    Action action = null;
    Command cmd = options(args);

    try {action = Action.valueOf(cmd.args.get(0));}
    catch (Exception e) {usage();}

    boolean logging = (action != Action.status);
    Config config = new Config(cmd.inst,cmd.config,logging);

    switch(action)
    {
      case start:   Server server = new Server(config,cmd.inst);
                    server.startup();
                    break;

      case status:  status(config); break;

      default: usage();
    }
  }


  private static void status(Config config) throws Exception
  {
    Cluster cluster = new Cluster(config,0);
    System.out.println(cluster.status());
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
}
