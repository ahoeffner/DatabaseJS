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
import database.js.config.Config;
import database.js.database.Pool;
import database.js.database.AuthMethod;


public class Rest
{
  private final String path;
  private final String payload;
  private final String[] parts;
  private final boolean modify;

  private String error = null;
  private Session session = null;

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
      return(error);
    }

    try
    {
      JSONObject payload = parse();
      if (error != null) return(error);

      if (hasSessionSpec() && !getSession())
        return(error);

      String cmd = getCommand(session != null);
      if (error != null) return(error);

      if (cmd.equals("batch"))
        return(batch(payload));

      if (cmd.equals("script"))
        return(script(payload));

      String result = exec(cmd,payload);
      if (error != null) return(error);

      return(result);
    }
    catch (Throwable e)
    {
      error(e);
      return(error);
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

    switch(cmd)
    {
      case "connect" :
        response = connect(payload); break;
    }

    if (error != null)
    {
      state.releaseAll(this);
      return(error);
    }

    return(response);
  }


  private String connect(JSONObject payload)
  {
    Pool pool = null;
    String secret = null;
    String username = null;
    AuthMethod method = null;
    boolean dedicated = false;
    boolean anonymous = false;

    try
    {
      if (payload.has("username"))
        username = payload.getString("username");

      if (payload.has("dedicated"))
        dedicated = payload.getBoolean("dedicated");

      if (payload.has("anonymous"))
        anonymous = payload.getBoolean("anonymous");

      if (payload.has("auth.secret"))
        secret = payload.getString("auth.secret");

      if (payload.has("auth.method"))
      {
        String meth = payload.getString("auth.method");

        switch(meth.toLowerCase())
        {
          case "oauth" : method = AuthMethod.OAuth; break;
          case "database" : method = AuthMethod.Database; break;
          case "pool-token" : method = AuthMethod.PoolToken; break;

          default: error("Unknown authentication method "+meth);
        }

        if (method == AuthMethod.PoolToken)
        {
          if (anonymous) pool = config.getDatabase().proxy();
          else           pool = config.getDatabase().anonymous();
        }

        if (error != null)
          return(null);

        if (!anonymous && username == null)
          error("Username must be specified");

        if (error != null)
          return(null);

        this.session = new Session(method,pool,dedicated,username,secret);
      }
    }
    catch (Throwable e)
    {
      error(e);
    }

    return("{\"status\": \"ok\"}");
  }


  private String query(JSONObject payload)
  {
    if (session == null)
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
    catch (Throwable e)
    {
      error(e);
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
    catch (Throwable e)
    {
      error(e);
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
    this.session = Session.get(parts[0]);

    if (this.session == null)
    {
      error("Session '"+parts[0]+"' does not exist");
      return(false);
    }

    return(true);
  }


  void error(Throwable e)
  {
    error(e.getMessage());
    logger.log(Level.WARNING,e.getMessage(),e);
  }


  void error(String message)
  {
    if (message == null)
      message = "An unexpected error has occured";

    message = escape(message);
    this.error = "{\"status\": \"failed\", \"message\": \""+message+"\"}";
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
      Session session = rest.session;

      try
      {
        session.lock(exclusive);
        if (!exclusive) shared++;
        else this.exclusive = true;
      }
      catch (Throwable e)
      {
        rest.error(e);
      }
    }


    void release(Rest rest, boolean exclusive)
    {
      Session session = rest.session;

      try
      {
        if (!exclusive && shared < 1)
          throw new Throwable("Cannot release shared lock not obtained");

        session.release(exclusive);

        if (!exclusive) shared--;
        else this.exclusive = false;
      }
      catch (Throwable e)
      {
        rest.error(e);
      }
    }


    void releaseAll(Rest rest)
    {
      Session session = rest.session;
      if (session == null) return;

      try
      {
        if (exclusive || shared > 0)
          session.releaseAll(exclusive,shared);
      }
      catch (Throwable e)
      {
        rest.error(e);
      }
    }
  }
}