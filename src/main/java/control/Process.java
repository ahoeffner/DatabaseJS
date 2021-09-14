package control;

import config.Config;
import java.util.logging.Logger;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ManagementFactory;


public class Process
{
  private final String cmd;
  private final String htopt;
  private final String srvopt;
  private final Logger logger;
  
  
  public Process(Config config) throws Exception
  {
    logger = config.getLogger().logger;
    String java = config.getJava().exe();
    
    this.htopt = config.getJava().getHttpOptions();
    this.srvopt = config.getJava().getServerOptions();

    String classpath = (String) System.getProperties().get("java.class.path");

    RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
    
    
    String cmd = java+" -cp "+classpath;
    this.cmd = cmd;
  }
  

  public void start(Type type, int inst)
  {
    String options = null;
    
    if (type == Type.http) options = htopt;
    else                   options = srvopt;

    String cmd = this.cmd + " " + options + " Server " + inst;
    System.out.println(cmd);
    /*
    String cmd = this.cmd + " --instance "+inst;
    if (config.name != null) cmd += " --config "+config.name;
    cmd += " start";
    
    try
    {
      Runtime.getRuntime().exec(cmd);
      config.log.cluster.info("Starting instance["+inst+"]");
    }
    catch (Exception e)
    {
      config.log.cluster.log(Level.SEVERE,null,e);
    } 
    */
  }
  
  
  public static enum Type
  {
    http,
    rest,
    micro
  }
}
