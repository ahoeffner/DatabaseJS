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
import java.util.HashMap;
import org.json.JSONArray;
import java.sql.Savepoint;
import org.json.JSONObject;
import java.util.ArrayList;
import java.io.FileInputStream;
import database.js.config.Config;
import database.js.database.Pool;
import database.js.security.OAuth;
import database.js.custom.SQLRewriter;
import database.js.database.SQLParser;
import database.js.database.BindValue;
import database.js.custom.SQLValidator;
import database.js.database.AuthMethod;
import database.js.database.BindValueDef;
import database.js.database.NameValuePair;
import java.util.concurrent.ConcurrentHashMap;
import static database.js.handlers.rest.JSONFormatter.Type.*;


public class Rest
{
  private final String host;
  private final String repo;
  private final Config config;
  
  private final boolean compact;
  private final String dateform;

  private final boolean modify;
  private final boolean sppost;
  private final boolean sppatch;

  private boolean failed = false;
  private Session session = null;
  private Savepoint savepoint = null;

  private final SQLRewriter rewriter;
  private final SQLValidator validator;

  private final SessionState state = new SessionState();
  private final HashMap<String,BindValueDef> bindvalues = new HashMap<String,BindValueDef>();
  private static final ConcurrentHashMap<String,String> sqlfiles = new ConcurrentHashMap<String,String>();


  public Rest(Config config, boolean modify, String host) throws Exception
  {
    this.host      = host;
    this.config    = config;
    this.modify    = modify;
    this.compact   = config.getDatabase().compact;
    this.rewriter  = config.getDatabase().rewriter;
    this.validator = config.getDatabase().validator;
    this.dateform  = config.getDatabase().dateformat;
    this.repo      = config.getDatabase().repository;
    this.sppost    = config.getDatabase().savepoint("sppost");
    this.sppatch   = config.getDatabase().savepoint("sppatch");
  }


  public String execute(String path, String payload)
  {
    try
    {
      Request request = new Request(this,path,payload);

      if (request.session != null)
        this.session = Session.get(request.session);

      if (request.func.equals("batch"))
        return(batch(request.payload));

      if (request.func.equals("script"))
        return(script(request.payload));

      return(exec(request,false));
    }
    catch (Throwable e)
    {
      return(error(e,false));
    }
  }


  private String batch(JSONObject payload)
  {
    boolean savepoint = getSavepoint(payload,false);

    try
    {
      JSONArray services = payload.getJSONArray("batch");
      if (payload.has("savepoint")) savepoint = payload.getBoolean("savepoint");

      if (savepoint)
      {
        state.lock(this,true);
        this.savepoint = session.setSavePoint();
      }

      String result = null;
      String response = "[\n";

      for (int i = 0; i < services.length(); i++)
      {
        String cont = "\n";
        if (i < services.length() - 1) cont += ",\n";

        JSONObject spload = null;
        JSONObject service = services.getJSONObject(i);

        String path = service.getString("path");

        if (service.has("payload"))
          spload = service.getJSONObject("payload");

        Request request = new Request(this,path,spload);

        if (request.nvlfunc().equals("map"))
        {
          map(result,spload);
          continue;
        }

        result = exec(request,true);    
        response += result + cont;
        
        if (failed) break;
      }

      response += "]";

      if (savepoint && session != null)
      {
        if (!session.releaseSavePoint(this.savepoint))
        {
          this.savepoint = null;
          throw new Exception("Could not release savepoint");
        }

        state.release(this,true);
      }

      if (!session.dedicated())
        session.disconnect();

      return(response);
    }
    catch (Throwable e)
    {
      if (session != null)
      {
        session.releaseSavePoint(this.savepoint,true);
        this.savepoint = null;        
      }

      state.releaseAll(this);
      boolean fatal = session.fatal();

      if (!fatal && !session.dedicated())
        session.disconnect();

      return(error(e,fatal));
    }
  }


  private String script(JSONObject payload)
  {
    boolean savepoint = getSavepoint(payload,false);

    try
    {
      JSONArray services = payload.getJSONArray("script");
      if (payload.has("savepoint")) savepoint = payload.getBoolean("savepoint");

      if (savepoint)
      {
        state.lock(this,true);
        this.savepoint = session.setSavePoint();
      }

      String result = null;
      for (int i = 0; i < services.length(); i++)
      {
        String cont = "\n";
        if (i < services.length() - 1) cont += ",\n";

        JSONObject spload = null;
        JSONObject service = services.getJSONObject(i);

        String path = service.getString("path");

        if (service.has("payload"))
          spload = service.getJSONObject("payload");

        Request request = new Request(this,path,spload);

        if (request.nvlfunc().equals("map"))
        {
          map(result,spload);
          continue;
        }

        result = exec(request,true);
        if (failed) break;
      }

      if (savepoint && session != null)
      {
        if (!session.releaseSavePoint(this.savepoint))
        {
          this.savepoint = null;
          throw new Exception("Could not release savepoint");
        }

        state.release(this,true);
      }

      if (!session.dedicated())
        session.disconnect();

      return(result);
    }
    catch (Throwable e)
    {
      if (session != null)
      {
        session.releaseSavePoint(this.savepoint,true);
        this.savepoint = null;        
      }

      state.releaseAll(this);
      boolean fatal = session.fatal();

      if (!fatal && !session.dedicated())
        session.disconnect();

      return(error(e,fatal));
    }
  }


  private String exec(Request request, boolean batch)
  {
    String response = null;

    switch(request.cmd)
    {
      case "ping" :
        response = ping(request.payload); break;

      case "status" :
        response = ping(request.payload); break;

      case "connect" :
        response = connect(request.payload,batch); break;

      case "commit" :
        response = commit(); break;

      case "rollback" :
        response = rollback(); break;

      case "disconnect" :
        response = disconnect(); break;

      case "exec" :
        {
          switch(request.func)
          {
            case "run" :
              response = run(request.payload,batch); break;

            case "merge" :
              response = update(request.payload,batch); break;

            case "insert" :
              response = update(request.payload,batch); break;

            case "update" :
              response = update(request.payload,batch); break;

            case "select" :
              response = select(request.payload,batch); break;

            case "fetch" :
              response = fetch(request.payload); break;

            case "call" :
              response = call(request.payload,batch); break;

            default : return(error("Unknown command "+request));
          }

          break;
        }

      default : return(error("Unknown command "+request.cmd));
    }

    if (!batch)
    {
      state.releaseAll(this);
    }

    return(response);
  }


  private String ping(JSONObject payload)
  {
    JSONFormatter json = new JSONFormatter();
    json.success(true);
    return(json.toString());
  }


  private String status(JSONObject payload)
  {
    JSONFormatter json = new JSONFormatter();
    json.success(true);
    return(json.toString());
  }

  private String connect(JSONObject payload, boolean batch)
  {
    Pool pool = null;
    String secret = null;
    String username = null;
    AuthMethod method = null;
    boolean dedicated = false;
    boolean privateses = true;

    try
    {
      if (payload.has("username"))
        username = payload.getString("username");

      if (payload.has("private"))
        privateses = payload.getBoolean("private");

      if (payload.has("dedicated"))
        dedicated = payload.getBoolean("dedicated");

      if (payload.has("auth.secret"))
        secret = payload.getString("auth.secret");

      if (payload.has("auth.method"))
      {
        String meth = payload.getString("auth.method");

        switch(meth.toLowerCase())
        {
          case "oauth"    : method = AuthMethod.OAuth; break;
          case "database" : method = AuthMethod.Database; break;
          case "token"    : method = AuthMethod.PoolToken; break;

          default: return(error("Unknown authentication method "+meth));
        }

        if (method == AuthMethod.OAuth)
        {
          username = OAuth.getUserName(secret);

          if (username == null)
            return(error("OAuth authentication failed"));
        }

        boolean anonymous = username == null;
        if (method == AuthMethod.PoolToken || method == AuthMethod.OAuth)
        {
          if (!anonymous) pool = config.getDatabase().proxy;
          else            pool = config.getDatabase().anonymous;

          if (pool == null)
            return(error("Connection pool not configured"));
        }

        this.session = new Session(method,pool,dedicated,username,secret);

        if (dedicated || method == AuthMethod.Database) this.session.connect();
        if (!dedicated && !batch && method == AuthMethod.Database) this.session.disconnect();
      }
    }
    catch (Throwable e)
    {
      return(error(e,false));
    }

    JSONFormatter json = new JSONFormatter();
    String sesid = encode(privateses,session.guid(),host);

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
      session.remove();
    }
    catch (Throwable e)
    {
      return(error(e,true));
    }

    JSONFormatter json = new JSONFormatter();

    json.success(true);
    json.add("disconnected",true);

    return(json.toString());
  }


  private String run(JSONObject payload, boolean batch)
  {
    boolean success = false;
    
    if (session == null)
      return(error("not connected"));

    try
    {

      String sql = getStatement(payload);
      if (sql == null) return(error("Attribute \"sql\" is missing"));
      success = session.execute(sql);
    }
    catch (Throwable e)
    {
      return(error(e,true));
    }

    JSONFormatter json = new JSONFormatter();
    json.success(true);
    json.add("result",success);
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
      String curname = null;
      boolean compact = this.compact;
      String dateform = this.dateform;
      boolean savepoint = getSavepoint(payload,false);

      session.ensure();

      if (payload.has("bindvalues"))
        this.getBindValues(payload.getJSONArray("bindvalues"));

      if (payload.has("rows")) rows = payload.getInt("rows");
      if (payload.has("skip")) skip = payload.getInt("skip");

      if (payload.has("dateformat"))
      {
        if (payload.isNull("dateformat")) dateform = null;
        else   dateform = payload.getString("dateformat");
      }

      if (payload.has("compact")) compact = payload.getBoolean("compact");
      if (!batch && payload.has("savepoint")) savepoint = payload.getBoolean("savepoint");
      if (session.dedicated() && payload.has("cursor")) curname = payload.getString("cursor");

      String sql = getStatement(payload);
      if (sql == null) return(error("Attribute \"sql\" is missing"));

      SQLParser parser = new SQLParser(bindvalues,sql);

      sql = parser.sql();
      ArrayList<BindValue> bindvalues = parser.bindvalues();

      if (rewriter != null)
        sql = rewriter.rewrite(sql,bindvalues);

      if (validator != null)
        validator.validate(sql,bindvalues);

      if (!batch && savepoint)
      {
        state.lock(this,true);
        this.savepoint = session.setSavePoint();
      }

      session.closeCursor(curname);

      if (!batch && savepoint)
      {
        state.lock(this,true);
        this.savepoint = session.setSavePoint();
      }

      state.lock(this,false);
      Cursor cursor = session.executeQuery(curname,sql,bindvalues);
      state.release(this,false);

      if (!batch && savepoint)
      {
        if (!session.releaseSavePoint(this.savepoint))
        {
          this.savepoint = null;
          session.closeCursor(cursor);
          throw new Exception("Could not release savepoint");
        }

        state.release(this,true);
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

      if (!batch && !session.dedicated())
        session.disconnect();

      return(json.toString());
    }
    catch (Throwable e)
    {
      session.releaseSavePoint(this.savepoint,true);
      this.savepoint = null;

      state.releaseAll(this);
      boolean fatal = session.fatal();

      if (!fatal && !session.dedicated())
        session.disconnect();

      return(error(e,fatal));
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
      if (sql == null) return(error("Attribute \"sql\" is missing"));

      SQLParser parser = new SQLParser(bindvalues,sql);

      sql = parser.sql();
      ArrayList<BindValue> bindvalues = parser.bindvalues();

      if (rewriter != null)
        sql = rewriter.rewrite(sql,bindvalues);

      if (validator != null)
        validator.validate(sql,bindvalues);

      if (!batch && savepoint)
      {
        state.lock(this,true);
        this.savepoint = session.setSavePoint();
      }

      state.lock(this,false);
      int rows = session.executeUpdate(sql,bindvalues);
      state.release(this,false);

      if (!batch && savepoint)
      {
        if (!session.releaseSavePoint(this.savepoint))
        {
          this.savepoint = null;
          throw new Exception("Could not release savepoint");
        }

        state.release(this,true);
      }

      JSONFormatter json = new JSONFormatter();

      json.success(true);
      json.add("rows",rows);

      if (!batch && !session.dedicated())
        session.disconnect();

      return(json.toString());
    }
    catch (Throwable e)
    {
      session.releaseSavePoint(this.savepoint,true);
      this.savepoint = null;

      state.releaseAll(this);
      boolean fatal = session.fatal();

      if (!fatal && !session.dedicated())
        session.disconnect();

      return(error(e,fatal));
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
      if (sql == null) return(error("Attribute \"sql\" is missing"));

      SQLParser parser = new SQLParser(bindvalues,sql,true);

      sql = parser.sql();
      ArrayList<BindValue> bindvalues = parser.bindvalues();

      if (rewriter != null)
        sql = rewriter.rewrite(sql,bindvalues);

      if (validator != null)
        validator.validate(sql,bindvalues);

      if (!batch && savepoint)
      {
        state.lock(this,true);
        this.savepoint = session.setSavePoint();
      }

      state.lock(this,false);
      ArrayList<NameValuePair<Object>> values = session.executeCall(sql,bindvalues,dateconv);
      state.release(this,false);

      if (!batch && savepoint)
      {
        if (!session.releaseSavePoint(this.savepoint))
        {
          this.savepoint = null;
          throw new Exception("Could not release savepoint");
        }

        state.release(this,true);
      }

      JSONFormatter json = new JSONFormatter();

      json.success(true);

      for(NameValuePair<Object> nvp : values)
        json.add(nvp.getName(),nvp.getValue());

      if (!batch && !session.dedicated())
        session.disconnect();

      return(json.toString());
    }
    catch (Throwable e)
    {
      session.releaseSavePoint(this.savepoint,true);
      this.savepoint = null;

      state.releaseAll(this);
      boolean fatal = session.fatal();

      if (!fatal && !session.dedicated())
        session.disconnect();

      return(error(e,fatal));
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
      boolean fatal = session.fatal();
      return(error(e,fatal));
    }
  }


  private String commit()
  {
    JSONFormatter json = new JSONFormatter();
    json.success(true);
    return(json.toString());
  }


  private String rollback()
  {
    JSONFormatter json = new JSONFormatter();
    json.success(true);
    return(json.toString());
  }


  private void map(String latest, JSONObject payload) throws Exception
  {
    JSONArray rows = null;
    ArrayList<String> cols = null;
    JSONObject last = Request.parse(latest);

    if (last.has("rows"))
      rows = last.getJSONArray("rows");

    if (last.has("columns"))
    {
      cols = new ArrayList<String>();
      JSONArray columns = last.getJSONArray("columns");
      for (int i = 0; i < columns.length(); i++)
        cols.add((String) columns.get(i));
    }

    String[] bindvalues = JSONObject.getNames(payload);

    if (rows == null)
    {
      // Previous was procedure
      for (int i = 0; i < bindvalues.length; i++)
      {
        String bindv = bindvalues[i];
        String pointer = payload.getString(bindv).trim();
        this.bindvalues.put(bindv,new BindValueDef(bindv,last.get(pointer)));
      }
    }
    else
    {
      // Previous was select
      for (int i = 0; i < bindvalues.length; i++)
      {
        int row = 0;
        Object value = null;
        String bindv = bindvalues[i];
        String pointer = payload.getString(bindv).trim();

        if (pointer.endsWith("]"))
        {
          int pos = pointer.lastIndexOf('[');

          if (pos > 0)
          {
            row = Integer.parseInt(pointer.substring(pos+1,pointer.length()-1));
            pointer = pointer.substring(0,pos);
          }
        }

        if (cols == null)
        {
          JSONObject record = (JSONObject) rows.get(row);
          value = record.get(pointer);
        }
        else
        {
          int col = -1;

          for (int j = 0; j < cols.size(); j++)
          {
            if (cols.get(j).equals(pointer))
            {
              col = j;
              break;
            }
          }

          JSONArray record = (JSONArray) rows.get(row);
          value = record.get(col);
        }

        this.bindvalues.put(bindv,new BindValueDef(bindv,value));
      }
    }
  }


  String getStatement(JSONObject payload) throws Exception
  {
    if (!payload.has("sql"))
      return(null);

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

      String path = f.getCanonicalPath();

      if (!path.startsWith(repo+File.separator))
        throw new Exception("Illegal path '"+path+"'. File must be located in repository");

      byte[] content = new byte[(int) f.length()];
      FileInputStream in = new FileInputStream(f);
      int read = in.read(content);
      in.close();

      if (read != content.length)
        throw new Exception("Could not read '"+f.getCanonicalPath()+"'");

      sql = new String(content);
      sqlfiles.put(fname,sql);
    }

    return(sql);
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
      error(e,false);
      return(defaults);
    }
  }


  String encode(boolean priv, String data)
  {
    return(encode(priv,data,host));
  }


  String decode(String data)
  {
    return(decode(data,host));
  }


  static String encode(boolean priv, String data, String salt)
  {
    byte[] bdata = data.getBytes();
    byte[] bsalt = salt.getBytes();

    byte indicator;
    long ran = System.currentTimeMillis() % 25;

    if (priv) indicator = (byte) ('a' + ran);
    else      indicator = (byte) ('A' + ran);

    byte[] token = new byte[bdata.length+1];

    token[0] = indicator;
    System.arraycopy(bdata,0,token,1,bdata.length);

    if (priv)
    {
      for (int i = 1; i < token.length; i++)
      {
        byte s = bsalt[i % bsalt.length];
        token[i] = (byte) (token[i] ^ s);
      }
    }

    token = Base64.getEncoder().encode(token);

    int len = token.length;
    while(token[len-1] == '=') len--;

    return(new String(token,0,len));
  }


  static String decode(String data, String salt)
  {
    byte[] bsalt = salt.getBytes();
    while(data.length() % 4 != 0) data += "=";

    byte[] bdata = Base64.getDecoder().decode(data);

    byte indicator = bdata[0];
    boolean priv = (indicator >= 'a' && indicator <= 'z');

    byte[] token = new byte[bdata.length-1];
    System.arraycopy(bdata,1,token,0,token.length);

    if (priv)
    {
      for (int i = 0; i < token.length; i++)
      {
        byte s = bsalt[(i+1) % bsalt.length];
        token[i] = (byte) (token[i] ^ s);
      }
    }

    return(new String(token));
  }


  private String error(Throwable e, boolean fatal)
  {
    failed = true;
    JSONFormatter json = new JSONFormatter();

    json.set(e);
    json.success(false);
    json.fatal(fatal,"disconnected");

    return(json.toString());
  }


  private String error(String message)
  {
    failed = true;
    JSONFormatter json = new JSONFormatter();

    json.success(false);
    json.add("message",message);

    return(json.toString());
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
        rest.error(e,false);
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
        rest.error(e,false);
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
        rest.error(e,false);
      }
    }
  }
}