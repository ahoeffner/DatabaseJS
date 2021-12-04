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

package database.js.servers.rest;

import java.nio.ByteBuffer;


class RESTComm
{
  final long  id;
  final int   size;
  final short hsize;
  final int   extend;

  byte[] http;
  byte[] page;
  byte[] host;
  byte[] header;

  public final static int HEADER = 18;
  private final ByteBuffer buffer = ByteBuffer.allocate(HEADER);


  RESTComm(long id, int extend, byte[] host, byte[] page)
  {
    this.id = id;
    this.host = host;
    this.page = page;
    this.extend = extend;
    this.size = page.length;
    this.hsize = (short) host.length;

    buffer.putLong(id);
    buffer.putInt(extend);
    buffer.putShort(hsize);
    buffer.putInt(size);

    this.header = buffer.array();

    if (extend >= 0)
    {
      http = new byte[HEADER + hsize];
      System.arraycopy(header,0,http,0,HEADER);
      System.arraycopy(host,0,http,HEADER,hsize);
    }
    else
    {
      http = new byte[HEADER + hsize + size];
      System.arraycopy(header,0,http,0,HEADER);
      System.arraycopy(host,0,http,HEADER,hsize);
      System.arraycopy(page,0,http,HEADER+hsize,size);
    }
  }


  RESTComm(byte[] head)
  {
    buffer.put(head);
    buffer.flip();

    this.id     = buffer.getLong();
    this.extend = buffer.getInt();
    this.hsize  = buffer.getShort();
    this.size   = buffer.getInt();

    this.http = null;
    this.page = null;
    this.header = head;
  }


  long id()
  {
    return(id);
  }
  
  
  byte[] host()
  {
    return(host);
  }


  int hsize()
  {
    return(hsize);
  }


  void setHost(byte[] host)
  {
    this.host = host;
  }


  int need()
  {
    if (extend < 0) return(size);
    return(0);
  }


  void add(byte[] data)
  {
    this.page = data;
  }


  void set(byte[] data)
  {
    this.page = data;
  }


  int extend()
  {
    return(extend);
  }


  byte[] bytes()
  {
    if (http == null)
    {
      if (extend >= 0)
      {
        http = new byte[HEADER + hsize];
        System.arraycopy(header,0,http,0,HEADER);
        System.arraycopy(host,0,http,HEADER,hsize);
      }
      else
      {
        http = new byte[HEADER + hsize + size];
        System.arraycopy(header,0,http,0,HEADER);
        System.arraycopy(host,0,http,HEADER,hsize);
        System.arraycopy(page,0,http,HEADER+hsize,size);
      }
    }

    return(http);
  }


  byte[] page()
  {
    return(page);
  }


  @Override
  public String toString()
  {
    if (page == null) return("id="+id+" extend="+extend+" size="+size);
    return("id="+id+" extend="+extend+" size="+size+System.lineSeparator()+"<"+new String(page)+">"+System.lineSeparator());
  }
}