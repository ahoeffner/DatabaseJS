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

package database.js.client;

import java.util.ArrayList;


public class HTTPRequest
{
  private String body;
  private String method;
  private final String host;
  private final String path;

  private ArrayList<String> headers =
    new ArrayList<String>();

  private static final String EOL = "\r\n";


  public HTTPRequest(String host, String path)
  {
    this(host,path,null);
  }


  public HTTPRequest(String host, String path, String body)
  {
    this.host = host;
    this.path = path;
    this.body = body;
  }


  public void setBody(String body)
  {
    this.body = body;
  }

  public void setMethod(String method)
  {
    this.method = method;
  }

  public void setHeader(String header, String value)
  {
    headers.add(header+": "+value);
  }


  public void setCookie(String cookie, String value)
  {
    setCookie(cookie,value,null);
  }


  public void setCookie(String cookie, String value, String path)
  {
    if (path == null)
      path = "/";

    if (value == null)
      value = "";

    setHeader("Set-Cookie",cookie+"="+value+"; path="+path);
  }


  public byte[] page()
  {
    String header = method;
    if (header == null) header = (body == null) ? "GET" : "POST";

    header += " "+path + " HTTP/1.1"+EOL+"Host: "+host+EOL;
    byte[] body = this.body == null ? null : this.body.getBytes();
    if (body != null) header += "Content-Length: "+body.length+EOL;

    for(String h : this.headers)
      header += h + EOL;

    header += EOL;
    byte[] head = header.getBytes();

    if (body == null) return(head);

    byte[] page = new byte[head.length + body.length];

    System.arraycopy(head,0,page,0,head.length);
    System.arraycopy(body,0,page,head.length,body.length);

    return(page);
  }
}