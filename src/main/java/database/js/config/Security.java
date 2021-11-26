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
import org.json.JSONObject;
import database.js.security.Keystore;


public class Security
{
  private final Keystore trust;
  private final Keystore identity;


  Security(JSONObject config) throws Exception
  {
    String type = null;
    String file = null;
    String passwd = null;

    JSONObject identsec = config.getJSONObject("identity");

    type = identsec.getString("type");
    file = identsec.getString("keystore");
    passwd = identsec.getString("password");
    String alias = identsec.getString("alias");

    if (file.startsWith("."+File.separator))
      file = Paths.apphome + File.separator + file;

    identity = new Keystore(file,type,alias,passwd);

    JSONObject trustsec = config.getJSONObject("identity");

    type = trustsec.getString("type");
    file = trustsec.getString("keystore");
    passwd = trustsec.getString("password");

    if (file.startsWith("."+File.separator))
      file = Paths.apphome + File.separator + file;

    trust = new Keystore(file,type,null,passwd);
  }


  public Keystore getTrusted()
  {
    return(trust);
  }

  public Keystore getIdentity()
  {
    return(identity);
  }
}