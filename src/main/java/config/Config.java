package config;

import java.io.File;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.FileInputStream;


public class Config
{
  private final String loge; 
  private final String httpe; 
  private final String securitye; 
  private final String topologye; 
  private final String databasee;
  
  private Java java = null;
  private HTTP http = null;
  private Security security = null;
  private Topology topology = null;
  //private Database database = null;
  
  private static final String CONFDEF = "conf.json";
  private static final String JAVADEF = "java.json";
  private static final String HTTPDEF = "http.json";
  private static final String LOGGERDEF = "logger.json";
  private static final String SECURITYDEF = "security.json";

  private static final String TOPOLOGY = "topology";
  private static final String DATABASE = "database";
  
  
  public static boolean windows()
  {
    String os = System.getProperty("os.name");
    return(os.toLowerCase().startsWith("win"));    
  }


  public Config() throws Exception
  {
    FileInputStream in = new FileInputStream(confpath());
    
    JSONTokener tokener = new JSONTokener(in);
    JSONObject  config  = new JSONObject(tokener);
    
    this.topologye = config.getString("topology");
    this.databasee = config.getString("database");

    if (!config.has("log")) loge = "logger"; 
    else  loge = config.getString("logger");
    
    if (!config.has("http")) httpe = "http"; 
    else   httpe = config.getString("http");
    
    if (!config.has("security")) securitye = "security"; 
    else       securitye = config.getString("security");
  }
  
  
  public synchronized Java getJava() throws Exception
  {
    if (java != null) return(java);
    FileInputStream in = new FileInputStream(javapath());
    
    JSONTokener tokener = new JSONTokener(in);
    JSONObject  config  = new JSONObject(tokener);
    
    java = new Java(config);
    return(java);
  }
  
  
  public synchronized HTTP getHTTP() throws Exception
  {
    if (http != null) return(http);
    FileInputStream in = new FileInputStream(javapath());
    
    JSONTokener tokener = new JSONTokener(in);
    JSONObject  config  = new JSONObject(tokener);
    
    http = new HTTP(config);
    return(http);
  }
  
  
  public synchronized Security getSecurity() throws Exception
  {
    if (security != null) return(security);
    FileInputStream in = new FileInputStream(javapath());
    
    JSONTokener tokener = new JSONTokener(in);
    JSONObject  config  = new JSONObject(tokener);
    
    security = new Security(config);
    return(security);
  }
  
  
  public synchronized Topology getTopology() throws Exception
  {
    if (topology != null) return(topology);
    FileInputStream in = new FileInputStream(toppath());
    
    JSONTokener tokener = new JSONTokener(in);
    JSONObject  config  = new JSONObject(tokener);
    
    topology = new Topology(config);
    return(topology);
  }
  
  
  private String path()
  {
    return(Paths.confdir + File.separator);
  }
  
  
  private String javapath()
  {
    return(path() + JAVADEF);
  }
  
  
  private String httppath()
  {
    return(path() + HTTPDEF);
  }
  
  
  private String loggerpath()
  {
    return(path() + LOGGERDEF);
  }
  
  
  private String securitypath()
  {
    return(path() + SECURITYDEF);
  }
  
  
  private String confpath()
  {
    return(path() + CONFDEF);
  }
  
  
  private String dbpath()
  {
    return(path() + DATABASE + File.separator + databasee + ".json");
  }
  
  
  private String toppath()
  {
    return(path() + TOPOLOGY + File.separator + topologye + ".json");
  }
}
