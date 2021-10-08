package database.js.servers.rest;

import java.nio.ByteBuffer;
import java.util.logging.Logger;
import database.js.servers.Server;
import database.js.admin.HTTPRequest;
import java.nio.channels.SocketChannel;
import database.js.servers.http.HTTPChannel;


public class RESTEngine
{
  private final int port;
  private final Logger logger;
  private final Server server;
  private final boolean client;
  private final HTTPChannel channel;
  
  
  public RESTEngine(HTTPChannel channel) throws Exception
  {
    this.port = 0;
    this.client = true;
    this.channel = channel;
    this.server = channel.server();
    this.logger = server.config().getLogger().rest;
  }
  
  
  public RESTEngine(Server server, int port, boolean ssl) throws Exception
  {
    this.port = port;
    this.client = false;
    this.server = server;
    SocketChannel channel = SocketChannel.open();    
    this.logger = server.config().getLogger().rest;
    this.channel = new HTTPChannel(server,channel,ssl);
    
    connect();
  }
  
  
  private boolean connect()
  {
    try
    {
      this.channel.connect(port);
      HTTPRequest request = new HTTPRequest("localhost","/connect");

      channel.configureBlocking(true);      
      channel.write(request.getPage());
      
      ByteBuffer buf = channel.read();
      
      int read = buf.remaining();
      byte[] data = new byte[read];
      buf.get(data);
      logger.info(new String(data));
    }
    catch (Exception e)
    {
      return(false);
    }
    
    return(true);
  }
  
  
  private static class ServerID
  {
    short prc = -1;
    long started = -1;
    
    public String toString()
    {
      return(prc+" "+started);
    }
  }
}
