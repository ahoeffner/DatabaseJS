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
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.database.Pool;
import database.js.database.AuthMethod;


public class Rest
{
  private final String host;
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
    commands.add("disconnect");
  }


  public Rest(Config config, String path, boolean modify, String host, String payload) throws Exception
  {
    this.host     = host;
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
      if (payload == null) return(error);

      String cmd = divert();
      if (cmd == null) return(error);

      if (cmd.equals("batch"))
        return(batch(payload));

      if (cmd.equals("script"))
        return(script(payload));

      return(exec(cmd,payload,false));
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


  private String exec(String cmd, JSONObject payload, boolean batch)
  {
    String response = null;

    switch(cmd)
    {
      case "connect" :
        response = connect(payload,batch); break;

      case "select" :
        response = query(payload,batch); break;

      default : error("Unknown command "+cmd);
    }

    if (!batch || error != null)
      state.releaseAll(this);

    return(response);
  }


  private String connect(JSONObject payload, boolean batch)
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
          return(error);

        if (!anonymous && username == null)
          error("Username must be specified");

        if (error != null)
          return(error);

        this.session = new Session(method,pool,dedicated,username,secret);

        if (dedicated || method == AuthMethod.Database) this.session.connect();
        if (!dedicated && !batch && method == AuthMethod.Database) this.session.disconnect();
      }
    }
    catch (Throwable e)
    {
      error(e);
      return(error);
    }

    JSONFormatter json = new JSONFormatter();

    json.success(true);
    json.add("session",session.guid());

    return(json.toString());
  }


  private String query(JSONObject payload, boolean batch)
  {
    if (session == null)
    {
      error("Not connected");
      return(error);
    }

    try
    {
      session.ensure();

      if (!session.dedicated() && !batch)
        session.disconnect();
    }
    catch (Exception e)
    {
      error(e);
      return(error);
    }

    JSONFormatter json = new JSONFormatter();
    json.success(true);

    return(json.toString());
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


  private String divert()
  {
    String cmd = null;
    boolean ses = false;

    parts[0] = parts[0].toLowerCase();
    if (commands.contains(parts[0])) cmd = parts[0];

    if (cmd == null && parts.length > 1)
    {
      ses = true;
      this.session = Session.get(parts[0]);

      parts[1] = parts[1].toLowerCase();
      if (commands.contains(parts[1])) cmd = parts[1];
    }

    if (cmd == null)
    {
      error("Rest path "+path+" not mapped to any service");
      return(null);
    }

    if (ses && session == null)
    {
      error("Session '"+parts[0]+"' does not exist");
      return(null);
    }

    return(cmd);
  }


  void error(Throwable e)
  {
    JSONFormatter json = new JSONFormatter();

    json.set(e);
    json.success(false);

    this.error = json.toString();
  }


  void error(String message)
  {
    JSONFormatter json = new JSONFormatter();

    json.success(false);
    json.add("message",message);

    this.error = json.toString();
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