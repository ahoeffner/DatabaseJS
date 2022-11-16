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

package database.js.config;

import java.io.File;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import database.js.security.Keystore;
import database.js.database.NameValuePair;


public class Security
{
  private final String oaurl;
  private final String usrattr;
  private final Keystore trust;
  private final Keystore identity;
  private final ArrayList<NameValuePair<Object>> headers;


  Security(JSONObject config) throws Exception
  {
    String type = null;
    String file = null;
    String passwd = null;

    JSONObject identsec = Config.getSection(config,"identity");

    type = Config.get(identsec,"type");
    file = Config.get(identsec,"keystore");
    passwd = Config.get(identsec,"password");
    String alias = Config.get(identsec,"alias");

    if (file.startsWith("."+File.separator) || file.startsWith("./"))
      file = Paths.apphome + File.separator + file;

    identity = new Keystore(file,type,alias,passwd);

    JSONObject trustsec = Config.getSection(config,"trust");

    type = Config.get(trustsec,"type");
    file = Config.get(trustsec,"keystore");
    passwd = Config.get(trustsec,"password");

    if (file.startsWith("."+File.separator) || file.startsWith("./"))
      file = Paths.apphome + File.separator + file;

    trust = new Keystore(file,type,null,passwd);

    JSONObject oauth = Config.getSection(config,"oauth2");

    this.oaurl = Config.get(oauth,"url");
    this.usrattr = Config.get(oauth,"user.attr");
    this.headers = new ArrayList<NameValuePair<Object>>();

    JSONArray headers = oauth.getJSONArray("headers");

    for (int i = 0; i < headers.length(); i++)
    {
      JSONObject header = headers.getJSONObject(i);
      String name = JSONObject.getNames(header)[0];

      Object value = Config.get(header,name);
      this.headers.add(new NameValuePair<Object>(name,value));
    }
  }


  public Keystore getTrusted()
  {
    return(trust);
  }

  public Keystore getIdentity()
  {
    return(identity);
  }

  public String oauthurl()
  {
    return(oaurl);
  }

  public String usrattr()
  {
    return(usrattr);
  }

  public ArrayList<NameValuePair<Object>> oaheaders()
  {
    return(headers);
  }
}