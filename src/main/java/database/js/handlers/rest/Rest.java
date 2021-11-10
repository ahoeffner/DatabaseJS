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

package database.js.handlers.rest;

import java.util.TreeSet;
import java.util.logging.Logger;


public class Rest
{
  private final String path; 
  private final String payload;
  private final String[] parts;
  
  private final Logger logger;
  private static final TreeSet<String> commands = new TreeSet<String>();
  
  static
  {
    commands.add("ping");
    commands.add("lock");
    commands.add("call");
    commands.add("batch");
    commands.add("fetch");
    commands.add("script");
    commands.add("status");
    commands.add("select");
    commands.add("insert");
    commands.add("update");
    commands.add("delete");
    commands.add("commit");
    commands.add("connect");
    commands.add("rollback");
  }
  
  
  public Rest(Logger logger, String path, String payload)
  {
    this.path = path;
    this.logger = logger;
    this.payload = payload;
    this.parts = path.split("/");
  }
  
  
  public String execute()
  {
    try
    {
      ;
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return(null);
  }
  
  
  private String exec(String cmd, String payload) throws Exception
  {
    return(null);    
  }
  
  
  private boolean isConnected()
  {
    if (parts.length < 2)
      return(false);
    
    if (!commands.contains(parts[1].toLowerCase()))
      return(false);
    
    return(true);
  }
}
