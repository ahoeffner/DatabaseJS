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
import java.io.Serializable;
import java.io.FileInputStream;
import java.util.logging.Logger;
import java.io.FileOutputStream;
import database.js.config.Config;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
  private Date moddate = null;
  private HashMap<String,StaticFile> index = null;

  private static final String sep = File.separator;


  public static Deployment get()
  {
    return(deployment);
  }


  public static synchronized void init(Config config) throws Exception
  {
    if (deployment == null)
      deployment = new Deployment(config);
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


  public static Date modified()
  {
    return(deployment.moddate);
  }


  public StaticFile get(String path) throws Exception
  {
    if (this.index == null)
    {
      synchronized(this)
      {
        while(this.index == null)
          this.wait();
      }
    }

    File deploy = new File(this.deploy + sep + modified);

    if (!deploy.exists() && !index())
      deploy();

    return(this.index.get(path));
  }


  @SuppressWarnings("unchecked")
  public synchronized boolean index() throws Exception
  {
    long latest = latest();
    if (latest == modified) return(false);

    Date modified = new Date(latest);
    String deployment = this.deploy + sep + latest;
    if (!(new File(deployment).exists())) return(false);

    logger.info("Indexing website");
    HashMap<String,StaticFile> index = new HashMap<String,StaticFile>();

    FileInputStream fin = new FileInputStream(deployment + sep + ".index");
    ObjectInputStream oin = new ObjectInputStream(fin);

    index = (HashMap<String,StaticFile>) oin.readObject();

    this.index = index;
    this.modified = latest;
    this.moddate = modified;

    return(true);
  }


  public synchronized void deploy() throws Exception
  {
    Date modified = new Date();
    File home = new File(this.home);
    modified.setTime(home.lastModified());

    String dep = this.deploy + sep + home.lastModified();
    String tmp = this.deploy + sep + "d" + home.lastModified();
    HashMap<String,StaticFile> index = new HashMap<String,StaticFile>();

    if (!(new File(dep).exists()))
    {
      if (logger.getHandlers().length < 0)
        logger.info("Deploying website");

      deploy(index,this.home,tmp,dep);

      File deployed = new File(tmp);
      deployed.renameTo(new File(dep));

      FileOutputStream fout = new FileOutputStream(dep + sep +".index");
      ObjectOutputStream oout = new ObjectOutputStream(fout);

      oout.writeObject(index);

      oout.close();
      fout.close();

      this.index = index;
      this.moddate = modified;
      this.modified = home.lastModified();
      synchronized(this) {this.notifyAll();}

      this.cleanup();
    }
  }


  private void deploy(HashMap<String,StaticFile> index, String fr, String to, String dest) throws Exception
  {
    File source = new File(fr);
    File target = new File(to);

    String[] entries = source.list();
    if (!target.exists()) target.mkdirs();

    for(String entry : entries)
    {
      String dfr = fr + sep + entry;
      String dto = to + sep + entry;
      String des = dest + sep + entry;

      File deploy = new File(dfr);
      if (deploy.isDirectory())
      {
        deploy(index,dfr,dto,des);
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

        index.put(dfr,new StaticFile(deploy.getName(),des,cache,compress));
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


  public long latest() throws Exception
  {
    long latest = modified;

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

        if (mod > latest) latest = mod;
      }
    }

    return(latest);
  }


  private void cleanup()
  {
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

        if (mod > 0 && mod != this.modified)
        {
          File old = new File(this.deploy + sep + mod);
          delete(old);
        }
      }
    }
  }


  private void delete(File file)
  {
    File[] entries = file.listFiles();

    for(File entry : entries)
    {
      if (entry.isDirectory())
        delete(entry);

      entry.delete();
    }

    file.delete();
  }


  public static class StaticFile implements Serializable
  {
    public final String virpath;
    public final String actpath;

    public final boolean cache;
    public final boolean compressed;

    @SuppressWarnings("compatibility:3091933754958126888")
    private static final long serialVersionUID = 5613263707445370115L;


    StaticFile(String virpath, String actpath, boolean cache, boolean compressed)
    {
      this.cache = cache;
      this.virpath = virpath;
      this.actpath = actpath;
      this.compressed = compressed;
    }
    
    
    public byte[] get() throws Exception
    {
      File file = new File(actpath);
      if (!file.exists()) throw new Exception("File "+actpath+" not found");
      
      byte[] content = new byte[(int) file.length()];
      FileInputStream in = new FileInputStream(file);
      
      int read = in.read(content);
      if (read != content.length) throw new Exception("Read "+actpath+" returned partial result");
      
      return(content);
    }
  }
}
