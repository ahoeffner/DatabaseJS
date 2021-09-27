package database.js.admin;

import java.util.ArrayList;
import java.io.InputStream;
import java.io.OutputStream;
import javax.net.ssl.SSLSocket;
import database.js.config.Config;
import database.js.security.PKIContext;


public class Client
{
  private int port;
  private String host;
  private SSLSocket socket;
  private final PKIContext pki;
  
  
  public Client(String host, int port) throws Exception
  {
    this.host = host;    
    this.port = port;
    this.pki = Config.PKIContext();
  }


  public String send(String cmd) throws Exception
  {
    return(send(cmd,null));
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
  
  
  public void connect() throws Exception
  {
    this.socket = (SSLSocket) pki.getSSLContext().getSocketFactory().createSocket(host,port);
  }
}