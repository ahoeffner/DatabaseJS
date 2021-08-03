package config;

import java.io.File;

import org.json.JSONObject;


public class HTTP
{
  public final int ssl;
  public final int plain;
  public final int admin;
  public final String path;
  public final String corsheader;
  public final boolean requiressl;
  public final RequestMap requestmap;
  
  
  public HTTP(String inst, String path, JSONObject section) throws Exception
  {
    JSONObject ports = section.getJSONObject("ports");

    this.ssl = ports.getInt("ssl");
    this.plain = ports.getInt("plain");
    this.admin = ports.getInt("admin");
    
    
    String corsheader = null;
    boolean requiressl = false;
    JSONObject security = section.getJSONObject("security");
    
    if (security.has("require.ssl")) requiressl = security.getBoolean("require.ssl");
    this.requiressl = requiressl;

    String elem = "Access-Control-Allow-Origin";
    
    if (security.has(elem) && !security.isNull(elem)) 
      corsheader = security.getString("Access-Control-Allow-Origin");

    if (corsheader != null && corsheader.trim().length() == 0)
      corsheader = null;
    
    this.corsheader = corsheader;
    

    String apppath = null;
    JSONObject app = section.getJSONObject("application");
    apppath = app.getString("path");
    
    if (apppath.startsWith("."))
    {
      apppath = path + File.separator + apppath;
      File appf = new File(apppath);
      apppath = appf.getCanonicalPath();
    }
    
    this.path = apppath;
    this.requestmap = new RequestMap();
        
    JSONObject entry = app.getJSONObject("html");
    this.requestmap.add(entry.getString("url"),entry.getString("methods"));
        
    entry = app.getJSONObject("rest");
    this.requestmap.add(entry.getString("url"),entry.getString("methods"));
        
    entry = app.getJSONObject("service");
    this.requestmap.add(entry.getString("url"),entry.getString("methods"));
    
    this.requestmap.print();
  }
}
