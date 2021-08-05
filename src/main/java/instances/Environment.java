package instances;

import java.io.File;
import java.util.List;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ManagementFactory;


public class Environment
{
  public Environment()
  {
    System.out.println(System.getProperties().get("java.class.path"));    
    RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
    List<String> options = bean.getInputArguments();
    for (String opt : options) System.out.println(opt); 
    
    String jvm;
    
    if (System.getProperty("os.name").startsWith("Win")) jvm = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe";
    else                                                 jvm = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java";

    System.out.println(jvm);
  }
}
