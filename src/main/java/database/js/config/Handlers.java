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

import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import database.js.handlers.Handler;
import database.js.handlers.FileHandler;
import database.js.handlers.RestHandler;
import database.js.handlers.AdminHandler;


public class Handlers
{  
  private final Config config;  
  private final ArrayList<HandlerClass> entries = new ArrayList<HandlerClass>();
  
  private RestHandler rest = null;
  private FileHandler file = null;
  private AdminHandler admin = null;
  
  
  Handlers(Config config)
  {
    this.config = config;
  }
  
  
  void finish() throws Exception
  {
    Collections.sort(this.entries);
    
    this.admin = new AdminHandler(config);
    
    for(HandlerClass hdl : this.entries)
    {
      switch(hdl.name())
      {
        case "database.js.handlers.FileHandler" : 
          this.file = (FileHandler) hdl.handler;
          break;

        case "database.js.handlers.RestHandler" : 
          this.rest = (RestHandler) hdl.handler;
          break;
      }
    }
  }
    
  
  void add(String prefix, String methods, String clazz) throws Exception
  {
    if (!prefix.endsWith("/")) prefix += "/";
    this.entries.add(new HandlerClass(config,prefix,methods,clazz));
  }
  
  
  public RestHandler getRESTHandler()
  {
    return(rest);
  }
  
  
  public FileHandler getFileHandler()
  {
    return(file);
  }
  
  
  public AdminHandler getAdminHandler()
  {
    return(admin);
  }
  
  
  public Handler getHandler(String path, String method)
  {
    for(HandlerClass entry : entries)
    {
      if (path.startsWith(entry.prefix))
      {
        if (entry.methods.contains(method))
          return(entry.handler);
      }
    }
    
    return(null);
  }
  
  
  private static class HandlerClass implements Comparable<HandlerClass>
  {
    public final String prefix;
    public final Handler handler;
    public final HashSet<String> methods = new HashSet<String>();
    
    HandlerClass(Config config, String prefix, String methods, String clazz) throws Exception
    {
      this.prefix = prefix;
      String meth[] = methods.split(",");
      for(String m : meth) if (m.length() > 0) this.methods.add(m.toUpperCase());
      this.handler = (Handler) Class.forName(clazz).getDeclaredConstructor(Config.class).newInstance(config);
    }
    
    
    public String name()
    {
      return(handler.getClass().getName());
    }
    
    
    @Override
    public String toString()
    {
      return(prefix+" "+methods+" "+handler.getClass().getName());
    }


    @Override
    public int compareTo(HandlerClass another)
    {
      return(this.prefix.length() - another.prefix.length());
    }
  }
}
