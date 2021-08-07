package config;

import java.io.File;
import logger.Formatter;
import org.json.JSONObject;
import java.util.logging.Level;
import java.util.logging.FileHandler;


public class Logger
{
  public final boolean db;
  public final boolean http;
  public final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("default");
  public final java.util.logging.Logger cluster = java.util.logging.Logger.getLogger("cluster");

  
  private int count = 2;
  private int size = LOGSIZE;
  private String level = "WARNING";
  private String logdir = "." + File.separator + "logs";
  
  private static final String logfile = "server.log";
  private static final String clsfile = "cluster.log";
  private final static int LOGSIZE = 10 * 1024 * 1024;
  
  
  public void exception(Throwable exception)
  {
    logger.log(Level.SEVERE,null,exception);
  }


  public Logger(String path, JSONObject section) throws Exception
  {
    String lfsize = null;
    
    boolean db = false, http = false;
        
    if (section.has("db"))      db = section.getBoolean("db");
    if (section.has("http"))    http = section.getBoolean("http");
    if (section.has("level"))   level = section.getString("level");
    
    if (section.has("files"))   count = section.getInt("files");
    if (section.has("size"))    lfsize = section.getString("size");
    if (section.has("path"))    logdir = section.getString("path");
    
    if (lfsize != null)
    {
      int mp = 1;
      lfsize = lfsize.trim();
      if (lfsize.endsWith("KB")) mp = 1024;
      if (lfsize.endsWith("MB")) mp = 1024*1024;
      if (lfsize.endsWith("GB")) mp = 1024*1024*1024;
      if (mp > 1) lfsize = lfsize.substring(0,lfsize.length()-2);
      size = Integer.parseInt(lfsize.trim()) * mp;      
    }

    if (logdir.startsWith("."))
    {
      logdir = path + File.separator + logdir;
      File logf = new File(logdir);
      logdir = logf.getCanonicalPath();
    }
    
    File ldir = new File(logdir);

    if (!ldir.exists())
      throw new Exception(ldir+" does not exist");

    if (!ldir.isDirectory())
      throw new Exception(ldir+" is not a directory");

    this.db = db;
    this.http = http;
  }
  
  
  public void open(int inst) throws Exception
  {
    String instdir = logdir + File.separator+"inst"+String.format("%1$3s",inst).replace(' ','0');

    File ldir = new File(instdir);
    if (!ldir.exists()) ldir.mkdir();

    logger.setUseParentHandlers(false);
    logger.setLevel(Level.parse(level.toUpperCase()));
    
    Formatter formatter = new Formatter();

    FileHandler handler = new FileHandler(instdir+File.separator+logfile,size,count,true);
    handler.setFormatter(formatter);

    logger.addHandler(handler);
  }
  
  
  public void opencls() throws Exception
  {
    String clsdir = logdir;  

    cluster.setLevel(Level.ALL);
    cluster.setUseParentHandlers(false);
    
    Formatter formatter = new Formatter();

    FileHandler chandler = new FileHandler(clsdir+File.separator+clsfile,1024*1024,2,true);
    chandler.setFormatter(formatter);

    cluster.addHandler(chandler);
  }
}
