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
  ByteBuffer send;
  ByteBuffer data;
  ByteBuffer recv;
  ByteBuffer sslb;

  private final int size;
  private final int asize;
  private final int psize;
  private final boolean ssl;

  private static int SIZE = 4*1024;


  public static void setSize(int size)
  {
    SIZE = size;
  }


  public HTTPBuffers()
  {
    this.asize = 0;
    this.psize = 0;
    this.size = SIZE;
    this.ssl = false;
  }


  public HTTPBuffers(int asize, int psize)
  {
    this.ssl = true;
    this.size = SIZE;
    this.asize = asize;
    this.psize = psize;
  }


  public int size()
  {
    return(size);
  }


  public void alloc(boolean free) throws Exception
  {
    if (free) done();
    alloc();
  }


  public void alloc() throws Exception
  {
    this.data = ByteBuffer.allocateDirect(size);
    if (ssl) this.sslb = ByteBuffer.allocateDirect(psize);

    if (data == null || (ssl & sslb == null))
      throw new Exception("Unable to allocate ByteBuffer");
  }


  public void handshake() throws Exception
  {
    this.data = ByteBuffer.allocateDirect(asize);
    this.send = ByteBuffer.allocateDirect(psize);
    this.recv = ByteBuffer.allocateDirect(psize);

    if (data == null || send == null || recv == null)
      throw new Exception("Unable to allocate ByteBuffer");
  }


  public ByteBuffer done()
  {
    ByteBuffer data = this.data;

    this.data = null;
    this.sslb = null;
    this.send = null;
    this.recv = null;

    return(data);
  }
}