package config;

import java.io.File;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.XMLFormatter;

import org.json.JSONObject;


public class Logger
{
  public final boolean db;
  public final boolean http;
  public final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("global");

  private final static int LOGSIZE = 10 * 1024 * 1024;


  public Logger(String inst, String path, JSONObject section) throws Exception
  {
    int size = LOGSIZE;
    String lfsize = null;
    
    String level = "warning";
    String logdir = path+"/logs";
    String logfile = "server"+inst+".xml";
    
    int count = 2;
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
      logdir = path + "/" + logdir;
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
    
    logger.setUseParentHandlers(false);
    logger.setLevel(Level.parse(level.toUpperCase()));

    FileHandler handler = new FileHandler(logdir+File.separator+logfile,size,count,true);
    handler.setFormatter(new XMLFormatter());

    logger.addHandler(handler);
  }
}
