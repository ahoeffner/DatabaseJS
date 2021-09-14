package control;

import config.Config;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Process
{
  private final String cmd;
  private final String htopt;
  private final String srvopt;
  private final Logger logger;
  
  
  public Process(Config config) throws Exception
  {
    logger = config.getLogger().control;
    String java = config.getJava().exe();
    
    this.htopt = config.getJava().getHttpOptions();
    this.srvopt = config.getJava().getServerOptions();
    String classpath = (String) System.getProperties().get("java.class.path");

    String cmd = java+" -cp "+classpath;
    this.cmd = cmd;
  }
  

  public void start(Type type, int inst)
  {
    String options = null;
    
    if (type == Type.http) options = htopt;
    else                   options = srvopt;

    String cmd = this.cmd + " " + options + " control.Server " + inst;
    System.out.println(cmd);

    try
    {
      Runtime.getRuntime().exec(cmd);
      logger.info("Starting instance["+inst+"]");
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,null,e);
    } 
  }
  
  
  public static enum Type
  {
    http,
    rest,
    micro
  }
}
