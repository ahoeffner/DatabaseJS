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
  private String header;
  private String response;
  private boolean finished;

  private final ArrayList<String> headers =
    new ArrayList<String>();
  
  private final static String EOL = "\r\n";
  private final static String server = "database.js";
  private static final SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM YYYY hh:mm:ss z");
  
  
  public HTTPResponse()
  {
    add("server",server);
    add("Date",new Date());
    add("Connection","Keep-Alive");
    add("Keep-Alive","timeout=5, max=100");
  }
  
  /*

  HTTP/1.1 200 OK
  Date: Mon, 20 Sep 2021 12:11:30 GMT
  Server: Apache/2.4.38 (Raspbian)
  Last-Modified: Mon, 25 Jan 2021 23:29:29 GMT
  ETag: "65-5b9c1e8ce9deb-gzip"
  Accept-Ranges: bytes
  Vary: Accept-Encoding
  Content-Encoding: gzip
  Content-Length: 105
  Keep-Alive: timeout=5, max=100
  Connection: Keep-Alive
  Content-Type: text/html


   */
  
  
  void finish()
  {
    finished = true;
    add("Content-Length",body.length);
    if (this.response == null) setResponse(200);
  }
  
  
  public void setResponse(int code)
  {
    this.response = HTTPCodes.get(code);
  }
  
  
  public void setLastModified()
  {
    setLastModified(new Date());
  }
  
  
  public void setLastModified(Date date)
  {
    add("Last-Modified",format.format(date));
    add("ETag",Long.toHexString(date.getTime()));
  }
  
  
  public void setLastModified(long time)
  {
    Date date = new Date();
    date.setTime(time);
    setLastModified(date);    
  }
  
  
  public void add(String header, Date value)
  {
    headers.add(header+": "+format.format(value));
  }
  
  
  public void add(String header, int value)
  {
    headers.add(header+": "+value);
  }
  
  
  public void add(String header, String value)
  {
    headers.add(header+": "+value);
  }
  
  
  public void setBody(String body)
  {
    setBody(body.getBytes());
  }
  
  
  public void setBody(byte[] body)
  {
    this.body = body;
  }
  
  
  public String header()
  {
    if (header != null)
      return(header);
    
    header = response + EOL;
    for(String h : headers) header += h+EOL;
    
    header += EOL;
    return(header);
  }
  
  
  byte[] body()
  {
    if (!finished) finish();
    
    byte[] head = header().getBytes();
    byte[] body = new byte[header.length()+this.body.length];
    
    System.out.println(header);

    System.arraycopy(head,0,body,0,head.length);    
    System.arraycopy(this.body,0,body,head.length,this.body.length);
    
    return(body);
  }
}
