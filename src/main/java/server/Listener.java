package server;

import config.Config;
import java.net.Socket;
import java.net.ServerSocket;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;


public class Listener extends Thread
{
  private int port;
  private String host;
  
  private boolean reqssl = false;

  private Config config = null;  
  private SSLContext ctx = null;
  private ServerSocket socket = null;


  public Listener(Config config, PKIContext pki, String host, int port, boolean auth) throws Exception
  {
    this.setDaemon(true);
    this.config = config;

    if (pki == null)
    {
      this.socket = new ServerSocket(port);
      this.reqssl = config.http.requiressl;
    }
    else
    {
      this.ctx = pki.getSSLContext();
      this.socket = this.ctx.getServerSocketFactory().createServerSocket(port);
      if (auth) ((SSLServerSocket) socket).setNeedClientAuth(true);
    }

    this.host = host;
    this.port = port;
  }
  
  
  @Override
  public void run()
  {
    Thread.currentThread().setName("listener port "+port);
    
    while(true)
    {
      try
      {
        Socket socket = this.socket.accept();
        config.log.logger.finest("Accept new session on port "+port);
        //Session session = new Session(host,port,socket);
        //session.serve(this.reqssl);
      }
      catch(Exception e)
      {
        config.log.exception(e);    
      }
    }
  }
}
