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

package database.js.security;

import java.net.URL;
import java.util.Map;
import java.util.List;
import java.io.InputStream;
import javax.net.ssl.SSLContext;
import java.net.HttpURLConnection;
import database.js.client.HTTPClient;
import java.io.ByteArrayOutputStream;
import database.js.client.HTTPRequest;


public class OAuth
{
  private final int port;
  private final String host;
  private final String path;
  private final String auth;


  @SuppressWarnings("unused")
  public static void main(String[] args) throws Exception
  {
    OAuth auth = new OAuth("https://oauth2.googleapis.com:443/token","4/0AX4XfWhWiLRCFJrxiDKjQh3aWdv29fiFuk-UBvS6byAmfcjIQ4Fv7N_UeHzIBKHGRtx1vw");

    //auth.getTestToken();
    auth.verify("ya29.a0ARrdaM83y9p5sgxgVbYbJhHDW4aNZhv42HXLE4LQxznTm9NAbMpILnuRUY-4C4cuL3fdqqG2MYCvHmlMCqHReQBbxI6LhYdYJ1gvyxcIWGsm-NXVVLHlYz51bnqxPir9figDj5rpyLY5CJW_s3LNZaBJLW_b");
  }


  public OAuth(String oaurl, String auth) throws Exception
  {
    URL url = new URL(oaurl);

    this.auth = auth;
    this.host = url.getHost();
    this.port = url.getPort();
    this.path = url.getPath();
  }


  public String verify(String token) throws Exception
  {
    SSLContext ctx = SSLContext.getDefault();
    HTTPClient client = new HTTPClient(host,port,ctx);
    HTTPRequest request = new HTTPRequest(host,path,token);
    if (auth != null) request.setHeader("Authorization",auth);

    client.connect();
    byte[] bytes = client.send(request.page());

    String response = new String(bytes);
    System.out.println(response);
    return(response);
  }


  private String getTestToken() throws Exception
  {
    String code = "code=4/0AX4XfWgD7jrRm3wHZOeqyPU7WbCjOHehYhn6wdW6Wuk6BbOVaqRO4jBSp7gVZP6mRjTisQ&scope=https://www.googleapis.com/auth/cloud-platform";

    URL url = new URL("https://oauth2.googleapis.com/token");

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestProperty("user-agent","google-oauth-playground");
    conn.setRequestProperty("content-type","application/x-www-form-urlencoded");
    conn.setRequestProperty("Authorization","\"4/0AX4XfWhWiLRCFJrxiDKjQh3aWdv29fiFuk-UBvS6byAmfcjIQ4Fv7N_UeHzIBKHGRtx1vw\"");

    conn.setDoOutput(true);
    conn.getOutputStream().write(code.getBytes());

    byte[] bytes = new byte[4196];
    InputStream in = conn.getInputStream();

    for(Map.Entry<String,List<String>> header : conn.getHeaderFields().entrySet())
    {
      System.out.print(header.getKey());
      for(String val : header.getValue()) System.out.print(" "+val);
      System.out.println();
    }

    int read = 0;
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    while(read >= 0)
    {
      read = in.read(bytes);
      if (read > 0) out.write(bytes,0,read);
    }

    String response = new String(out.toByteArray());
    System.out.println(response);
    return(response);
  }
}