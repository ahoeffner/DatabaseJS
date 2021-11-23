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

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


public class JSONFormatter
{
  private Content content = null;
  private final static Logger logger = Logger.getLogger("rest");


  public static void main(String[] args)
  {
    JSONFormatter format = new JSONFormatter();
    String[] cols = {"col1","col2","col3"};

    format.add("message","everything is fine");

    format.push("employee");
    format.add("first_name","Alex");
    format.add("last_name","Høffner");
    format.pop();

    format.push("rows",true);
    format.add(cols,new String[] {"1","2","3"});
    format.add(cols,new String[] {"4","5","6"});
    format.pop();

    format.add("last",null);

    format.success(true);
    System.out.println(format);



    System.out.println();
    System.out.println();

    format = new JSONFormatter();

    format.push("connect");
    format.add("session","w828e828e");
    format.success(true);
    format.pop();

    format.push("query");
    format.push("rows",true);
    format.add(cols,new String[] {"1","2","3"});
    format.add(cols,new String[] {"4","5","6"});
    format.pop();
    format.success(true);
    format.pop();

    System.out.println(format);
  }


  public JSONFormatter()
  {
    this(false);
  }


  public JSONFormatter(boolean array)
  {
    content = new Content(array);
  }


  public void success(boolean success)
  {
    content.status(success);
  }


  public void pop()
  {
    content = content.parent;
  }


  public void push(String name)
  {
    content = content.push(name,false);
  }


  public void push(String name, boolean array)
  {
    content = content.push(name,array);
  }


  public void set(Throwable err)
  {
    content.set(err);
    logger.log(Level.WARNING,err.getMessage(),err);
  }


  public void add(String name, String value)
  {
    content.add(name,value);
  }


  public void add(String[] name, String[] value)
  {
    content.add(name,value);
  }


  @Override
  public String toString()
  {
    Content content = this.content;

    while(content.parent != null)
      content = content.parent;

    return(content.persist());
  }


  private static class Content
  {
    private final String name;
    private final boolean array;
    private final Content parent;

    private final static String nl =
      System.lineSeparator();

    private final ArrayList<Object> content =
      new ArrayList<Object>();

    Content(boolean array)
    {
      this(null,null,array);
    }

    Content(Content parent, String name, boolean array)
    {
      this.name = name;
      this.array = array;
      this.parent = parent;
    }

    Content push(String name, boolean array)
    {
      Content next = new Content(this,name,array);
      content.add(next);
      return(next);
    }

    void status(boolean success)
    {
      String name = "status";
      String value = success ? "ok" : "failed";
      content.add(0,new String[] {name,value});
    }

    void add(String name, String value)
    {
      content.add(new String[] {name,value});
    }

    void add(String[] name, String[] value)
    {
      content.add(new String[][] {name,value});
    }

    void set(Throwable err)
    {
      String message = err.getMessage();
      if (message == null) message = "An unexpected error has occured";
      add("message",message);
    }

    String escape(String str)
    {
      if (str == null)
        return("null");

      str = JSONObject.quote(str);
      return(str);
    }


    String quote(String str)
    {
      return("\""+str+"\"");
    }


    String persist()
    {
      return(persist(this,0));
    }


    private String persist(Content node, int level)
    {
      if (node.array)
        return(persistArray(node,level));

      String lev = "";
      if (level > 0) lev = String.format("%"+(2*level)+"s"," ");

      String str = "";
      String ind = lev + "  ";

      if (level > 0) str += nl;
      str += lev + "{" + nl;

      int elements = node.content.size();

      for (int i = 0; i < elements; i++)
      {
        Object elem = node.content.get(i);

        String comm = "";
        if (i < elements - 1) comm = ",";

        String newl = "";
        if (i > 0) newl = nl;

        if (elem instanceof Content)
        {
          Content next = (Content) elem;
          str += newl + ind + quote(next.name)+":";
          str += persist(next,level+1)+comm;
        }
        else
        {
          String[] nvp = (String[]) elem;
          str += newl + ind + quote(nvp[0])+": "+escape(nvp[1])+comm;
        }
      }

      str += nl + lev + "}";
      return(str);
    }


    String persistArray(Content node, int level)
    {
      String lev = "";
      if (level > 0) lev = String.format("%"+(2*level)+"s"," ");

      String str = "";
      String ind = lev + "  ";

      if (level > 0) str += nl;
      str += lev + "[" + nl;

      int elements = node.content.size();

      for (int i = 0; i < elements; i++)
      {
        Object elem = node.content.get(i);

        String comm = "";
        if (i < elements - 1) comm = ",";

        String newl = "";
        if (i > 0) newl = nl;

        if (elem instanceof Content)
        {
          Content next = (Content) elem;
          str += newl + ind + quote(next.name)+":";
          str += persist(next,level+1)+comm;
        }
        else
        {
          String[][] row = (String[][]) elem;

          String[] names = row[0];
          String[] values = row[1];

          str += newl + ind + "{";

          for (int j = 0; j < names.length; j++)
          {
            String next = "";
            if (j < names.length - 1) next = ",";

            String name = names[j];
            String value = values[j];

            str += quote(name)+": "+escape(value)+next;
          }

          str += "}" + comm;
        }
      }

      str += nl + lev + "]";
      return(str);
    }
  }
}