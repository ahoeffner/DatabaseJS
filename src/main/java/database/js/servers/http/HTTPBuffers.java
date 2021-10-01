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

import java.nio.ByteBuffer;


class HTTPBuffers
{
  private int asize = 0;
  private int psize = 0;
  private int size = 4*1024;

  ByteBuffer send;
  ByteBuffer data;
  ByteBuffer recv;
  ByteBuffer sslb;
  
  
  public int size()
  {
    return(size);
  }


  public void nossl()
  {
    this.data = ByteBuffer.allocateDirect(size);
  }


  public void setSize(int asize, int psize)
  {
    this.asize = asize;
    this.psize = psize;
  }


  public void init()
  {
    this.data = ByteBuffer.allocateDirect(asize);
    this.send = ByteBuffer.allocateDirect(psize);
    this.recv = ByteBuffer.allocateDirect(psize);
  }


  public void done()
  {
   this.send = null;
   this.sslb = recv;
  }
}
