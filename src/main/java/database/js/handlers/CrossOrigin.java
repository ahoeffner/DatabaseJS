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

package database.js.handlers;

import java.net.URL;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.logging.Logger;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;
import database.js.handlers.rest.JSONFormatter;


public class CrossOrigin
{
  private final static TreeSet<String> domains =
    new TreeSet<String>();

  private final static ArrayList<String> allowed =
    new ArrayList<String>();

  private final static Logger logger = Logger.getLogger("rest");
  private final static String methods = "GET, POST, PATCH, DELETE, PUT, OPTIONS, HEAD";


  public static void init(String host, ArrayList<String> domains)
  {
    int pos = host.indexOf(':');
    if (pos > 0) host = host.substring(0,pos);

    CrossOrigin.domains.add(host);

    for (String pattern : domains)
    {
      pattern = pattern.replace(".","\\.");
      pattern = pattern.replace("*",".*");

      CrossOrigin.allowed.add(".*\\."+pattern+"\\..*");
    }
  }


  public String allow(HTTPRequest request) throws Exception
  {
    String mode = request.getHeader("Sec-Fetch-Mode");

    if (mode == null || !mode.equalsIgnoreCase("cors"))
      return(null);

    String origin = request.getHeader("Origin");
    if (domains.contains(origin)) return(null);

    URL url = new URL(origin);
    origin = url.getHost();

    origin = "." + origin + ".";
    for(String pattern : allowed)
    {
      if (origin.matches(pattern))
      {
        domains.add(origin);
        return(null);
      }
    }

    JSONFormatter jfmt = new JSONFormatter();

    jfmt.success(false);
    jfmt.add("message","Origin \""+origin+"\" rejected by Cors");

    logger.severe("Origin \""+origin+"\" rejected by Cors");
    return(jfmt.toString());
  }


  public void addHeaders(HTTPRequest request, HTTPResponse response)
  {
    String mode = request.getHeader("Sec-Fetch-Mode");
    if (mode == null || !mode.equalsIgnoreCase("cors")) return;
    String method = request.getHeader("Access-Control-Request-Method");

    String origin = request.getHeader("Origin");
    response.setHeader("Access-Control-Allow-Headers","*");
    response.setHeader("Access-Control-Request-Headers","*");
    response.setHeader("Access-Control-Allow-Origin",origin);
    response.setHeader("Access-Control-Request-Method",method);
    response.setHeader("Access-Control-Allow-Methods",methods);
    response.setHeader("Access-Control-Allow-Credentials","true");
  }
}