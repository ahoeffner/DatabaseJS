package config;

import java.io.File;

import org.json.JSONObject;


public class Security
{
  private final Keystore trust;
  private final Keystore identity;
  
  
  Security(JSONObject section) throws Exception
  {
    String type = null;
    String file = null;
    String passwd = null;
    
    JSONObject identsec = section.getJSONObject("identity");

    type = identsec.getString("type");
    file = identsec.getString("keystore");
    passwd = identsec.getString("password");
    String alias = identsec.getString("alias");
    
    if (file.startsWith("."))
      file = Paths.apphome + File.separator + file;
    
    identity = new Keystore(file,type,alias,passwd);
    
    JSONObject trustsec = section.getJSONObject("identity");

    type = trustsec.getString("type");
    file = trustsec.getString("keystore");
    passwd = trustsec.getString("password");
    
    if (file.startsWith("."))
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
