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
import java.util.ArrayList;
import org.json.JSONObject;
import org.json.JSONTokener;
import database.js.config.Config;


public class Request
{
  public final String cmd;
  public final String path;
  public final String func;
  public final String session;
  public final JSONObject payload;
  public final ArrayList<String> args;

  private static final TreeSet<String> function = new TreeSet<String>();
  private static final TreeSet<String> commands = new TreeSet<String>();

  static
  {
    function.add("map");
    function.add("run");
    function.add("call");
    function.add("batch");
    function.add("merge");
    function.add("script");
    function.add("fetch");
    function.add("select");
    function.add("insert");
    function.add("update");
    function.add("delete");
  }

  static
  {
    commands.add("ping");
    commands.add("exec");
    commands.add("status");
    commands.add("commit");
    commands.add("connect");
    commands.add("rollback");
    commands.add("disconnect");
  }


  public static void main(String[] args) throws Exception
  {
    String payload =

    "{" +
    "  \"sql\": \"select bla   \"" +
    "}";

    Rest rest = new Rest(new Config(false),false,"localhost");
    Request request = new Request(rest,"/connect",payload);

    System.out.println(request+" "+request.func);
  }


  public Request(Rest rest, String path, String payload) throws Exception
  {
    this(rest,path,parse(payload));
  }


  public Request(Rest rest, String path, JSONObject payload) throws Exception
  {
    this.path = path;
    this.payload = payload;
    this.args = new ArrayList<String>();

    String cmd = null;
    String func = null;
    String session = null;

    if (path.startsWith("/"))
      path = path.substring(1);

    String[] args = path.split("/");

    int pos = 0;
    for (int i = 0; i < args.length; i++)
    {
      if (function.contains(args[i]))
      {
        pos = i;
        cmd = "exec";
        func = args[i];
        break;
      }
      
      String syn = args[i];

      if (syn.equals("sql")) syn = "exec";
      if (syn.equals("execute")) syn = "exec";

      if (commands.contains(syn))
      {
        pos = i;
        cmd = syn;
        break;
      }
    }

    if (pos < 0 || pos > 1 || cmd == null)
      throw new Exception("Unknown rest path \""+path+"\"");

    if (func == null)
    {
      if (pos < args.length - 1)
      {
        if (function.contains(args[pos+1]))
          func = args[++pos];
      }
    }

    if (func == null && cmd.equals("exec"))
      func = peek(rest,payload);

    if (pos == 1)
      session = rest.decode(args[0]);

    for (int i = pos+1; i < args.length; i++)
      this.args.add(args[i]);

    this.cmd = cmd;
    this.func = func;
    this.session = session;
  }


  public static JSONObject parse(String payload) throws Exception
  {
    if (payload == null)
      payload = "{}";

    try
    {
      JSONTokener tokener = new JSONTokener(payload);
      return(new JSONObject(tokener));
    }
    catch (Throwable e)
    {
      throw new Exception("Could not parse json payload: ["+payload+"]");
    }
  }
  
  
  public String nvlfunc()
  {
    if (func == null) return("");
    else              return(func);
  }


  @Override
  public String toString()
  {
    String str = "";

    if (session == null) str += "[]/";
    else                 str += "[" + session + "]/";

    str += this.cmd;
    if (func != null) str += "/" + func;
    for(String arg : args) str += " <" + arg + ">";
    return(str);
  }


  private String peek(Rest rest, JSONObject payload) throws Exception
  {
    if (payload.has("batch"))
      return("batch");

    if (payload.has("script"))
      return("script");

    String sql = rest.getStatement(payload);
    if (sql == null) throw new Exception("Attribute \"sql\" is missing");

    if (sql.length() > 6)
    {
      String cmd = sql.substring(0,7).toLowerCase();

      if (cmd.equals("merge " )) return("update");
      if (cmd.equals("upsert ")) return("update");
      if (cmd.equals("select ")) return("select");
      if (cmd.equals("insert ")) return("update");
      if (cmd.equals("update ")) return("update");
      if (cmd.equals("delete ")) return("update");
    }

    return("call");
  }
}