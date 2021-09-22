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
  ByteBuffer encpt;
  ByteBuffer plain;
  
  private final static int size = 1024*1024;


  public HTTPBuffers()
  {
    this.encpt = ByteBuffer.allocate(size);
  }
  
  
  public void usessl()
  {
    if (encpt == null)
      this.plain = ByteBuffer.allocate(size);
  }
}
