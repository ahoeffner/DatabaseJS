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
import database.js.security.PKIContext;
import java.util.concurrent.ConcurrentHashMap;


public class Config
{
  private final String inst;
  private final boolean full;
  private final String topconf;
  private final String dataconf;
  private final JSONObject config;

  private SSO sso = null;
  private Java java = null;
  private Rest rest = null;
  private HTTP http = null;
  private Ports ports = null;
  private Logger logger = null;
  private Handlers handlers = null;
  private PKIContext pkictx = null;
  private Security security = null;
  private Topology topology = null;
  private Database database = null;

  private ConcurrentHashMap<String,JSONObject> sections =
    new ConcurrentHashMap<String,JSONObject>();


  public static int clientTimeout()
  {
    return(2000);
  }


  public static String path()
  {
    return(Paths.confdir + File.separator);
  }


  public static boolean windows()
  {
    String os = System.getProperty("os.name");
    return(os.toLowerCase().startsWith("win"));
  }


  public Config() throws Exception
  {
    this(true);
  }


  public Config(boolean full) throws Exception
  {
    this.full = full;
    String path = path() + "config.json";
    FileInputStream in = new FileInputStream(path);
    this.config  = new JSONObject(new JSONTokener(in));

    this.inst = get("instance");
    this.topconf = get("topology");
    this.dataconf = get("database");

    sections.put("sso",getSection("sso"));
    sections.put("http",getSection("http"));
    sections.put("rest",getSection("rest"));
    sections.put("logger",getSection("logger"));
    sections.put("security",getSection("security"));
    sections.put("topology",getSection("topology",topconf));
    sections.put("database",getSection("database",dataconf));

    Statics.init(this);
  }


  public String instance()
  {
    return(inst);
  }


  public synchronized SSO getSSO() throws Exception
  {
    if (sso != null) return(sso);
    this.sso = new SSO(sections.get("sso"));
    return(sso);
  }


  public synchronized Ports getPorts() throws Exception
  {
    if (ports != null) return(ports);
    ports = getHTTP().ports;
    return(ports);
  }


  public synchronized PKIContext getPKIContext() throws Exception
  {
    if (pkictx != null) return(pkictx);
    Security security = this.getSecurity();
    pkictx = new PKIContext(security.getIdentity(),security.getTrusted());
    return(pkictx);
  }


  public synchronized Java getJava() throws Exception
  {
    if (java != null) return(java);
    java = new Java(getSection(sections.get("topology"),"java"));
    return(java);
  }


  public synchronized HTTP getHTTP() throws Exception
  {
    if (http != null) return(http);
    if (full) handlers = new Handlers(this);
    http = new HTTP(handlers,sections.get("http"));
    return(http);
  }


  public synchronized Rest getREST() throws Exception
  {
    if (rest != null) return(rest);
    rest = new Rest(sections.get("rest"));
    return(rest);
  }


  public synchronized Logger getLogger() throws Exception
  {
    if (logger != null) return(logger);
    logger = new Logger(sections.get("logger"),inst);
    return(logger);
  }


  public synchronized Database getDatabase() throws Exception
  {
    if (database != null) return(database);
    database = new Database(sections.get("database"));
    return(database);
  }


  public synchronized Security getSecurity() throws Exception
  {
    if (security != null) return(security);
    security = new Security(sections.get("security"));
    return(security);
  }


  public synchronized Topology getTopology() throws Exception
  {
    if (topology != null) return(topology);
    topology = new Topology(sections.get("topology"));
    return(topology);
  }


  @SuppressWarnings({ "unchecked", "cast" })
  public static <T> T get(JSONObject config, String attr)
  {
    return((T) config.get(attr));
  }


  @SuppressWarnings({ "unchecked", "cast" })
  public static <T> T getArray(JSONObject config, String attr)
  {
    return((T) config.getJSONArray(attr));
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


  public static String getPath(String path, String parent)
  {
    try
    {
      if (path.startsWith("." + File.separator) || path.startsWith("./"))
      {
        path = parent + File.separator + path;
        File appf = new File(path);
        path = appf.getCanonicalPath();
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return(path);
  }


  @SuppressWarnings({ "unchecked", "cast" })
  private <T> T get(String attr)
  {
    return((T) config.get(attr));
  }


  private JSONObject getSection(String section) throws Exception
  {
    return(getSection(this.config,section));
  }


  private JSONObject getSection(String path, String fname) throws Exception
  {
    path = path() + path + File.separator + fname;
    FileInputStream in = new FileInputStream(path+".json");
    JSONObject config  = new JSONObject(new JSONTokener(in));
    return(config);
  }
}