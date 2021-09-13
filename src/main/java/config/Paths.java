package config;

import java.net.URL;
import java.io.File;


public class Paths
{
  public static final String libdir;
  public static final String tmpdir;
  public static final String apphome;
  public static final String confdir;

  private static final String LIBDIR = "lib";
  private static final String TMPDIR = "tmp";
  private static final String CONFDIR = "conf";


  static
  {
    apphome = findAppHome();
    libdir = apphome + File.separator + LIBDIR;
    tmpdir = apphome + File.separator + TMPDIR;
    confdir = apphome + File.separator + CONFDIR;
  }
  
  
  private static String findAppHome()
  {
    String sep = File.separator;
    Object obj = new Object() { };

    String cname = obj.getClass().getEnclosingClass().getName();
    cname = "/" + cname.replace('.','/') + ".class";

    URL url = obj.getClass().getResource(cname);
    String path = url.getPath();

    if (url.getProtocol().equals("jar") || url.getProtocol().equals("code-source"))
    {
      path = path.substring(5); // get rid of "file:"
      path = path.substring(0,path.indexOf("!")); // get rid of "!class"
      path = path.substring(0,path.lastIndexOf("/")); // get rid jarname
    }
    else
    {
      path = path.substring(0,path.length()-cname.length());
      if (path.endsWith("/classes")) path = path.substring(0,path.length()-8);
    }
    
    String escape = "\\";
    if (sep.equals("\\")) escape = "\\"; 
    path = path.replaceAll("/",escape+sep);
    
    // Back until conf folder
    
    while(true)
    {
      String conf = path+sep+"conf";
      
      File test = new File(conf);
      if (test.exists()) break;
      path = path.substring(0,path.lastIndexOf(File.separator));
    }

    return(path);
  }
}
