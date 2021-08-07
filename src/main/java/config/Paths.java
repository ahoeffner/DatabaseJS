package config;

import java.net.URL;
import java.io.File;


public class Paths
{
  public static final String tmpdir;
  public static final String confdir;
  public static final String apphome;
  public static final String sharefile;
  
  private static final String TMPDIR = "tmp";
  private static final String CONFDIR = "conf";
  private static final String SHAREFILE = "cluster.dat";

  
  
  static
  {
    apphome = findAppHome();
    tmpdir = apphome + File.separator + TMPDIR;
    confdir = apphome + File.separator + CONFDIR;
    sharefile = tmpdir + File.separator + SHAREFILE;
  }
  
  
  private static String findAppHome()
  {
    Object obj = new Object() { };

    String cname = obj.getClass().getEnclosingClass().getName();
    cname = "/" + cname.replace('.','/') + ".class";

    URL url = obj.getClass().getResource(cname);
    String path = url.getPath();
    
    if (url.getProtocol().equals("jar") || url.getProtocol().equals("code-source"))
    {
      path = path.substring(5); // get rid of file:
      path = path.substring(0,path.indexOf("!")); // get rid of !class
      path = path.substring(0,path.lastIndexOf(File.separator)); // get rid jarname
    }
    else
    {
      path = path.substring(0,path.length()-cname.length());
      if (path.endsWith("/classes")) path = path.substring(0,path.length()-8);
    }
    
    while(path.length() > 0)
    {
      if (new File(path+File.separator+CONFDIR).exists()) break;
      path = path.substring(0,path.lastIndexOf(File.separator));    
    }
    
    return(path);
  }
}
