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

package database.js.cluster;


public class PreAuthRecord
{
  public final long time;
  public final String guid;
  public final String username;
  public final static int reclen = 16 + Long.BYTES + 1;


  public PreAuthRecord(String guid, String username)
  {
    this.guid = guid;
    this.username = username;
    this.time = System.currentTimeMillis();
  }

  public PreAuthRecord(byte[] guid, long time, byte[] username)
  {
    this.time = time;
    this.guid = new String(guid);
    this.username = new String(username);
  }

  public int size()
  {
    return(reclen + username.length());
  }

  @Override
  public String toString()
  {
    return(guid+" "+username);
  }
}