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
import org.json.JSONObject;
import org.json.JSONTokener;
import java.util.logging.Level;
import javax.net.ssl.SSLContext;
import java.util.logging.Logger;
import database.js.config.Config;
import javax.net.ssl.TrustManager;
import database.js.client.HTTPClient;
import database.js.client.HTTPRequest;
import database.js.database.NameValuePair;


public class OAuth
{
  private final int port;
  private final String host;
  private final String path;
  private final String usrattr;
  private final SSLContext ctx;
  private final ArrayList<NameValuePair<Object>> headers;
  private final static Logger logger = Logger.getLogger("rest");

  private static OAuth instance = null;

  public static synchronized void init(Config config) throws Exception
  {
    if (instance == null)
      instance = new OAuth(config);
  }


  public static String getUserName(String token)
  {
    return(instance.verify(token));
  }


  private OAuth(Config config) throws Exception
  {
    String endp = config.getSecurity().oauthurl();
    this.usrattr = config.getSecurity().usrattr();
    this.headers = config.getSecurity().oaheaders();

    ctx = SSLContext.getInstance("TLS");
    FakeTrustManager tmgr = new FakeTrustManager();
    ctx.init(null,new TrustManager[] {tmgr},new java.security.SecureRandom());

    URL url = new URL(endp);

    this.host = url.getHost();
    this.port = url.getPort();
    this.path = url.getPath();
  }


  private String verify(String token)
  {
    try
    {
      HTTPClient client = new HTTPClient(host,port,ctx);
      HTTPRequest request = new HTTPRequest(host,path,token);

      for(NameValuePair<Object> header : headers)
        request.setHeader(header.getName(),header.getValue().toString());

      logger.info("OAuth connect to "+host+":"+port);
      client.connect();

      logger.info("OAuth send request");
      byte[] bytes = client.send(request.page());

      String payload = new String(bytes);
      logger.info("OAuth response \n"+payload);

      JSONTokener tokener = new JSONTokener(payload);
      JSONObject response = new JSONObject(tokener);


      String user = response.getString(usrattr);
      return(user);
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
      return(null);
    }
  }
}