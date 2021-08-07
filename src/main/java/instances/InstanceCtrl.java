package instances;

import java.io.File;
import config.Config;
import java.util.List;
import java.util.logging.Level;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ManagementFactory;


public class InstanceCtrl
{
  public final String cmd;
  
  
  public InstanceCtrl()
  {
    String jvm = null;
    
    if (System.getProperty("os.name").startsWith("Win")) jvm = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe";
    else                                                 jvm = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java";

    String classpath = (String) System.getProperties().get("java.class.path");

    RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
    List<String> options = bean.getInputArguments();
    
    String cmd = jvm+" -cp "+classpath;
    for(String option : options) cmd += " "+option;
    cmd += " DatabaseJS";
    
    this.cmd = cmd;
  }
  
  
  public void start(int inst, Config config)
  {
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
  }
}
