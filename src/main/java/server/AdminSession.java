package server;

import config.Config;
import handlers.Handler;
import java.net.Socket;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.OutputStream;
import handlers.AdminHandler;


public class AdminSession extends Thread
{
  private final int inst;
  private final int port;
  private final String host;
  private final Config config;
  private final Socket socket;


  public AdminSession(Server server, Socket socket, String host, int port)
  {
    this.host = host;
    this.port = port;
    this.socket = socket;
    this.inst = server.inst;
    this.config = server.config;
  }


  @Override
  public void run()
  {
    Thread.currentThread().setName("session port "+port);

    try
    {
      InputStream in = this.socket.getInputStream();
      OutputStream out = this.socket.getOutputStream();

      SocketReader reader = new SocketReader(in);
      String remote = socket.getInetAddress().getCanonicalHostName();

      config.log.logger.fine("Client "+remote+" connected to admin server");

      while(true)
      {
        ArrayList<String> headers = reader.getHeader();

        HTTPRequest request = new HTTPRequest(port,headers);
        HTTPResponse response = new HTTPResponse(this.host+":"+port);

        String cl = request.getHeader("Content-Length");

        if (cl != null)
        {
          byte[] body = reader.getContent(Integer.parseInt(cl));
          request.setBody(body);
        }

        Handler handler = new AdminHandler();
        handler.handle(config,request,response);

        int off = 0;
        int maxsz = 8192;
        byte[] page = response.getPage();

        while(off < page.length)
        {
          int chk = page.length - off;
          if (chk > maxsz) chk = maxsz;
          out.write(page,off,chk); off += chk;
        }
      }
    }
    catch (Exception e)
    {
      boolean skip = false;

      String msg = e.getMessage();
      if (msg == null) msg = "An unknown error has occured";

      if (msg.contains("Broken pipe")) skip = true;
      if (msg.contains("Socket closed")) skip = true;
      if (msg.contains("Connection reset")) skip = true;
      if (msg.contains("certificate_unknown")) skip = true;
      if (msg.contains("Remote host terminated")) skip = true;

      if (!skip) config.log.exception(e);
    }
  }
}
