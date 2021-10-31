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

package database.js.handlers.file;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.FileInputStream;
import java.util.logging.Logger;
import java.io.FileOutputStream;
import database.js.config.Config;
import java.text.SimpleDateFormat;
import java.util.zip.GZIPOutputStream;
import database.js.config.HTTP.FilePattern;


public class Deployment
{
  private static Deployment deployment = null;

  private final String home;
  private final String deploy;
  private final Config config;
  private final Logger logger;
  
  private final ArrayList<FilePattern> cache;
  private final ArrayList<FilePattern> compression;
  
  private long modified = 0;
  private HashMap<String,StaticFile> index = null;
  
  private static final String sep = File.separator;
  private static final SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM YYYY hh:mm:ss z");
  
  
  public static Deployment get()
  {
    return(deployment);
  }
  
  
  public static synchronized void init(Config config) throws Exception
  {
    if (deployment == null) 
    {
      deployment = new Deployment(config);
      deployment.deploy();
    }
  }
  
  
  public Deployment(Config config) throws Exception
  {
    this.config = config;
    this.logger = config.getLogger().http;
    this.cache = this.config.getHTTP().cache();
    this.home = this.config.getHTTP().getAppPath();
    this.deploy = this.config.getHTTP().getTmpPath();
    this.compression = this.config.getHTTP().compression();
  }
  
  
  public StaticFile get(String path) throws Exception
  {
    File home = new File(this.home);
    long current = home.lastModified();
    
    if (this.index == null)
    {
      synchronized(this)
      {
        while(this.index == null)
          this.wait();
      }
    }
    
    if (current != this.modified)
    {
      long newest = 0;
      File deployed = new File(this.deploy);
      File[] deployments = deployed.listFiles();
      
      for(File deployment : deployments)
      {
        String name = deployment.getName();
        char fc = deployment.getName().charAt(0);
        
        if (fc >= '0' && fc <= '9')
        {
          long mod = 0;
          
          try {mod = Long.parseLong(name);} 
          catch (Exception e) {;}
          
          if (mod > newest) newest = mod;
        }
      }
      
      if (newest > this.modified)
      {
        logger.info("New deployment detected");

        synchronized(this) 
        {
          //reindex
          //this.index = index;
          this.modified = home.lastModified();
        }
      }
    }
    
    return(this.index.get(path));
  }
  
  
  public synchronized void deploy() throws Exception
  {
    Date mod = new Date();
    File home = new File(this.home);
    mod.setTime(home.lastModified());
    String dep = this.deploy + sep + home.lastModified();
    String tmp = this.deploy + sep + "d" + home.lastModified();
    HashMap<String,StaticFile> index = new HashMap<String,StaticFile>();
    
    if (!(new File(dep).exists())) 
    {
      String modified = format.format(mod);      
      deploy(index,modified,this.home,tmp);

      File deployed = new File(tmp);
      deployed.renameTo(new File(dep));
    }
    
    this.index = index;
    this.modified = home.lastModified();
    synchronized(this) {this.notifyAll();}
  }
  
  
  private void deploy(HashMap<String,StaticFile> index, String modified, String fr, String to) throws Exception
  {
    File source = new File(fr);
    File target = new File(to);
    if (!target.exists()) target.mkdirs();
    
    String[] entries = source.list();
    
    for(String entry : entries)
    {
      String dfr = fr + sep + entry;
      String dto = to + sep + entry;
      
      File deploy = new File(dfr);
      if (deploy.isDirectory())
      {
        deploy(index,modified,dfr,dto);
      }
      else
      {
        boolean cache = false;
        boolean compress = false;
        long size = deploy.length();
        dfr = dfr.substring(this.home.length());
        
        for(FilePattern fpatrn : this.cache)
        {
          if (size <= fpatrn.size && deploy.getName().matches(fpatrn.pattern))
            cache = true;
        }
        
        for(FilePattern fpatrn : this.compression)
        {
          if (size >= fpatrn.size && deploy.getName().matches(fpatrn.pattern))
            compress = true;
        }

        if (!compress) copy(deploy,dto);
        else           compress(deploy,dto);
        
        index.put(dfr,new StaticFile(modified,deploy.getName(),dto,cache,compress));
      }
    }
  }
  
  
  public void copy(File ifile, String file) throws Exception
  {
    FileInputStream in = new FileInputStream(ifile);
    FileOutputStream out = new FileOutputStream(file);

    int read = 0;
    byte[] buf = new byte[4096];

    while(read >= 0)
    {
      read = in.read(buf);
      if (read > 0) out.write(buf,0,read);
    }

    out.close();
    in.close();
  }
  
  
  public void compress(File ifile, String file) throws Exception
  {
    FileInputStream in = new FileInputStream(ifile);
    FileOutputStream out = new FileOutputStream(file);
    GZIPOutputStream gout = new GZIPOutputStream(out);

    int read = 0;
    byte[] buf = new byte[4096];

    while(read >= 0)
    {
      read = in.read(buf);
      if (read > 0) gout.write(buf,0,read);
    }

    gout.close();
    out.close();
    in.close();
  }
  
  
  public class StaticFile
  {
    public final String virpath;
    public final String actpath;
    public final String modified;
    
    public final boolean cache;
    public final boolean compressed;
    
    
    StaticFile(String modified, String virpath, String actpath, boolean cache, boolean compressed)
    {
      this.cache = cache;
      this.virpath = virpath;
      this.actpath = actpath;
      this.modified = modified;
      this.compressed = compressed;
    }
  }
}
