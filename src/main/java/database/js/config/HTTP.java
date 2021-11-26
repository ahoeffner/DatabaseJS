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
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;


public class HTTP
{
  private final int ssl;
  private final int plain;
  private final int admin;
  private final int bsize;
  private final int grace;
  private final String host;
  private final String path;
  private final int timeout;
  private final String tmppath;
  private final String virtendp;
  private final Handlers handlers;
  private final boolean requiressl;
  private final ArrayList<FilePattern> cache;
  private final ArrayList<String> corsdomains;
  private final ArrayList<FilePattern> compression;
  private final ConcurrentHashMap<String,String> mimetypes;


  HTTP(Handlers handlers, JSONObject config) throws Exception
  {
    String host = InetAddress.getLocalHost().getHostName();
    if (config.has("Host") && !config.isNull("Host")) host = config.getString("Host");

    this.host = host;
    this.timeout = config.getInt("KeepAlive") * 1000;

    if (!config.has("buffers")) this.bsize = 4096;
    else
    {
      JSONObject buffers = config.getJSONObject("buffers");
      this.bsize = buffers.getInt("network");
    }

    JSONObject app = config.getJSONObject("application");

    grace = app.getInt("grace.period");

    String apppath = null;
    apppath = app.getString("path");

    if (apppath.startsWith("." + File.separator))
    {
      apppath = Paths.apphome + File.separator + apppath;
      File appf = new File(apppath);
      apppath = appf.getCanonicalPath();
    }

    this.path = apppath;
    File tmp = new File(Paths.tmpdir);
    this.tmppath = tmp.getCanonicalPath();

    JSONObject ports = config.getJSONObject("ports");

    this.ssl = ports.getInt("ssl");
    this.plain = ports.getInt("plain");
    this.admin = ports.getInt("admin");


    boolean requiressl = false;
    JSONObject security = config.getJSONObject("security");

    if (security.has("require.ssl")) requiressl = security.getBoolean("require.ssl");
    this.requiressl = requiressl;

    String elem = "Cors-Allow-Domains";
    this.corsdomains = new ArrayList<String>();

    String corsdomains = null;

    if (security.has(elem) && !security.isNull(elem))
      corsdomains = security.getString(elem);

    if (corsdomains != null)
    {
      String[] domains = corsdomains.split("[ ,]+");

      for (int i = 0; i < domains.length; i++)
      {
        domains[i] = domains[i].trim();
        if (domains[i].length() > 0) this.corsdomains.add(domains[i]);
      }
    }


    if (!config.has("virtual-path")) this.virtendp = null;
    else
    {
      JSONObject virtp = config.getJSONObject("virtual-path");
      this.virtendp = virtp.getString("endpoint");
    }


    this.handlers = handlers;

    JSONArray hconfig = config.getJSONArray("handlers");

    for (int i = 0; i < hconfig.length(); i++)
    {
      JSONObject entry = hconfig.getJSONObject(i);
      this.handlers.add(entry.getString("url"),entry.getString("methods"),entry.getString("class"));
    }

    this.handlers.finish();


    this.cache = new ArrayList<FilePattern>();
    JSONArray cache = config.getJSONArray("cache");

    for (int i = 0; i < cache.length(); i++)
    {
      JSONObject entry = cache.getJSONObject(i);

      int size = entry.getInt("maxsize");
      String pattern = entry.getString("pattern");

      this.cache.add(new FilePattern(pattern,size));
    }


    this.compression = new ArrayList<FilePattern>();
    JSONArray compression = config.getJSONArray("compression");

    for (int i = 0; i < compression.length(); i++)
    {
      JSONObject entry = compression.getJSONObject(i);

      int size = entry.getInt("minsize");
      String pattern = entry.getString("pattern");

      this.compression.add(new FilePattern(pattern,size));
    }

    JSONArray mtypes = config.getJSONArray("mimetypes");
    this.mimetypes = new ConcurrentHashMap<String,String>();

    for (int i = 0; i < mtypes.length(); i++)
    {
      JSONObject entry = mtypes.getJSONObject(i);
      mimetypes.put(entry.getString("ext"),entry.getString("type"));
    }
  }


  public String host()
  {
    return(host);
  }

  public int timeout()
  {
    return(timeout);
  }

  public int ssl()
  {
    return(ssl);
  }

  public int plain()
  {
    return(plain);
  }

  public int admin()
  {
    return(admin);
  }

  public int bufsize()
  {
    return(bsize);
  }

  public int graceperiod()
  {
    return(grace);
  }

  public String getVirtualEndpoint()
  {
    return(virtendp);
  }

  public String getAppPath()
  {
    return(path);
  }

  public String getTmpPath()
  {
    return(tmppath);
  }

  public Handlers handlers()
  {
    return(handlers);
  }

  public boolean requiressl()
  {
    return(requiressl);
  }

  public ArrayList<FilePattern> cache()
  {
    return(cache);
  }

  public ArrayList<String> corsdomains()
  {
    return(corsdomains);
  }

  public ArrayList<FilePattern> compression()
  {
    return(compression);
  }

  public ConcurrentHashMap<String,String> mimetypes()
  {
    return(mimetypes);
  }


  public static class FilePattern
  {
    public final int size;
    public final String pattern;

    FilePattern(String pattern, int size)
    {
      this.size = size;

      pattern = pattern.replace(".","\\.");
      pattern = pattern.replace("*",".*");

      this.pattern = pattern;
    }
  }
}