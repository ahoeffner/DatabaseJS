package config;

import java.net.URL;
import java.io.File;


public class Config
{
  private String apphome = null;
  
  
  public Config() throws Exception
  {
    this.apphome = this.findAppHome();
    System.out.println("app: "+this.apphome);
  }
  
  
  private String findAppHome()
  {
    Object obj = new Object() { };

    String cname = obj.getClass().getEnclosingClass().getName();
    cname = "/" + cname.replace('.','/') + ".class";

    URL url = this.getClass().getResource(cname);
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
      if (new File(path+"/config").exists()) break;
      path = path.substring(0,path.lastIndexOf(File.separator));    
    }
    
    return(path);
  }
}
