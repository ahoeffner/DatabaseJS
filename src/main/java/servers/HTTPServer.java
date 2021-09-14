package servers;

import config.Config;



public class HTTPServer extends Thread
{
  private final boolean embedded;
  
  
  public HTTPServer(Config config, Type type, boolean embedded)
  {
    this.embedded = embedded;
  }
  
  
  public static enum Type
  {
    SSL,
    Plain,
    Admin
  }
}
