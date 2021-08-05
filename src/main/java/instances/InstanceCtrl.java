package instances;

import java.io.File;
import java.util.List;
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
  
  
  public void start(int inst, String config)
  {
    String cmd = this.cmd + " --instance "+inst;
    if (config != null) cmd += " --config "+config;
    cmd += " start";
    
    try
    {
      System.out.println("Starting new inst");
      Process p = Runtime.getRuntime().exec(cmd);
      
      byte[] buf = new byte[30];
      int read = p.getInputStream().read(buf);
      System.out.println("Read "+new String(buf,0,read));
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }    
  }
}
