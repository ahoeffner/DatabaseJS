/*
  MIT License

  Copyright © 2023 Alex Høffner

  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
  and associated documentation files (the “Software”), to deal in the Software without
  restriction, including without limitation the rights to use, copy, modify, merge, publish,
  distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
  Software is furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all copies or
  substantial portions of the Software.

  THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
  BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package database.rest.config;

import java.io.File;
import org.json.JSONObject;
import database.rest.security.Keystore;


public class Security
{
  private final String api;
  private final String clazz;
  private final String secret;
  private final Keystore trust;
  private final Keystore identity;


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

    JSONObject api = Config.getSection(config,"api");

    this.api = Config.get(api,"type");
    this.clazz = Config.get(api,"class");

    this.secret = Config.get(config,"shared_secret");
  }


  public String secret()
  {
    return(secret);
  }

  public Keystore getTrusted()
  {
    return(trust);
  }

  public Keystore getIdentity()
  {
    return(identity);
  }

  public String api()
  {
    return(api);
  }

  public String apiclass()
  {
    return(clazz);
  }
}