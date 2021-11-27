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
import java.util.ArrayList;
import javax.net.ssl.SSLContext;
import java.util.logging.Logger;
import database.js.config.Config;
import javax.net.ssl.TrustManager;
import database.js.client.HTTPClient;
import database.js.client.HTTPRequest;
import database.js.database.NameValuePair;

import java.util.logging.Logger;


public class OAuth
{
  private final int port;
  private final String host;
  private final String path;
  private final Logger logger;
  private final SSLContext ctx;
  private final ArrayList<NameValuePair<Object>> headers;

  private static OAuth instance = null;

  public static synchronized void init(Config config) throws Exception
  {
    if (instance == null)
      instance = new OAuth(config);
  }


  public static String getUserName(String token) throws Exception
  {
    return(instance.verify(token));
  }


  private OAuth(Config config) throws Exception
  {
    String endp = config.getSecurity().oauthurl();
    this.headers = config.getSecurity().oaheaders();

    ctx = SSLContext.getInstance("TLS");
    FakeTrustManager tmgr = new FakeTrustManager();
    ctx.init(null,new TrustManager[] {tmgr},new java.security.SecureRandom());

    URL url = new URL(endp);

    this.host = url.getHost();
    this.port = url.getPort();
    this.path = url.getPath();
    this.logger = config.getLogger().rest;
  }


  private String verify(String token) throws Exception
  {
    HTTPClient client = new HTTPClient(host,port,ctx);
    HTTPRequest request = new HTTPRequest(host,path,token);

    for(NameValuePair<Object> header : headers)
      request.setHeader(header.getName(),header.getValue().toString());

    logger.info("OAuth connect to "+host+":"+port);
    client.connect();

    logger.info("OAuth send request");
    byte[] bytes = client.send(request.page());

    String response = new String(bytes);
    logger.info("OAuth response \n"+response);

    String user = "????";
    return(user);
  }
}