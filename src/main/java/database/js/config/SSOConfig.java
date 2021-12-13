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
import org.json.JSONTokener;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.logger.Formatter;
import database.js.security.Keystore;
import java.util.logging.FileHandler;
import database.js.security.PKIContext;


public class SSOConfig
{
  public final PKIContext pkictx;
  private final Formatter formatter = new Formatter();
  private final static int LOGSIZE = 10 * 1024 * 1024;
  private String logdir = "." + File.separator + "logs";
  public final static Logger logger = Logger.getLogger("sso");


  public SSOConfig() throws Exception
  {
    String path = path() + "ssoconfig.json";
    FileInputStream in = new FileInputStream(path);
    JSONObject config  = new JSONObject(new JSONTokener(in));

    initlogger(getSection(config,"logger"));
    this.pkictx = getPkiContext(getSection(config,"security"));
  }


  private void initlogger(JSONObject config) throws Exception
  {
    String path = Paths.apphome;
    String level = get(config,"level");

    int count = get(config,"files",1);
    String logdir = get(config,"path",this.logdir);

    String lfsize = Config.get(config,"size",null);
    if (config.has("size")) lfsize = Config.get(config,"size");

    int size = LOGSIZE;

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

    if (logdir.startsWith("."+File.separator))
    {
      logdir = path + File.separator + logdir;
      File logf = new File(logdir);
      logdir = logf.getCanonicalPath();
    }

    File ldir = new File(logdir);

    if (ldir.exists() && !ldir.isDirectory())
      throw new Exception(ldir+" is not a directory");

    if (!ldir.exists())
      ldir.mkdirs();

    String lfile = logdir + File.separator + "sso.log";

    FileHandler handler = new FileHandler(lfile,size,count,true);
    handler.setFormatter(formatter);

    logger.setUseParentHandlers(false);
    logger.setLevel(Level.parse(level.toUpperCase()));

    logger.addHandler(handler);
  }


  private PKIContext getPkiContext(JSONObject config) throws Exception
  {
    String type = null;
    String file = null;
    String passwd = null;

    JSONObject identsec = getSection(config,"identity");

    type = get(identsec,"type");
    file = get(identsec,"keystore");
    passwd = get(identsec,"password");
    String alias = get(identsec,"alias");

    if (file.startsWith("."+File.separator))
      file = Paths.apphome + File.separator + file;

    Keystore identity = new Keystore(file,type,alias,passwd);

    JSONObject trustsec = getSection(config,"trust");

    type = get(trustsec,"type");
    file = get(trustsec,"keystore");
    passwd = get(trustsec,"password");

    if (file.startsWith("."+File.separator))
      file = Paths.apphome + File.separator + file;

    Keystore trust = new Keystore(file,type,null,passwd);

    return(new PKIContext(identity,trust));
  }


  public static String path()
  {
    return(Paths.confdir + File.separator);
  }


  @SuppressWarnings({ "unchecked", "cast" })
  public static <T> T get(JSONObject config, String attr)
  {
    return((T) config.get(attr));
  }


  @SuppressWarnings({ "unchecked", "cast" })
  public static <T> T get(JSONObject config, String attr, T defval)
  {
    T value = defval;

    if (config.has(attr) && !config.isNull(attr))
      value = (T) config.get(attr);

    return(value);
  }


  public static JSONObject getSection(JSONObject config, String section) throws Exception
  {
    JSONObject conf = null;

    if (config.has(section))
      conf = config.getJSONObject(section);

    if (conf == null)
      System.err.println("Section <"+section+"> does not exist");

    return(conf);
  }
}