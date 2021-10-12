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

import java.nio.ByteBuffer;

import java.util.HashMap;


public class HTTPResponse
{
  private int code = -1;
  private int header = -1;
  private int clength = -1;
  
  private byte[] body = null;
  private boolean finished = false;
  private byte[] response = new byte[0];
  private final static String EOL = "\r\n";

  private HashMap<String,String> headers =
    new HashMap<String,String>();

  private HashMap<String,String> cookies =
    new HashMap<String,String>();
  
  
  public boolean finished()
  {
    return (finished);
  }
  
  
  public byte[] getBody()
  {
    if (body != null) return(body);
    int blen = response.length - this.header - 4;

    if (blen > 0)
    {
      this.body = new byte[blen];
      System.arraycopy(response,this.header+4,this.body,0,blen);
    }

    return(body);
  }
  
  
  public void add(ByteBuffer buf) throws Exception
  {
    int read = buf.remaining();
    byte[] data = new byte[read]; buf.get(data);
    add(data,0,data.length);
  }
  
  
  public void add(byte[] data) throws Exception
  {
    add(data,0,data.length);
  }
  
  
  public void add(byte[] data, int pos, int len) throws Exception
  {
    int last = response.length;
    byte[] response = new byte[this.response.length+len];

    System.arraycopy(data,pos,response,last,len);
    System.arraycopy(this.response,0,response,0,last);

    this.response = response;
    
    if (response.length < 16)
      return;
    
    if (header < 0)
      forward(last);
    
    if (header < 0)
      return;
    
    if (code < 0)
      parse();

    if (clength < 0)
    {
      String cl = headers.get("Content-Length");

      if (cl == null) clength = 0;
      else clength = Integer.parseInt(cl);
    }
    
    if (response.length > header + clength + 4)
      throw new Exception("Received multiple requests without client waiting for response");
    
    finished = response.length == header + clength + 4;
  }
  
  
  void parse()
  {
    String header = new String(response,0,this.header);

    String[] lines = header.split(EOL);
    String[] resp = lines[0].split(" ");
    
    this.code = Integer.parseInt(resp[1]);
    
    for (int i = 1; i < lines.length; i++)
    {
      int pos = lines[i].indexOf(':');
      if (pos <= 0) continue;
      
      String key = lines[i].substring(0,pos).trim();
      String val = lines[i].substring(pos+1).trim();
      
      this.headers.put(key,val);
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


  void forward(int last)
  {
    int start = 0;
    if (last > 3) start = last - 3;

    for (int h = start; h < response.length-3; h++)
    {
      if (response[h] == '\r' && response[h+1] == '\n' && response[h+2] == '\r' && response[h+3] == '\n')
      {
        header = h;
        return;
      }
    }
  }
}
