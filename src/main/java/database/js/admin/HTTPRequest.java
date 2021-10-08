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

package database.js.admin;

public class HTTPRequest
{
  private String body;
  private String method;
  private final String host;
  private final String path;
  private static final String EOL = "\r\n";
  
  
  public HTTPRequest(String host, String path)
  {
    this.host = host;
    this.path = path;
  }


  public void setBody(String body)
  {
    this.body = body;
  }
  
  
  public void setMethod(String method)
  {
    this.method = method;    
  }
  
  
  public byte[] getPage()
  {
    String header = method;
    if (header == null) header = (body == null) ? "GET" : "POST";
    
    header += " "+path + " HTTP/1.1"+EOL+"Host: "+host+EOL;
    byte[] body = this.body == null ? null : this.body.getBytes();
    if (body != null) header += "Content-Length: "+body.length+EOL;
    
    header += EOL;
    byte[] head = header.getBytes();
    
    if (body == null) return(head);

    byte[] page = new byte[head.length + body.length];
    
    System.arraycopy(head,0,page,0,head.length);
    System.arraycopy(body,0,page,head.length,body.length);
    
    return(page);
  }
}
