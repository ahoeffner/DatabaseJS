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

import java.io.File;
import java.util.Date;
import java.util.Base64;
import java.util.TreeSet;
import java.util.HashMap;
import org.json.JSONArray;
import java.sql.Savepoint;
import org.json.JSONObject;
import java.util.ArrayList;
import org.json.JSONTokener;
import java.io.FileInputStream;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.database.Pool;
import database.js.database.SQLParser;
import database.js.database.AuthMethod;
import database.js.database.BindValueDef;
import database.js.database.NameValuePair;
import java.util.concurrent.ConcurrentHashMap;
import static database.js.handlers.rest.JSONFormatter.Type.*;


public class Rest
{
  private final String host;
  private final String path;
  private final String repo;

  private final boolean compact;
  private final String dateform;

  private final String rewclass;
  private final String valclass;

  private final String payload;
  private final String[] parts;

  private final boolean modify;

  private final boolean sppost;
  private final boolean sppatch;

  private final Config config;
  private final Logger logger;

  private String error = null;
  private Session session = null;
  private Savepoint savepoint = null;

  private final SessionState state = new SessionState();

  private static final ConcurrentHashMap<String,String> sqlfiles =
    new ConcurrentHashMap<String,String>();

  private final HashMap<String,BindValueDef> bindvalues = new HashMap<String,BindValueDef>();


  private static final TreeSet<String> commands = new TreeSet<String>();

  static
  {
    commands.add("ping");
    commands.add("call");
    commands.add("batch");
    commands.add("fetch");
    commands.add("script");
    commands.add("status");
    commands.add("execute");
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
    this.rewclass = config.getDatabase().rewrite();
    this.compact  = config.getDatabase().compact();
    this.valclass = config.getDatabase().validator();
    this.dateform = config.getDatabase().dateformat();
    this.repo     = config.getDatabase().repository();
    this.sppost   = config.getDatabase().savepoint("sppost");
    this.sppatch  = config.getDatabase().savepoint("sppatch");
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
      session.assertOpen();
      return(error(e));
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

    if (cmd.equals("execute"))
      cmd = peek(payload);

    if (error != null)
      return(error);

    switch(cmd)
    {
      case "connect" :
        response = connect(payload,batch); break;

      case "update" :
        response = update(payload,batch); break;

      case "select" :
        response = select(payload,batch); break;

      case "fetch" :
        response = fetch(payload); break;

      case "call" :
        response = call(payload,batch); break;

      case "disconnect" :
        response = disconnect(); break;

      default : return(error("Unknown command "+cmd));
    }

    if (!batch)
    {
      state.releaseAll(this);
    }

    session.assertOpen();
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

        if (method == AuthMethod.PoolToken || method == AuthMethod.OAuth)
        {
          if (!anonymous) pool = config.getDatabase().proxy();
          else            pool = config.getDatabase().anonymous();
        }

        if (error != null)
          return(error);

        if (!anonymous && username == null)
          return(error("Username must be specified"));

        this.session = new Session(method,pool,dedicated,username,secret);

        if (dedicated || method == AuthMethod.Database) this.session.connect();
        if (!dedicated && !batch && method == AuthMethod.Database) this.session.disconnect();
      }
    }
    catch (Throwable e)
    {
      return(error(e));
    }

    JSONFormatter json = new JSONFormatter();
    String sesid = encode(session.guid(),host);

    json.success(true);
    json.add("session",sesid);

    return(json.toString());
  }


  private String disconnect()
  {
    if (session == null)
      return(error("not connected"));

    try
    {
      session.disconnect();
    }
    catch (Throwable e)
    {
      return(error(e));
    }

    JSONFormatter json = new JSONFormatter();
    String sesid = encode(session.guid(),host);

    json.success(true);
    json.add("disconnected",sesid);

    return(json.toString());
  }


  private String select(JSONObject payload, boolean batch)
  {
    if (session == null)
      return(error("Not connected"));

    try
    {
      int rows = 0;
      int skip = 0;
      String name = null;
      boolean compact = this.compact;
      String dateform = this.dateform;
      boolean savepoint = getSavepoint(payload,false);

      session.ensure();

      if (payload.has("bindvalues"))
        this.getBindValues(payload.getJSONArray("bindvalues"));

      if (payload.has("rows")) rows = payload.getInt("rows");
      if (payload.has("skip")) skip = payload.getInt("skip");
      if (payload.has("cursor")) name = payload.getString("cursor");
      if (payload.has("compact")) compact = payload.getBoolean("compact");
      if (payload.has("dateformat")) dateform = payload.getString("dateformat");
      if (!batch && payload.has("savepoint")) savepoint = payload.getBoolean("savepoint");

      String sql = getStatement(payload);
      if (!session.dedicated()) name = null;

      if (error != null)
        return(error);

      SQLParser parser = new SQLParser(bindvalues,sql);

      session.closeCursor(name);

      if (!batch && savepoint)
      {
        state.lock(this,true);
        this.savepoint = session.setSavePoint();
      }

      state.lock(this,false);
      Cursor cursor = session.executeQuery(name,parser.sql(),parser.bindvalues());
      state.release(this,false);

      if (!batch && savepoint)
      {
        if (!session.releaseSavePoint(this.savepoint))
        {
          this.savepoint = null;
          session.closeCursor(cursor);
          throw new Exception("Could not release savepoint");
        }
      }

      cursor.rows = rows;
      cursor.compact = compact;
      cursor.dateformat = dateform;

      String[] columns = session.getColumnNames(cursor);
      ArrayList<Object[]> table = session.fetch(cursor,skip);

      JSONFormatter json = new JSONFormatter();

      json.success(true);
      json.add("more",!cursor.closed);

      if (compact)
      {
        json.push("columns",SimpleArray);
        json.add(columns);
        json.pop();

        json.push("rows",Matrix);
        json.add(table);
        json.pop();
      }
      else
      {
        json.push("rows",ObjectArray);
        for(Object[] row : table) json.add(columns,row);
        json.pop();
      }

      if (cursor.name == null)
        session.closeCursor(cursor);

      if (!session.dedicated() && !batch)
        session.disconnect();

      return(json.toString());
    }
    catch (Throwable e)
    {
      session.releaseSavePoint(this.savepoint,true);

      this.savepoint = null;
      state.releaseAll(this);

      return(error(e));
    }
  }


  private String update(JSONObject payload, boolean batch)
  {
    if (session == null)
      return(error("Not connected"));

    try
    {
      boolean savepoint = getSavepoint(payload,true);

      if (payload.has("bindvalues"))
        this.getBindValues(payload.getJSONArray("bindvalues"));

      String sql = getStatement(payload);

      if (error != null)
        return(error);

      SQLParser parser = new SQLParser(bindvalues,sql);

      if (!batch && savepoint)
      {
        state.lock(this,true);
        this.savepoint = session.setSavePoint();
      }

      state.lock(this,false);
      int rows = session.executeUpdate(parser.sql(),parser.bindvalues());
      state.release(this,false);

      if (!batch && savepoint)
      {
        if (!session.releaseSavePoint(this.savepoint))
        {
          this.savepoint = null;
          throw new Exception("Could not release savepoint");
        }
      }

      JSONFormatter json = new JSONFormatter();

      json.success(true);
      json.add("rows",rows);

      return(json.toString());
    }
    catch (Throwable e)
    {
      session.releaseSavePoint(this.savepoint,true);

      this.savepoint = null;
      state.releaseAll(this);

      return(error(e));
    }
  }


  private String call(JSONObject payload, boolean batch)
  {
    if (session == null)
      return(error("Not connected"));

    try
    {
      String dateconv = null;
      boolean savepoint = getSavepoint(payload,true);

      if (payload.has("dateconversion"))
        dateconv = payload.getString("dateconversion");

      if (payload.has("bindvalues"))
        this.getBindValues(payload.getJSONArray("bindvalues"));

      String sql = getStatement(payload);

      if (error != null)
        return(error);

      SQLParser parser = new SQLParser(bindvalues,sql,true);

      if (!batch && savepoint)
      {
        state.lock(this,true);
        this.savepoint = session.setSavePoint();
      }

      state.lock(this,false);
      ArrayList<NameValuePair<Object>> values = session.executeCall(parser.sql(),parser.bindvalues(),dateconv);
      state.release(this,false);

      if (!batch && savepoint)
      {
        if (!session.releaseSavePoint(this.savepoint))
        {
          this.savepoint = null;
          throw new Exception("Could not release savepoint");
        }
      }

      JSONFormatter json = new JSONFormatter();

      json.success(true);

      for(NameValuePair<Object> nvp : values)
        json.add(nvp.getName(),nvp.getValue());

      return(json.toString());
    }
    catch (Throwable e)
    {
      session.releaseSavePoint(this.savepoint,true);

      this.savepoint = null;
      state.releaseAll(this);

      return(error(e));
    }
  }


  private String fetch(JSONObject payload)
  {
    if (session == null)
      return(error("Not connected"));

    try
    {
      boolean close = false;
      JSONFormatter json = new JSONFormatter();
      String name = payload.getString("cursor");

      Cursor cursor = session.getCursor(name);
      if (payload.has("close")) close = payload.getBoolean("close");

      if (cursor == null)
        return(error("Cursor \'"+name+"\' does not exist"));

      if (close)
      {
        session.closeCursor(name);
        json.success(true);
        json.add("closed",true);
        return(json.toString());
      }

      String[] columns = session.getColumnNames(cursor);
      ArrayList<Object[]> table = session.fetch(cursor,0);

      json.success(true);
      json.add("more",!cursor.closed);

      if (cursor.compact)
      {
        json.push("columns",SimpleArray);
        json.add(columns);
        json.pop();

        json.push("rows",Matrix);
        json.add(table);
        json.pop();
      }
      else
      {
        json.push("rows",ObjectArray);
        for(Object[] row : table) json.add(columns,row);
        json.pop();
      }

      return(json.toString());
    }
    catch (Throwable e)
    {
      return(error(e));
    }
  }


  private String peek(JSONObject payload)
  {
    String sql = getStatement(payload);

    if (error != null)
      return(error);

    if (sql.length() > 6)
    {
      String cmd = sql.substring(0,7).toLowerCase();

      if (cmd.equals("select ")) return("select");
      if (cmd.equals("insert ")) return("update");
      if (cmd.equals("update ")) return("update");
      if (cmd.equals("delete ")) return("update");
    }

    return("call");
  }


  private String getStatement(JSONObject payload)
  {
    String file = "@";
    String sql = payload.getString("sql");

    if (sql.startsWith(file))
    {
      String fname = sql.substring(file.length());
      if (!fname.startsWith(File.separator)) fname = File.separator + fname;

      fname = repo + fname;
      sql = sqlfiles.get(fname);
      if (sql != null) return(sql);

      File f = new File(fname);

      try
      {
        String path = f.getCanonicalPath();

        if (!path.startsWith(repo+File.separator))
          return(error("Illegal path '"+path+"'. File must be located in repository"));

        byte[] content = new byte[(int) f.length()];
        FileInputStream in = new FileInputStream(f);
        int read = in.read(content);
        in.close();

        if (read != content.length)
          return(error("Could not read '"+f.getCanonicalPath()+"'"));

        sql = new String(content);
        sqlfiles.put(fname,sql);
      }
      catch (Exception e)
      {
        return(error(e));
      }
    }

    return(sql);
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


  private void getBindValues(JSONArray values)
  {
    for (int i = 0; i < values.length(); i++)
    {
      JSONObject bvalue = values.getJSONObject(i);

      Object value = null;
      boolean outval = false;

      String name = bvalue.getString("name");
      String type = bvalue.getString("type");

      if (!bvalue.has("value")) outval = true;
      else value = bvalue.get("value");

      BindValueDef bindvalue = new BindValueDef(name,type,outval,value);

      if (value != null && bindvalue.isDate())
      {
        if (value instanceof Long)
          value = new Date((Long) value);
      }

      this.bindvalues.put(name,bindvalue);
    }
  }


  private boolean getSavepoint(JSONObject payload, boolean modify)
  {
    boolean defaults = modify ? sppatch : sppost;

    try
    {
      boolean savepoint = defaults;

      if (payload != null && payload.has("savepoint"))
        savepoint = payload.getBoolean("savepoint");

      return(savepoint);
    }
    catch (Throwable e)
    {
      error(e);
      return(defaults);
    }
  }


  private String divert()
  {
    String cmd = null;
    boolean ses = false;

    if (commands.contains(parts[0].toLowerCase()))
      cmd = parts[0].toLowerCase();

    if (cmd == null && parts.length > 1)
    {
      ses = true;
      String sesid = decode(parts[0],host);

      this.session = Session.get(sesid);

      if (commands.contains(parts[1].toLowerCase()))
        cmd = parts[1].toLowerCase();
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


  private static String encode(String data, String salt)
  {
    byte[] bdata = data.getBytes();
    byte[] bsalt = salt.getBytes();

    for (int i = 0; i < bdata.length; i++)
    {
      byte s = bsalt[i % bsalt.length];
      bdata[i] = (byte) (bdata[i] ^ s);
    }

    bdata = Base64.getEncoder().encode(bdata);

    int len = bdata.length;
    while(bdata[len-1] == '=') len--;

    return(new String(bdata,0,len));
  }


  private static String decode(String data, String salt)
  {
    byte[] bsalt = salt.getBytes();
    while(data.length() % 4 != 0) data += "=";
    byte[] bdata = Base64.getDecoder().decode(data);

    for (int i = 0; i < bdata.length; i++)
    {
      byte s = bsalt[i % bsalt.length];
      bdata[i] = (byte) (bdata[i] ^ s);
    }

    return(new String(bdata));
  }


  private String error(Throwable e)
  {
    JSONFormatter json = new JSONFormatter();

    json.set(e);
    json.success(false);

    this.error = json.toString();
    return(this.error);
  }


  private String error(String message)
  {
    JSONFormatter json = new JSONFormatter();

    json.success(false);
    json.add("message",message);

    this.error = json.toString();
    return(this.error);
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

        shared = 0;
        exclusive = false;
      }
      catch (Throwable e)
      {
        rest.error(e);
      }
    }
  }
}