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
  private final boolean admin;

  private Server server = null;
  private Config config = null;
  private SSLContext ctx = null;
  private ServerSocket socket = null;


  public Listener(Server server, PKIContext pki, String host, int port) throws Exception
  {
    this(server,pki,host,port,false,0);
  }


  public Listener(Server server, PKIContext pki, String host, int port, int rssl) throws Exception
  {
    this(server,pki,host,port,false,rssl);
  }


  public Listener(Server server, PKIContext pki, String host, int port, boolean auth) throws Exception
  {
    this(server,pki,host,port,auth,0);
  }


  public Listener(Server server, PKIContext pki, String host, int port, boolean admin, int rssl) throws Exception
  {
    this.rssl = rssl;
    this.admin = admin;
    this.server = server;
    this.inst = server.inst;
    this.config = server.config;

    this.setDaemon(true);

    if (pki == null)
    {
      this.socket = new ServerSocket(port);
    }
    else
    {
      this.ctx = pki.getSSLContext();
      this.socket = this.ctx.getServerSocketFactory().createServerSocket(port);
      if (admin) ((SSLServerSocket) socket).setNeedClientAuth(true);
    }

    this.host = host;
    this.port = port;
  }


  @Override
  public void run()
  {
    String[] corsdomains = config.http.corsdomains;
    Thread.currentThread().setName("listener port "+port);

    while(true)
    {
      try
      {
        Socket socket = this.socket.accept();
        config.log.logger.finest("Accept new session on port "+port);

        if (this.admin)
        {
          AdminSession session = new AdminSession(server,socket,host,port);
          session.start();
        }
        else
        {
          Session session = new Session(server,socket,host,port,corsdomains,rssl);
          session.start();
        }
      }
      catch(Exception e)
      {
        config.log.exception(e);
      }
    }
  }
}
