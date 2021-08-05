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
  public final String sharefile;

  public static final String SEP = File.separator;
  
  private static final String TMPDIR = "tmp";
  private static final String CONFDIR = "conf";
  private static final String CONFDEF = "server.json";
  private static final String SHAREFILE = "shared.tab";
  
  
  public Config(int inst) throws Exception
  {
    this(findAppHome(),inst,null);
  }
  
  
  public Config(String apphome, int inst) throws Exception
  {
    this(apphome,inst,null);
  }
  
  
  public Config(String apphome, int inst, String config) throws Exception
  {
    this.inst = inst;
    
    this.apphome = apphome;
    this.tmpdir = tmpdir(this.apphome);
    this.sharefile = sharefile(this.apphome);
    
    Object[] sections = this.load(inst,this.apphome,config);
    
    this.log =  (Logger)  sections[0];
    this.http = (HTTP)    sections[1];
  }
  
  
  private Object[] load(int instno, String path, String name) throws Exception
  {
    FileInputStream in = null;

    try
    {
      if (name == null) name = CONFDEF;
      if (!name.endsWith(".json")) name += ".json";
      
      String inst = String.format("%1$3s",instno).replace(' ','0');
      in = new FileInputStream(path+SEP+CONFDIR+SEP+name);
      
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
  
  
  public static String tmpdir()
  {
    return(tmpdir(findAppHome()));
  }
  
  
  public static String tmpdir(String apphome)
  {
    return(apphome + SEP + TMPDIR);
  }
  
  
  public static String sharefile()
  {
    return(sharefile(findAppHome()));
  }
  
  
  public static String sharefile(String apphome)
  {
    return(tmpdir(apphome) + SEP + SHAREFILE);    
  }
  
  
  public static String findAppHome()
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
