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

import java.util.Date;
import java.util.ArrayList;
import java.text.SimpleDateFormat;


public class HTTPResponse
{
  private byte[] body;
  private byte[] page;
  private String header;
  private String response;
  private String mimetype;
  private boolean finished;

  private final ArrayList<String> headers =
    new ArrayList<String>();

  private final static String EOL = "\r\n";
  private final static String server = "database.js";
  private static final SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM YYYY hh:mm:ss z");


  public HTTPResponse()
  {
    setHeader("server",server);
    setHeader("Date",new Date());
    setHeader("Connection","Keep-Alive");
    setHeader("Keep-Alive","timeout=5, max=100");
  }


  public HTTPResponse(byte[] data)
  {
    finished = true;
    this.page = data;

    int hlen = hlength(data);
    this.header = new String(data,0,hlen);

    this.body = new byte[data.length-hlen];
    System.arraycopy(data,hlen,body,0,body.length);
  }


  void finish()
  {
    if (finished)
      return;
    
    finished = true;

    if (body == null)
      body = new byte[0];

    if (mimetype == null)
      mimetype = "text/plain";

    setHeader("Content-Type",mimetype);
    setHeader("Content-Length",body.length);

    if (this.response == null) setResponse(200);
  }


  public void setResponse(int code)
  {
    this.response = HTTPCodes.get(code);
  }


  public void setContentType(String mimetype)
  {
    this.mimetype = mimetype;
  }


  public void setLastModified()
  {
    setLastModified(new Date());
  }


  public void setLastModified(Date date)
  {
    setHeader("Last-Modified",format.format(date));
    setHeader("ETag",Long.toHexString(date.getTime()));
  }


  public void setLastModified(long time)
  {
    Date date = new Date();
    date.setTime(time);
    setLastModified(date);
  }


  public void setHeader(String header, Date value)
  {
    headers.add(header+": "+format.format(value));
  }


  public void setHeader(String header, int value)
  {
    headers.add(header+": "+value);
  }


  public void setHeader(String header, String value)
  {
    headers.add(header+": "+value);
  }


  public void setCookie(String cookie, String value)
  {
    Date expires = null;
    setCookie(cookie,value,expires,"/");
  }


  public void setCookie(String cookie, String value, Date expires, String path)
  {
    String expire = "";

    if (value == null)
      value = "";

    if (expires != null)
      expire = "; expires="+format.format(expires);

    setHeader("Set-Cookie",cookie+"="+value+expire+"; path="+path);
  }


  public void removeCookie(String cookie)
  {
    removeCookie(cookie,"/");
  }


  public void removeCookie(String cookie, String path)
  {
    Date date = new Date();
    date.setTime(0);
    setCookie(cookie,null,date,path);
  }



  public void setBody(String body)
  {
    setBody(body.getBytes());
  }


  public void setBody(byte[] body)
  {
    this.body = body;
  }


  public void setBody(byte[] body, int pos, int len)
  {
    this.body = new byte[len];
    System.arraycopy(body,pos,this.body,0,len);
  }


  public String header()
  {
    if (header != null)
      return(header);

    finish();

    header = response + EOL;
    for(String h : headers) header += h+EOL;

    header += EOL;
    return(header);
  }


  public byte[] body()
  {
    return(body);
  }


  public byte[] page()
  {
    if (!finished) finish();
    if (page != null) return(page);

    byte[] head = header().getBytes();
    this.page = new byte[header.length()+this.body.length];

    System.arraycopy(head,0,page,0,head.length);
    System.arraycopy(this.body,0,page,head.length,this.body.length);

    return(page);
  }


  private int hlength(byte[] data)
  {
    for (int h = 0; h < data.length-3; h++)
    {
      if (data[h] == '\r' && data[h+1] == '\n' && data[h+2] == '\r' && data[h+3] == '\n')
        return(h+3);
    }

    return(data.length);
  }
}