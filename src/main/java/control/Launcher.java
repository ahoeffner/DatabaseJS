package control;

import java.io.File;
import config.Paths;
import config.Config;
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
    config.getLogger().openControlLog();
    Process process = new Process(config);
    Topology topology = config.getTopology();
    
    if (topology.type() == Topology.Type.Micro)
    {
      process.start(Process.Type.http,0);

      if (topology.hotstandby())
        process.start(Process.Type.http,1);
    }
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
        Loader loader = new Loader(ILauncher.class);

        String path = Paths.libdir+File.separator+"json";
        String psep = System.getProperty("path.separator");
        String classpath = (String) System.getProperties().get("java.class.path");

        File dir = new File(path);
        String[] jars = dir.list();

        for(String jar : jars)
          loader.load(path + File.separator + jar);

        jars = classpath.split(psep);

        for(String jar : jars)
          loader.load(jar);

        Class Launcher = loader.getClass(Launcher.class);
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
