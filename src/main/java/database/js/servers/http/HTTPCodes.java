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

import java.util.concurrent.ConcurrentHashMap;


public class HTTPCodes
{
  public static final ConcurrentHashMap<Integer,String> codes =
    new ConcurrentHashMap<Integer,String>();


  static
  {
    codes.put(200,"OK");
    codes.put(404,"Not Found");
    codes.put(400,"Bad Request");
    codes.put(304,"Not Modified");
    codes.put(301,"Moved Permanently");
    codes.put(503,"Service Unavailable");
  }


  public static String get(int code)
  {
    String reason = codes.get(code);
    return("HTTP/1.1 " + code + " " +reason);
  }
}