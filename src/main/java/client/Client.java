package client;

import config.Config;
import config.Keystore;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import server.PKIContext;


public class Client
{
  private int port;
  private String host;
  private Socket socket;
  private Logger logger;
  private PKIContext pki;
  
  
  public static void main(String[] args) throws Exception
  {
    Config config = new Config(0,null,false);
    PKIContext pki = new PKIContext(config.security.identity,config.security.trust);
    
    Client client = new Client(null,pki,"localhost",9001);
    client.connect();
  }


  public Client(Logger logger, PKIContext pki, String host, int port) throws Exception
  {
    this.pki = pki;
    this.host = host;
    this.port = port;
    this.logger = logger;
  }
  
  
  public boolean connect()
  {
    try
    {
      this.socket = pki.getSSLContext().getSocketFactory().createSocket(host,port);;
    }
    catch (Exception e)
    {
      if (logger == null) e.printStackTrace();
      else logger.log(Level.SEVERE,e.getMessage(),e);
      return(false);
    }
    
    return(true);
  }
}
