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

import database.js.config.Config;

import java.io.File;


public class Deployment
{
  private final String home;
  private final Config config;
  private static Deployment deployment = null;
  
  
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
    this.home = this.config.getHTTP().getAppPath();
  }
  
  
  public synchronized void deploy() throws Exception
  {
    String sep = File.separator;
    File home = new File(this.home);
    String dep = this.home + sep + home.lastModified();
    if (!(new File(dep).exists())) deploy(this.home,dep);
  }
  
  
  private void deploy(String fr, String to)
  {
    File source = new File(fr);
    File target = new File(to);
    if (!target.exists()) target.mkdirs();
  }
}
