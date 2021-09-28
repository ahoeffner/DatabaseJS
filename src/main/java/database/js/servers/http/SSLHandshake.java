package database.js.servers.http;

import java.util.logging.Logger;
import database.js.config.Config;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;


public class SSLHandshake extends Thread
{
  private final Config config;
  private final Logger logger;
  private final boolean twoway;
  private final Selector selector;
  private final HTTPBuffers buffers;
  private final SocketChannel channel; 
  
  
  public SSLHandshake(HTTPServer server, Selector selector, SocketChannel channel, boolean twoway) throws Exception
  {
    this.twoway = twoway;
    this.channel = channel;
    this.selector = selector;
    this.config = server.config();
    this.logger = server.logger();
    this.buffers = new HTTPBuffers();
  }
  
  
  @Override
  public void run()
  {
    try
    {
      HTTPChannel helper = new HTTPChannel(config,buffers,channel,true,twoway);
      boolean accept = helper.accept();
      
      if (accept)
      {
        buffers.done();
        System.out.println("register");
        selector.wakeup();
        channel.register(selector,SelectionKey.OP_READ,helper);
        logger.fine("Connection Accepted: "+channel.getLocalAddress());
      }
      else
      {
        channel.register(selector,0);
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}