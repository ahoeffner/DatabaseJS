package test;

import java.net.Socket;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.OutputStream;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLContext;
import database.js.admin.SocketReader;
import javax.net.ssl.X509TrustManager;


public class Session
{
  private final int port;
  private final String host;
  private final Socket socket;
  
  
  public Session(String host, int port, boolean ssl) throws Exception
  {
    this.host = host;
    this.port = port;
    
    if (!ssl)
    {
      socket = new Socket(host,port);
    }
    else
    {
      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(null,new X509TrustManager[] {new FakeTrustManager()}, new java.security.SecureRandom());
      socket = ctx.getSocketFactory().createSocket(host,port);
      ((SSLSocket) socket).startHandshake(); 
    }
    
    socket.setSoTimeout(5000);
  }
  
  
  public void invoke(String url, String message) throws Exception
  {
    HTTPRequest request = new HTTPRequest(host,url);
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
  }
  
  
  public void close() throws Exception
  {
    socket.close();
  }
}