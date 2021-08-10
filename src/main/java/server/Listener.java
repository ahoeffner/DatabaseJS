package server;

import config.Config;
import java.net.Socket;
import java.net.ServerSocket;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;


public class Listener extends Thread
{
  private final int inst;
  private final int rssl;
  private final int port;
  private final String host;

  private Config config = null;  
  private SSLContext ctx = null;
  private ServerSocket socket = null;


  public Listener(Config config, PKIContext pki, int inst, String host, int port) throws Exception
  {
    this(config,pki,inst,host,port,false,0);
  }


  public Listener(Config config, PKIContext pki, int inst, String host, int port, int rssl) throws Exception
  {
    this(config,pki,inst,host,port,false,rssl);
  }


  public Listener(Config config, PKIContext pki, int inst, String host, int port, boolean auth) throws Exception
  {
    this(config,pki,inst,host,port,auth,0);
  }
  

  public Listener(Config config, PKIContext pki, int inst, String host, int port, boolean auth, int rssl) throws Exception
  {
    this.inst = inst;
    this.rssl = rssl;    
    this.config = config;
    
    this.setDaemon(true);

    if (pki == null)
    {
      this.socket = new ServerSocket(port);
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
    String corsdomains = config.http.corsdomains;
    Thread.currentThread().setName("listener port "+port);
    
    while(true)
    {
      try
      {
        Socket socket = this.socket.accept();
        config.log.logger.finest("Accept new session on port "+port);
        Session session = new Session(config,socket,inst,host,port,corsdomains,rssl);
        session.start();
      }
      catch(Exception e)
      {
        config.log.exception(e);    
      }
    }
  }
}
