package server;

import config.Config;
import java.util.UUID;
import java.net.Socket;
import handlers.Handler;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;


public class Session extends Thread
{
  private final int inst;
  private final int port;
  private final int rssl;
  private final String host;
  private final Config config;
  private final Socket socket;
  private final String corsdomains;

  private final boolean fine;
  private final boolean full;
  private final boolean httplog;

  private final static String nl = System.lineSeparator();


  public Session(Server server, Socket socket, String host, int port, String cors)
  {
    this(server,socket,host,port,cors,0);
  }


  public Session(Server server, Socket socket, String host, int port, String corsdomains, int rssl)
  {
    this.host = host;
    this.port = port;
    this.rssl = rssl;
    this.socket = socket;
    this.inst = server.inst;
    this.config = server.config;
    this.corsdomains = corsdomains;

    this.httplog = config.log.http;
    this.fine = config.log.logger.getLevel() == Level.FINE;
    this.full = config.log.logger.getLevel() == Level.FINEST;
  }


  @Override
  public void run()
  {
    int id = 0;
    String thread = Thread.currentThread().getId()+":";
    Thread.currentThread().setName("session port "+port);

    try
    {
      InputStream in = this.socket.getInputStream();
      OutputStream out = this.socket.getOutputStream();

      SocketReader reader = new SocketReader(in);
      String remote = socket.getInetAddress().getCanonicalHostName();

      if (this.rssl > 0)
      {
        ArrayList<String> headers = reader.getHeader();
        HTTPRequest request = new HTTPRequest(port,headers);

        String path = request.getPath();
        String host = this.host + ":" + rssl;

        HTTPResponse response = new HTTPResponse(this.host+":"+port);

        response.setCode("302 Moved Permanently");
        response.setHeader("location","https://"+host+path);

        out.write(response.getPage());
        return;
      }

      config.log.logger.info("Client "+remote+" connected");

      while(true)
      {
        ArrayList<String> headers = reader.getHeader();

        HTTPRequest request = new HTTPRequest(port,headers);
        HTTPResponse response = new HTTPResponse(this.host+":"+port);


        //************************************************************
        // Detect reload and get/set cookies
        //************************************************************

        boolean reload = false;

        String caching = request.getHeader("Cache-Control");
        String version = request.getCookie("database.js.version");
        String session = request.getCookie("database.js.session");
        String trxinst = request.getCookie("database.js.trxinst");

        if (caching != null && (caching.contains("max-age=0") || caching.contains("no-cache")))
          reload = true;

        if (session == null || reload)
        {
          String uuid = ""+UUID.randomUUID();

          Sessions.logout(session);
          Sessions.register(remote,uuid);
          response.setCookie("database.js.session",uuid);
        }

        if (version == null || reload)
          version = config.http.version;

        if (reload) trxinst = null;
        request.setVersion(version);


        //************************************************************
        // Send appropiate CORS headers
        //************************************************************

        String origin = request.getHeader("Origin");
        String corsreq = request.getHeader("Sec-Fetch-Mode");

        if (corsdomains != null && corsreq != null && origin != null && corsreq.equals("cors"))
        {
          String site = origin.split(":")[1].substring(2);

          if (corsdomains.equals("*") || this.corsdomains.contains(site))
            response.addCorsHeaders(origin);
        }


        //************************************************************
        // Handle request
        //************************************************************

        String cl = request.getHeader("Content-Length");

        if (cl != null)
        {
          byte[] body = reader.getContent(Integer.parseInt(cl));
          request.setBody(body);
        }

        if (httplog)
        {
          if (fine) config.log.logger.fine("cid:"+thread+id+nl+request.getHeaders());
          if (full) config.log.logger.finest("cid:"+thread+id+nl+request.toString());
        }

        Handler handler = getHandler(request);
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

        if (httplog)
        {
          if (fine) config.log.logger.fine("cid:"+thread+id+nl+response.getHeaders());
          if (full) config.log.logger.finest("cid:"+thread+id+nl+response.toString());
        }

        id++;
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


  private Handler getHandler(HTTPRequest request)
  {
    String path = request.getPath();
    String method = request.getMethod();
    if (!path.endsWith("/")) path += "/";
    return(config.http.handlers.getHandler(path,method));
  }
}
