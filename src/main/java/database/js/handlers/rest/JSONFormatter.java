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
    JSONFormatter format = null;
    
    /*
    format = new JSONFormatter();
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
    
    String[][] rows = new String[][]
    {
      {"1","2","3"},
      {"4","5","6"},
    };
    
    ArrayList<Object[]> list = new ArrayList<Object[]>();
    list.add(new Object[] {"1","2","3"});
    list.add(new Object[] {"4","5","6"});
    
    format = new JSONFormatter();
    format.success(true);
    format.push("columns",Type.SimpleArray);
    format.add(new String[] {"col1","col2"});
    format.pop();
    format.push("columns",Type.Matrix);
    format.add(list);
    format.pop();
    format.add("message","fine");

    System.out.println(format);
    */

    format = new JSONFormatter();
    String[] columns;
  }


  public JSONFormatter()
  {
    this(Type.Object);
  }


  public JSONFormatter(Type type)
  {
    content = new Content(type);
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
    content = content.push(name,Type.Object);
  }


  public void push(String name, Type type)
  {
    content = content.push(name,type);
  }


  public void set(Throwable err)
  {
    content.set(err);
    logger.log(Level.WARNING,err.getMessage(),err);
  }


  public void add(Object[] values)
  {
    content.add(values);
  }


  @SuppressWarnings("unchecked")
  public void add(ArrayList<Object[]> list)
  {
    content.add(list.toArray(new Object[0][]));
  }


  public void add(String name, Object value)
  {
    content.add(name,value);
  }


  public void add(String[] name, Object[] value)
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
    private final Type type;
    private final String name;
    private final Content parent;

    private final static String nl =
      System.lineSeparator();

    private final ArrayList<Object> content =
      new ArrayList<Object>();

    Content()
    {
      this(null,Type.Object,null);
    }

    Content(Type type)
    {
      this(null,type,null);
    }

    private Content(Content parent, Type type, String name)
    {
      this.name = name;
      this.type = type;
      this.parent = parent;
    }

    Content push(String name, Type type)
    {
      Content next = new Content(this,type,name);
      content.add(next);
      return(next);
    }

    void status(boolean success)
    {
      String name = "status";
      String value = success ? "ok" : "failed";
      content.add(0,new String[] {name,value});
    }

    void add(Object[] array)
    {
      content.add(array);
    }

    void add(String name, Object value)
    {
      content.add(new Object[] {name,value});
    }

    void add(String[] name, Object[] value)
    {
      content.add(new Object[][] {name,value});
    }

    void set(Throwable err)
    {
      String message = err.getMessage();
      if (message == null) message = "An unexpected error has occured";
      add("message",message);
    }

    String escape(Object str)
    {
      if (str == null)
        return("null");

      str = JSONObject.quote(str.toString());
      return(str.toString());
    }


    String quote(Object str)
    {
      return("\""+str+"\"");
    }


    String persist()
    {
      return(persist(this,0));
    }


    private String persist(Content node, int level)
    {
      if (node.type == Type.Matrix)
        return(persistMatrix(node,level));    

      if (node.type == Type.SimpleArray)
        return(persistSimpleArray(node,level));        

      if (node.type == Type.ObjectArray)
        return(persistObjectArray(node,level));    

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
          Object[] nvp = (Object[]) elem;
          str += newl + ind + quote(nvp[0])+": "+escape(nvp[1])+comm;
        }
      }

      str += nl + lev + "}";
      return(str);
    }


    String persistObjectArray(Content node, int level)
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
          Object[][] row = (String[][]) elem;

          Object[] names = row[0];
          Object[] values = row[1];

          str += newl + ind + "{";

          for (int j = 0; j < names.length; j++)
          {
            String next = "";
            if (j < names.length - 1) next = ",";

            Object name = names[j];
            Object value = values[j];

            str += quote(name)+": "+escape(value)+next;
          }

          str += "}" + comm;
        }
      }

      str += nl + lev + "]";
      return(str);
    }


    String persistSimpleArray(Content node, int level)
    {
      String lev = "";
      if (level > 0) lev = String.format("%"+(2*level)+"s"," ");

      String str = lev + "[";

      Object elem = node.content.get(0);
      Object[] values = (String[]) elem;

      for (int j = 0; j < values.length; j++)
      {
        String next = "";
        if (j < values.length - 1) next = ",";

        Object value = values[j];
        str += escape(value)+next;
      }

      str += "]";
      return(str);
    }


    String persistMatrix(Content node, int level)
    {
      String lev = "";
      if (level > 0) lev = String.format("%"+(2*level)+"s"," ");

      String ind = lev + "  ";
      String str = nl + lev + "[";

      Object[][] rows = (Object[][]) node.content.get(0);

      for (int i = 0; i < rows.length; i++)
      {
        String comm = "";
        if (i < rows.length - 1) comm = ",";
        
        str += nl + ind + "[";

        Object[] cols = rows[i];
        for (int j = 0; j < cols.length; j++)
        {
          String next = "";
          if (j < cols.length-1) next = ",";
          str += escape(cols[j])+next;          
        }
        
        str += "]" + comm;
      }

      str += nl + lev + "]";
      return(str);
    }
  }
  
  
  public static enum Type
  {
    Object,
    Matrix,
    ObjectArray,
    SimpleArray,
  }
}