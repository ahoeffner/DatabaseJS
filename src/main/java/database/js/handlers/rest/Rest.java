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

import database.js.config.Config;
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

  private final Config config;
  private final Logger logger;
  
  private final SessionState state = new SessionState();
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


  public Rest(Config config, String path, boolean modify, String payload) throws Exception
  {
    this.path     = path;
    this.config   = config;
    this.modify   = modify;
    this.payload  = payload;
    this.logger   = config.getLogger().rest;
    this.parts    = path.substring(1).split("/");
  }


  public String execute()
  {
    if (parts.length == 0)
    {
      error("Invalid rest path");
      System.out.println(err);
      return(err);
    }

    try
    {
      System.out.println("I");
      JSONObject payload = parse();
      if (err != null) return(err);

      System.out.println("II");
      if (hasSessionSpec() && !getSession())
        return(err);

      System.out.println("III");
      String cmd = getCommand(ses != null);
      if (err != null) return(err);

      System.out.println("IIII");
      if (cmd.equals("batch"))
        return(batch(payload));

      System.out.println("IIIII");
      if (cmd.equals("script"))
        return(script(payload));

      System.out.println("IIIIII");
      String result = exec(cmd,payload);
      if (err != null) System.out.println(err);
      if (err != null) return(err);
      System.out.println(result);
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
    String response = null;
    
    response = query(cmd,payload);

    if (err != null)
    {
      state.releaseAll(this);
      return(err);
    }

    return(response);
  }


  private String query(String cmd, JSONObject payload)
  {
    if (ses == null)
    {
      error("Not connected");
      return(null);      
    }
    
    return("{\"status\": \"ok\"}");
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
    System.out.println("Parts <"+parts[0]+">"+parts.length);
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


  void error(String message)
  {
    if (message == null)
      message = "An unexpected error has occured";

    message = escape(message);
    this.err = "{\"status\": \"failed\", \"message\": \""+message+"\"}";
  }


  static String escape(String str)
  {
    str = JSONObject.quote(str);
    return(str);
  }


  static String quote(String str)
  {
    return("\""+str+"\"");
  }


  private static class SessionState
  {
    int shared = 0;
    boolean exclusive = false;

    void lock(Rest rest, boolean exclusive)
    {
      Session session = rest.ses;

      try
      {
        session.lock(exclusive);
        if (!exclusive) shared++;
        else this.exclusive = true;
      }
      catch (Exception e)
      {
        rest.error(e.getMessage());
      }
    }

    void release(Rest rest, boolean exclusive)
    {
      Session session = rest.ses;

      try
      {
        if (!exclusive && shared < 1)
          throw new Exception("Cannot release shared lock not obtained");

        session.release(exclusive);

        if (!exclusive) shared--;
        else this.exclusive = false;
      }
      catch (Exception e)
      {
        rest.error(e.getMessage());
      }
    }

    void releaseAll(Rest rest)
    {
      Session session = rest.ses;

      try
      {
        session.releaseAll(exclusive,shared);
      }
      catch (Exception e)
      {
        rest.error(e.getMessage());
      }
    }
  }
}