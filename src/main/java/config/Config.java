package config;

import java.net.URL;
import java.io.File;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.FileInputStream;


public class Config
{
  public final int inst;
  public final HTTP http;
  public final Logger log;
  public final String tmpdir;
  public final String apphome;
  public final String lockfile;
  
  private static final String confdir = "conf";
  private static final String confdef = "server.json";
  
  public static final String sep = File.separator;
  
  
  public Config(int inst) throws Exception
  {
    this(inst,null);
  }
  
  
  public Config(int inst, String name) throws Exception
  {
    this.inst = inst;
    
    this.apphome = this.findAppHome();
    this.tmpdir = this.apphome + sep + "tmp";
    this.lockfile = this.tmpdir + sep + "locks.tab";
    
    Object[] sections = this.load(inst,this.apphome,name);
    
    this.log =  (Logger)  sections[0];
    this.http = (HTTP)    sections[1];
  }
  
  
  private Object[] load(int instno, String path, String name) throws Exception
  {
    FileInputStream in = null;

    try
    {
      if (name == null) name = confdef;
      if (!name.endsWith(".json")) name += ".json";
      
      String inst = String.format("%1$3s",instno).replace(' ','0');
      in = new FileInputStream(path+sep+confdir+sep+name);
      
      JSONTokener tokener = new JSONTokener(in);
      JSONObject  config  = new JSONObject(tokener);
      
      JSONObject  logconf = config.getJSONObject("log");
      Logger log = new Logger(inst,path,logconf);
      
      JSONObject  httpconf = config.getJSONObject("http");
      HTTP http = new HTTP(path,httpconf);
      
      in.close();
      return(new Object[] {log,http});
    }
    catch (Exception e)
    {
      try {in.close();}
      catch(Exception ic) {;}
      throw e;
    }
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
      if (new File(path+File.separator+confdir).exists()) break;
      path = path.substring(0,path.lastIndexOf(File.separator));    
    }
    
    return(path);
  }
}
