package server;

import java.util.HashMap;
import java.util.ArrayList;


public class HTTPRequest
{
  private int port;
  private String path;
  private byte[] body;
  private String method;
  private ArrayList<String> headers;
  private HashMap<String,String> headermap;
  private ArrayList<Pair<String,String>> query;


  public HTTPRequest(int port, ArrayList<String> headers)
  {
    this.port = port;
    this.headers = headers;

    headermap = new HashMap<String,String>();
    query = new ArrayList<Pair<String,String>>();

    String request = headers.remove(0);

    int pos = request.indexOf(' ');
    this.method = request.substring(0,pos);

    this.path = request.substring(pos+1);
    pos = this.path.indexOf(' ');
    this.path = this.path.substring(0,pos);

    pos = this.path.indexOf('?');

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

    if (path.endsWith("/"))
      path = path.substring(0,path.length()-1);

    for (String header: headers)
    {
      pos = header.indexOf(':');
      this.headermap.put(header.substring(0,pos).trim(),header.substring(pos+1).trim());
    }
  }


  public void setBody(byte[] body)
  {
    this.body = body;
  }


  public int getPort()
  {
    return(port);
  }


  public byte[] getBody()
  {
    return(body);
  }


  public String getPath()
  {
    return(path);
  }

  public String getMethod()
  {
    return(method);
  }

  public String getHeader(String header)
  {
    return(headermap.get(header));
  }
  
  public String[] getCookies()
  {
    String cookie = headermap.get("Cookie");
    if (cookie == null) return(new String[0]);

    String[] cookies = cookie.split(";");
    for (int i = 0; i < cookies.length; i++) 
      cookies[i] = cookies[i].trim();

    return(cookies);
  }

  public ArrayList<Pair<String,String>> getQuery()
  {
    return(query);
  }
  
  
  public String getHeaders()
  {
    String str = method+" "+(path.length() == 0 ? "/" : path);

    for (int i = 0; i < query.size(); i++)
    {
      str += i == 0 ? "?" : "&";
      Pair<String,String> p = query.get(i);

      str += p.getKey();
      str += p.getValue() == null ? "" : "=" + p.getValue();
    }

    str += "\n";

    for(String header : headers)
      str += header + "\n";
    
    return(str);
  }


  @Override
  public String toString()
  {
    String str = method+" "+path;

    for (int i = 0; i < query.size(); i++)
    {
      str += i == 0 ? "?" : "&";
      Pair<String,String> p = query.get(i);

      str += p.getKey();
      str += p.getValue() == null ? "" : "=" + p.getValue();
    }

    str += "\n";

    for(String header : headers)
      str += header + "\n";

    if (body != null) str += "\n\n" + new String(body);

    return(str);
  }
}
