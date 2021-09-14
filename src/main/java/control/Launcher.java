package control;

import java.io.File;
import config.Paths;
import config.Config;
import config.Java;
import config.Topology;

import org.json.JSONTokener;


/**
 *
 * In case there are no json-lib in classpath, the launcher will dynamically load it.
 * It then reads the topologi and starts the servers as per config.
 *
 */
public class Launcher implements ILauncher
{
  @SuppressWarnings("unused") 
  public static void main(String[] args) throws Exception
  {    
    ILauncher launcher = create();
    launcher.startProcesses();
  }
  
  
  public void startProcesses() throws Exception
  {
    Config config = new Config();
    Java java = config.getJava();
    Topology topology = config.getTopology();
    
    System.out.println("java: "+java.exe());
    System.out.println("type: "+topology.type());
  }
  
  
  @SuppressWarnings("unchecked")
  private static ILauncher create() throws Exception
  {
    ILauncher launcher = null;
    
    try
    {
      // Test json-lib
      new JSONTokener("{}");
      launcher = new Launcher();
    }
    catch (Throwable e)
    {
      try
      {
        control.Loader loader = new control.Loader(Launcher.class);
        
        loader.add(Java.class);
        loader.add(Config.class);
        loader.add(Topology.class);
        
        loader.load(Paths.libdir+File.separator+"json");
        
        Class Launcher = loader.entrypoint();
        launcher = (ILauncher) Launcher.getDeclaredConstructor().newInstance();
      }
      catch (Exception le)
      {
        throw le;
      }
    }
    
    return(launcher);
  }
}
