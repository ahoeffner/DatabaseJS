import config.Config;

import java.io.File;
import config.Paths;
import org.json.JSONTokener;

/**
 * 
 * The launcher dynamically loads the json library to be able
 * 
 */
public class Launcher
{
  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws Exception
  {
    String config = "default";

    Loader loader = new Loader(Launcher.class);
    
    loader.add(Test.class);
    loader.load(Paths.libdir+File.separator+"json");
    
    Class Launcher = loader.entrypoint();
    Launcher.getDeclaredConstructor(String.class).newInstance(config);
  }
  
  
  public Launcher(String config) throws Exception
  {
    JSONTokener tokener = new JSONTokener("{}");
    System.out.println(config+" tokener = "+tokener);
    Test test = new Test();
  }
}
