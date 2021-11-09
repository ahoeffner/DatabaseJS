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

import java.io.InputStream;


public class SocketReader
{
  private int pos = 0;
  private int size = 0;
  private long total = 0;
  private final InputStream in;
  private final static int MAX = 8192;
  private final byte[] buffer = new byte[MAX];


  public SocketReader(InputStream in)
  {
    this.in = in;
  }


  public boolean empty()
  {
    return(pos >= size);
  }


  public long bytes()
  {
    return(total);
  }


  public byte read() throws Exception
  {
    if (this.pos < this.size)
      return(this.buffer[pos++]);

    this.pos = 0;
    this.size = in.read(buffer);
    if (size > 0) this.total += this.size;

    if (this.size == -1)
      throw new Exception("Socket closed");

    return(read());
  }


  public byte[] read(int size) throws Exception
  {
    int pos = 0;
    byte[] data = new byte[size];
    int available = this.size - this.pos;

    while(true)
    {
      if (available > 0)
      {
        if (available > size - pos)
          available = size - pos;

        System.arraycopy(this.buffer,this.pos,data,pos,available);

        pos += available;
        this.pos += available;
      }

      if (pos == size)
        break;

      this.pos = 0;
      this.size = in.read(buffer);
      if (size > 0) this.total += this.size;
      available = this.size - this.pos;

      if (this.size == -1)
        throw new Exception("Socket closed");
    }

    return(data);
  }
}