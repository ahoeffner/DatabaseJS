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


public class Handlers
{  
  private ArrayList<Entry> entries = new ArrayList<Entry>();
    
  
  void add(String prefix, String methods, String clazz) throws Exception
  {
    if (1 == 1) return;
    if (!prefix.endsWith("/")) prefix += "/";
    this.entries.add(new Entry(prefix,methods,clazz));
    Collections.sort(this.entries);
  }
  
  
  public database.js.handlers.Handler getHandler(String path, String method)
  {
    for(Entry entry : entries)
    {
      if (path.startsWith(entry.prefix))
      {
        if (entry.methods.contains(method))
          return(entry.handler);
      }
    }
    
    return(null);
  }
  
  
  private static class Entry implements Comparable<Entry>
  {
    public final String prefix;
    public final database.js.handlers.Handler handler;
    public final HashSet<String> methods = new HashSet<String>();
    
    Entry(String prefix, String methods, String clazz) throws Exception
    {
      this.prefix = prefix;
      String meth[] = methods.split(",");
      for(String m : meth) if (m.length() > 0) this.methods.add(m.toUpperCase());
      this.handler = (database.js.handlers.Handler) Class.forName(clazz).getDeclaredConstructor().newInstance();
    }
    
    
    @Override
    public String toString()
    {
      return(prefix+" "+methods+" "+handler.getClass().getName());
    }


    @Override
    public int compareTo(Entry another)
    {
      return(this.prefix.length() - another.prefix.length());
    }
  }
}
