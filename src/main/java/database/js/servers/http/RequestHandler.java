package database.js.servers.http;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;


public class RequestHandler extends Thread
{
  private final Logger logger;
  private final SelectionKey key;
  private final HTTPServer server;
  private final HTTPChannel helper;
  private final SocketChannel channel;


  public RequestHandler(HTTPServer server, SelectionKey key)
  {
    this.key = key;
    this.server = server;
    this.logger = server.logger();
    this.helper = (HTTPChannel) key.attachment();
    this.channel = (SocketChannel) key.channel();
  }
  
  
  public boolean connected()
  {
    return(helper.connected());
  }
  
  
  @Override
  public void run()
  {
    try
    {
      ByteBuffer buf = helper.read();

      if (buf == null)
      {
        channel.close();
        return;
      }

      int read = buf.remaining();

      HTTPRequest request = server.getIncomplete(key);
      if (request == null) request = new HTTPRequest(helper);

      if (!request.add(buf.array(),read))
      {
        server.setIncomplete(key,request);
        return;
      }

      HTTPWorker worker = new HTTPWorker(server,request);
      worker.run();
    }
    catch (Exception e)
    {
      e.printStackTrace();
      logger.log(Level.SEVERE,e.getMessage(),e);
    }
  }
}
