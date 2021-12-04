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

package database.js.handlers.rest;

import java.math.BigInteger;
import java.security.SecureRandom;


public class Guid
{
  private final String guid;

  public Guid()
  {
    BigInteger bi = null;
    byte[] bytes = new byte[4];
    SecureRandom random = new SecureRandom();

    random.nextBytes(bytes);
    bi = new BigInteger(bytes);
    String p1 = Integer.toHexString(bi.intValue());

    random.nextBytes(bytes);
    bi = new BigInteger(bytes);
    String p2 = Integer.toHexString(bi.intValue());

    String guid = (p1 + p2);

    while(guid.length() < 16)
      guid += random.nextInt(9);

    this.guid = guid;
  }


  @Override
  public String toString()
  {
    return(guid);
  }
}