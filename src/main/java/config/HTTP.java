package config;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONObject;


public class HTTP
{
  public final int ssl;
  public final int plain;
  public final int admin;
  public final String path;
  public final String version;
  public final Handlers handlers;
  public final boolean requiressl;
  public final String[] corsdomains;
  
  
  public HTTP(String path, JSONObject section) throws Exception
  {
    String apppath = null;
    apppath = section.getString("path");
    
    if (apppath.startsWith("."))
    {
      apppath = path + File.separator + apppath;
      File appf = new File(apppath);
      apppath = appf.getCanonicalPath();
    }

    this.path = apppath;
    
    String version = "";
    
    if (!section.isNull("version"))
      version = section.getString("version");
    
    this.version = version;

    JSONObject ports = section.getJSONObject("ports");

    this.ssl = ports.getInt("ssl");
    this.plain = ports.getInt("plain");
    this.admin = ports.getInt("admin");
    
    
    String corsdomains = null;
    boolean requiressl = false;
    JSONObject security = section.getJSONObject("security");
    
    if (security.has("require.ssl")) requiressl = security.getBoolean("require.ssl");
    this.requiressl = requiressl;

    String elem = "Cors-Allow-Sites";
    
    if (security.has(elem) && !security.isNull(elem)) 
      corsdomains = security.getString(elem);

    if (corsdomains != null && corsdomains.trim().length() == 0)
      corsdomains = null;
    
    String[] domains = null;
    
    if (corsdomains != null) 
    {
      domains = corsdomains.split("[ ,]+");
      for (int i = 0; i < domains.length; i++)
      {
        domains[i] = domains[i].trim();
        if (!domains[i].equals("*")) domains[i] = "."+domains[i]+".";
      }
    }
    
    this.corsdomains = domains;
    this.handlers = new Handlers();


    JSONArray handlers = section.getJSONArray("handlers");
    
    for (int i = 0; i < handlers.length(); i++)
    {
      JSONObject entry = handlers.getJSONObject(i);
      this.handlers.add(entry.getString("url"),entry.getString("methods"),entry.getString("class"));
    }
  }
}
