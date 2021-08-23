package client;

import config.Config;
import java.net.Socket;
import server.PKIContext;
import java.util.ArrayList;
import server.SocketReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;


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
    
    String response = client.send("test",null);
    System.out.println(response);
  }


  public Client(Logger logger, PKIContext pki, String host, int port) throws Exception
  {
    this.pki = pki;
    this.host = host;
    this.port = port;
    this.logger = logger;
  }
  
  
  public String send(String cmd, String message) throws Exception
  {
    HTTPRequest request = new HTTPRequest(host,"/"+cmd);
    request.setBody(message);
    
    InputStream in = socket.getInputStream();
    OutputStream out = socket.getOutputStream();
    SocketReader reader = new SocketReader(in);

    out.write(request.getPage());
    ArrayList<String> headers = reader.getHeader();

    int cl = 0;
    for(String header : headers)
    {
      if (header.startsWith("Content-Length"))
        cl = Integer.parseInt(header.split(":")[1].trim());
    }
    
    String response = null;
    if (cl > 0) response = new String(reader.getContent(cl));
    
    return(response);
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
