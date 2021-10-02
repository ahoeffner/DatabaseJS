/*
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.

 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

package database.js.config;

import java.io.File;
import org.json.JSONObject;
import java.util.logging.Level;
import java.io.FileOutputStream;
import database.js.logger.Formatter;
import java.util.logging.FileHandler;


public class Logger
{
  public final java.util.logging.Logger http = java.util.logging.Logger.getLogger("http");
  public final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("others");
  public final java.util.logging.Logger control = java.util.logging.Logger.getLogger("control");
  public final java.util.logging.Logger database = java.util.logging.Logger.getLogger("database");

  private final String level;
  private final String dblevel;
  private final String htlevel;
  private final Formatter formatter = new Formatter();
  
  private int count = 2;
  private int size = LOGSIZE;
  private boolean open = false;
  private String logdir = "." + File.separator + "logs";
      
  private static final String logfile = "server.log";
  private static final String ctrfile = "control.log";
  private final static int LOGSIZE = 10 * 1024 * 1024;


  Logger(JSONObject config) throws Exception
  {
    String lfsize = null;
    String path = Paths.apphome;
    
    level = config.getString("others");
    htlevel = config.getString("http");
    dblevel = config.getString("database");
    
    if (config.has("files"))   count = config.getInt("files");
    if (config.has("size"))    lfsize = config.getString("size");
    if (config.has("path"))    logdir = config.getString("path");
    
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
  }
  
  
  public synchronized String getServerOut(int inst)
  {
    File ldir = new File(logdir);
    if (!ldir.exists()) ldir.mkdir();

    String instdir = logdir + File.separator+"inst"+String.format("%1$2s",inst).replace(' ','0');

    ldir = new File(instdir);
    if (!ldir.exists()) ldir.mkdir();

    return(instdir+File.separator+"server.out");
  }
  
  
  public synchronized void openControlLog() throws Exception
  {
    File ldir = new File(logdir);
    if (!ldir.exists()) ldir.mkdir();

    FileHandler handler = new FileHandler(logdir+File.separator+ctrfile,size,count,true);
    handler.setFormatter(formatter);

    control.setUseParentHandlers(false);
    control.setLevel(Level.parse(level.toUpperCase()));

    control.addHandler(handler);
  }
  
  
  public synchronized void open(int inst) throws Exception
  {
    if (open) return;
    String instdir = logdir + File.separator+"inst"+String.format("%1$2s",inst).replace(' ','0');

    File ldir = new File(instdir);
    if (!ldir.exists()) ldir.mkdir();
    
    String lfile = instdir+File.separator+logfile;

    File check = new File(lfile+".0");
    if (check.exists())
    {
      FileOutputStream out = new FileOutputStream(lfile+".0",true);
      out.write(System.lineSeparator().getBytes());
      out.write(System.lineSeparator().getBytes());
      out.write(System.lineSeparator().getBytes());
      out.close();
    }

    FileHandler handler = new FileHandler(lfile,size,count,true);
    handler.setFormatter(formatter);

    http.setUseParentHandlers(false);
    http.setLevel(Level.parse(htlevel.toUpperCase()));

    http.addHandler(handler);

    logger.setUseParentHandlers(false);
    logger.setLevel(Level.parse(level.toUpperCase()));

    logger.addHandler(handler);

    database.setUseParentHandlers(false);
    database.setLevel(Level.parse(dblevel.toUpperCase()));

    database.addHandler(handler);
    open = true;
  }
}
