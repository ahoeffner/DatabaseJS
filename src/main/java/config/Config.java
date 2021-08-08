package config;

import java.io.File;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.FileInputStream;


public class Config
{
  public final HTTP http;
  public final Logger log;
  public final String name;
  public final Cluster cluster;
  public final Security security;

  private static final String CONFDEF = "server.json";


  public Config(int inst, String config, boolean log) throws Exception
  {
    this.name = config;
    Object[] sections = this.load(Paths.apphome,config);

    this.log        = (Logger)  sections[0];
    this.http       = (HTTP)    sections[1];
    this.cluster    = (Cluster) sections[2];
    this.security   = (Security) sections[3];

    if (log) this.log.open(inst);
  }
  
  
  public void clslog() throws Exception
  {
    this.log.opencls();
  }


  private Object[] load(String path, String name) throws Exception
  {
    FileInputStream in = null;

    try
    {
      if (name == null) name = CONFDEF;
      if (!name.endsWith(".json")) name += ".json";
            
      in = new FileInputStream(Paths.confdir + File.separator + name);

      JSONTokener tokener = new JSONTokener(in);
      JSONObject  config  = new JSONObject(tokener);

      JSONObject  logconf = config.getJSONObject("log");
      Logger log = new Logger(path,logconf);

      JSONObject  httpconf = config.getJSONObject("http");
      HTTP http = new HTTP(path,httpconf);

      JSONObject  clsconf = config.getJSONObject("cluster");
      Cluster cluster = new Cluster(clsconf);

      JSONObject  secconf = config.getJSONObject("security");
      Security security = new Security(secconf);

      in.close();
      return(new Object[] {log,http,cluster,security});
    }
    catch (Exception e)
    {
      try {in.close();}
      catch(Exception ic) {;}
      throw e;
    }
  }
}
