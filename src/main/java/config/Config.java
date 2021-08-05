package config;

import java.io.File;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.FileInputStream;


public class Config
{
  public final int inst;
  public final HTTP http;
  public final Logger log;

  private static final String CONFDEF = "server.json";


  public Config(int inst, String config) throws Exception
  {
    this.inst = inst;

    Object[] sections = this.load(inst,Paths.apphome,config);

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
      in = new FileInputStream(Paths.confdir + File.separator + name);

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
}
