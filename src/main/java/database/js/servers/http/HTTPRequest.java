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

package database.js.servers.http;

import java.util.HashMap;
import java.util.ArrayList;
import database.js.pools.PoolResponse;


public class HTTPRequest implements PoolResponse
{
  private int header = -1;
  private int clength = -1;

  private String path = null;
  private String method = null;
  private String version = null;
  private boolean parsed = false;

  private HTTPChannel channel;
  private byte[] request = new byte[0];

  private HashMap<String,String> headers =
    new HashMap<String,String>();

  private HashMap<String,String> cookies =
    new HashMap<String,String>();

  private final ArrayList<Pair<String,String>> query =
    new ArrayList<Pair<String,String>>();

  private final static String EOL = "\r\n";
  private long touched = System.currentTimeMillis();


  HTTPRequest(HTTPChannel channel)
  {
    this.channel = channel;
  }

  public String path()
  {
    return(path);
  }

  public String method()
  {
    return(method);
  }

  public String version()
  {
    return(version);
  }

  public String getHeader(String header)
  {
    return(headers.get(header));
  }

  public String getCookie(String cookie)
  {
    return(cookies.get(cookie));
  }

  @Override
  public void respond(byte[] data) throws Exception
  {
    channel.write(data);
  }


  boolean done()
  {
    return(clength >= 0);
  }


  boolean cancelled()
  {
    return(System.currentTimeMillis() - touched > 30000);
  }


  HTTPChannel channel()
  {
    return(channel);
  }


  void parse()
  {
    if (parsed) return;

    parsed = true;
    String header = new String(request,0,this.header);

    String[] lines = header.split(EOL);
    for (int i = 1; i < lines.length; i++)
    {
      int pos = lines[i].indexOf(':');
      String key = lines[i].substring(0,pos).trim();
      String val = lines[i].substring(pos+1).trim();
      this.headers.put(key,val);
    }

    int pos = this.path.indexOf('?');

    if (pos >= 0)
    {
      String query = this.path.substring(pos+1);
      this.path = this.path.substring(0,pos);
      String[] parts = query.split("&");

      for(String part : parts)
      {
        pos = part.indexOf('=');
        if (pos < 0) this.query.add(new Pair<String,String>(part,null));
        else this.query.add(new Pair<String,String>(part.substring(0,pos),part.substring(pos+1)));
      }
    }

    String hcookie = headers.get("Cookie");

    if (hcookie != null)
    {
      String[] cookies = hcookie.split(";");
      for (int i = 0; i < cookies.length; i++)
      {
        String[] nvp = cookies[i].split("=");

        String name = nvp[0].trim();
        String value = nvp.length > 1 ? nvp[1].trim() : "";
        this.cookies.put(name,value);
      }
    }
  }


  boolean add(byte[] data, int len) throws Exception
  {
    int last = request.length;
    byte[] request = new byte[this.request.length+len];

    System.arraycopy(data,0,request,last,len);
    System.arraycopy(this.request,0,request,0,this.request.length);

    this.request = request;

    if (request.length < 8)
      return(false);

    if (method == null)
      method = getMethod();

    if (method == null)
      return(false);

    if (header < 0)
    {
      if (method.equals("GET")) this.bckward(last);
      else                      this.forward(last);
    }

    if (header < 0)
      return(false);

    if (clength < 0)
    {
      path = getPath();
      version = getVersion();

      if (method.equals("GET"))
      {
        clength = 0;
      }
      else
      {
        parse();
        String cl = headers.get("Content-Length");

        if (cl == null) clength = 0;
        else clength = Integer.parseInt(cl);
      }
    }

    return(request.length == header + clength + 4);
  }


  private String getPath()
  {
    if (method == null)
      return(null);

    int b = method.length()+1;

    for (int i = b; i < request.length; i++)
    {
      if (request[i] == ' ')
      {
        String path = new String(request,b,i-b);

        if (path.length() > 1 && path.endsWith("/"))
          path = path.substring(0,path.length()-1);

        return(path);
      }
    }

    return(null);
  }


  private String getMethod()
  {
    for (int i = 0; i < request.length; i++)
    {
      if (request[i] == ' ')
        return(new String(request,0,i));
    }

    return(null);
  }


  private String getVersion()
  {
    if (path == null)
      return(null);

    int e = 0;
    int b = 2 + method.length() + path.length();

    for (int h = b; h < request.length-1; h++)
    {
      if (request[h] == '\r' && request[h+1] == '\n')
      {
        e = h-1;

        for (int i = h-1; i > 0; i--)
        {
          if (request[i] == ' ')
          {
            b = i+1;
            break;
          }
        }

        // Skip HTTP/ (5 bytes)
        return(new String(request,b+5,e-b-4));
      }
    }

    return(null);
  }


  void forward(int last)
  {
    int start = 0;
    if (last > 3) start = last - 3;

    for (int h = start; h < request.length-3; h++)
    {
      if (request[h] == '\r' && request[h+1] == '\n' && request[h+2] == '\r' && request[h+3] == '\n')
      {
        header = h;
        return;
      }
    }
  }


  void bckward(int last)
  {
    for (int h = request.length-1; h >= 3 && h >= last-3; h--)
    {
      if (request[h-3] == '\r' && request[h-2] == '\n' && request[h-1] == '\r' && request[h] == '\n')
      {
        header = h - 3;
        return;
      }
    }
  }


  @Override
  public String toString()
  {
    return(new String(request));
  }


  public static class Pair<K,V>
  {
    private final K key;
    private final V value;


    public Pair(K key, V value)
    {
      this.key = key;
      this.value = value;
    }

    public K getKey()
    {
      return(key);
    }

    public V getValue()
    {
      return(value);
    }
  }
}
