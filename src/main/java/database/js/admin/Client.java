package database.js.admin;

import database.js.config.Config;
import database.js.security.PKIContext;


public class Client
{
  private final PKIContext pki;
  
  
  Client(Config config) throws Exception
  {
    this.pki = config.getPKIContext();
  }
  
  
  void stop()
  {
    
  }
}
