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
  final long id;
  final int size;
  final int extend;

  byte[] data;
  byte[] request;
  
  public final static int HEADER = 16;
  private final ByteBuffer buffer = ByteBuffer.allocate(16);
  
  
  RESTComm(long id, int extend, byte[] data)
  {
    this.id = id;
    this.extend = extend;
    this.size = data.length;
    
    buffer.putLong(id);
    buffer.putInt(extend);
    buffer.putInt(data.length);
    
    byte[] header = buffer.array();

    if (extend >= 0) request = header;
    else
    {
      request = new byte[HEADER + data.length];
      System.arraycopy(header,0,request,0,header.length);
      System.arraycopy(data,0,request,header.length,data.length);
    }
  }
  
  
  RESTComm(byte[] data)
  {
    buffer.put(data);
    buffer.flip();
    
    this.id = buffer.getLong();
    this.extend = buffer.getInt();
    this.size = buffer.getInt();
    
    this.request = data;
  }
  
  
  long id()
  {
    return(id);
  }
  
  
  int need()
  {
    if (extend < 0) return(size);
    return(0);
  }
  
  
  void add(byte[] data)
  {
    this.data = data;
  }
  
  
  void set(byte[] data)
  {
    this.data = data;
  }
  
  
  int extend()
  {
    return(extend);
  }
  
  
  byte[] request()
  {
    return(request);
  }
  
  
  byte[] data()
  {
    return(data);
  }
}
