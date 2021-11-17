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
import org.json.JSONObject;
import org.json.JSONTokener;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Rest
{
  private final String path;
  private final String payload;
  private final String[] parts;
  private final boolean modify;

  private String err = null;
  private Session ses = null;

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


  public Rest(Logger logger, String path, boolean modify, String payload)
  {
    this.path = path;
    this.modify = modify;
    this.logger = logger;
    this.payload = payload;
    this.parts = path.split("/");
  }


  public String execute()
  {
    if (parts.length == 0)
    {
      error("Invalid rest path");
      return(err);
    }
    
    try
    {
      JSONObject payload = parse();
      if (err != null) return(err);

      if (hasSessionSpec() && !getSession())
        return(err);
      
      String cmd = getCommand(ses != null);
      if (err != null) return(err);
      
      if (cmd.equals("batch"))
        return(batch(payload));
      
      if (cmd.equals("script"))
        return(script(payload));
      
      String result = exec(cmd,payload);      
      return(result);
    }
    catch (Throwable e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
      error(e.getMessage());
      return(err);
    }
  }


  private String batch(JSONObject payload)
  {
    return(null);
  }


  private String script(JSONObject payload)
  {
    return(null);
  }


  private String exec(String cmd, JSONObject payload)
  {
    return(null);
  }


  private JSONObject parse()
  {
    if (this.payload == null)
    {
      error("Missing rest payload");
      return(null);
    }
    
    try
    {
      JSONTokener tokener = new JSONTokener(payload);
      return(new JSONObject(tokener));
    }
    catch (Exception e)
    {
      error(e.getMessage());
      return(null);
    }
  }


  private boolean getSavepoint(JSONObject payload)
  {
    try
    {
      if (payload.has("savepoint"))
        return(payload.getBoolean("savepoint"));
    }
    catch (Exception e)
    {
      error(e.getMessage());
    }

    return(false);
  }
  
  
  private String getCommand(boolean ses)
  {
    String cmd = parts[0];
    if (ses) cmd = parts[1];
    cmd = cmd.toLowerCase();
    
    if (!commands.contains(cmd))
    {
      error("Unknown rest part '"+cmd+"'");
      return(null);
    }
    
    return(cmd);
  }


  private boolean hasSessionSpec()
  {
    if (parts.length < 2)
      return(false);

    if (!commands.contains(parts[1].toLowerCase()))
      return(false);

    return(true);
  }


  private boolean getSession()
  {
    this.ses = Session.get(parts[0]);
    
    if (this.ses == null) 
    {
      error("Session '"+parts[0]+"' does not exist");
      return(false);
    }
    
    return(true);
  }


  private void error(String message)
  {
    if (message == null)
      message = "An unexpected error has occured";

    message = escape(message);
    this.err = "{\"status\": \"failed\", \"message\": \""+message+"\"}";
  }


  private static String escape(String str)
  {
    str = JSONObject.quote(str);
    return(str);
  }


  private static String quote(String str)
  {
    return("\""+str+"\"");
  }
  
  
  private static class SessionState
  {
    int shared = 0;
    boolean exclusive = false;
    
    void lock(Session session, boolean exclusive) throws Exception
    {
      session.lock(exclusive);
      if (!exclusive) shared++;
      else this.exclusive = true;
    }
    
    void release(Session session, boolean exclusive) throws Exception
    {
      if (!exclusive && shared < 1)
        throw new Exception("Cannot release shared lock not obtained");

      session.release(exclusive);
      
      if (!exclusive) shared--;
      else this.exclusive = false;
    }
    
    void releaseAll(Session session) throws Exception
    {
      session.releaseAll(exclusive,shared);
    }
  }
}