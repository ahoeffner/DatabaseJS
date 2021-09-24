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

package database.js.servers;

import java.nio.ByteBuffer;


public class HTTPBuffers
{
  boolean ssl = false;

  ByteBuffer myNetData;
  ByteBuffer myAppData;
  ByteBuffer peerNetData;
  ByteBuffer peerAppData;

  public final static int wmax = 4*1024;
  public final static int size = 16*1024;
  public final static int smax = 5*1024*1024;


  public HTTPBuffers()
  {
    this.myAppData = ByteBuffer.allocate(size);
  }


  public void usessl()
  {
    this.ssl = true;
    this.myNetData = ByteBuffer.allocate(size);
    this.peerAppData = ByteBuffer.allocate(size);
    this.peerNetData = ByteBuffer.allocate(size);
  }


  public void reset()
  {
    this.peerAppData = null;
    this.peerNetData = null;
    this.myAppData = ByteBuffer.allocate(size);
    this.myNetData = ByteBuffer.allocate(size);
  }


  public void clear()
  {
    this.myAppData = ByteBuffer.allocate(size);
    this.myNetData = ByteBuffer.allocate(size);
    this.peerAppData = ByteBuffer.allocate(size);
    this.peerNetData = ByteBuffer.allocate(size);
  }
}
