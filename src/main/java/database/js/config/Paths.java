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

import java.net.URL;
import java.io.File;
import java.nio.file.Path;


public class Paths
{
  public static final String ipcdir;
  public static final String libdir;
  public static final String tmpdir;
  public static final String apphome;
  public static final String confdir;

  private static final String LIBDIR = "lib";
  private static final String IPCDIR = "ipc";
  private static final String TMPDIR = "tmp";
  private static final String CONFDIR = "conf";


  static
  {
    apphome = findAppHome();
    libdir = apphome + File.separator + LIBDIR;
    ipcdir = apphome + File.separator + IPCDIR;
    tmpdir = apphome + File.separator + TMPDIR;
    confdir = apphome + File.separator + CONFDIR;
  }
  
  
  private static String findAppHome()
  {
    String sep = File.separator;
    Object obj = new Object() { };

    String cname = obj.getClass().getEnclosingClass().getName();
    cname = "/" + cname.replace('.','/') + ".class";

    URL url = obj.getClass().getResource(cname);
    String path = url.getPath();
    
    if (url.getProtocol().equals("jar") || url.getProtocol().equals("code-source"))
    {
      path = path.substring(5); // get rid of "file:"
      path = path.substring(0,path.indexOf("!")); // get rid of "!class"
      path = path.substring(0,path.lastIndexOf("/")); // get rid jarname
    }
    else
    {
      path = path.substring(0,path.length()-cname.length());
      if (path.endsWith("/classes")) path = path.substring(0,path.length()-8);
    }
    
    String escape = "\\";
    if (sep.equals(escape))
    {
      // Windows
      if (path.startsWith("/") && path.charAt(2) == ':')
        path = path.substring(1);
      
      path = path.replaceAll("/",escape+sep);
    }
    
    
    File cw = new File(".");
    Path abs = java.nio.file.Paths.get(path);
    Path base = java.nio.file.Paths.get(cw.getAbsolutePath());
    path = base.relativize(abs).toString();    

    // Back until conf folder
    
    while(true)
    {
      String conf = path+sep+"conf";
      
      File test = new File(conf);
      if (test.exists()) break;
      path = path.substring(0,path.lastIndexOf(sep));
    }
    
    return(path);
  }
}
