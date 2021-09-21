package database.js.servers;

import ipc.Broker;
import javax.net.ssl.SSLEngine;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.control.Server;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import database.js.security.PKIContext;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

import java.net.InetAddress;

import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;


public class HTTPSServer extends Thread
{
  private final int port;
  private final int threads;
  private final Config config;
  private final Broker broker;
  private final Logger logger;
  private final boolean embedded;
  private final boolean redirect;
  private final ThreadPool workers;


  public HTTPSServer(Server server, boolean embedded) throws Exception
  {
    this.redirect = false;
    this.embedded = embedded;
    this.broker = server.broker();
    this.config = server.config();
    this.port = config.getHTTP().ssl();
    this.logger = config.getLogger().logger;
    this.threads = config.getTopology().threads();  
    
    //this.setDaemon(true);
    this.setName("HTTP(S)Server");
    this.workers = new ThreadPool(threads);
  }
  
  
  @Override
  public void run()
  {
    if (port <= 0) 
      return;
    
    HashMap<SelectionKey,HTTPRequest> incomplete =
      new HashMap<SelectionKey,HTTPRequest>();

    ByteBuffer buf = ByteBuffer.allocate(2048);
    ByteBuffer decrypt = ByteBuffer.allocate(2048);
    
    try
    {
      Selector selector = Selector.open();
      
      InetAddress ip = InetAddress.getByName("localhost");
      ServerSocketChannel server = ServerSocketChannel.open();
      
      server.configureBlocking(false);
      server.bind(new InetSocketAddress(ip,port));
      
      server.register(selector,SelectionKey.OP_ACCEPT);
      
      while(true)
      {
        if (selector.select() <= 0)
          continue;         
        
        Set<SelectionKey> selected = selector.selectedKeys();
        Iterator<SelectionKey> iterator = selected.iterator();
        
        while(iterator.hasNext())
        {
          SelectionKey key = iterator.next();
          iterator.remove();
          
          if (key.isAcceptable())
          {
            SocketChannel sac = server.accept();
            sac.configureBlocking(false);
            
            SSLUtils ssl = new SSLUtils(config,sac,false);
            ssl.accept();

            sac.register(selector,SelectionKey.OP_READ,ssl);
            logger.fine("Connection Accepted: "+sac.getLocalAddress());              
          }          
          else if (key.isReadable())
          {
            try
            {
              System.out.println("************************");

              SSLUtils ssl = (SSLUtils) key.attachment();
              SocketChannel req = (SocketChannel) key.channel();
              
              int read = ssl.read(buf);
                            
              if (read < 0)
              {
                req.close();
                continue;                
              }
              
              byte[] test = buf.array();
              System.out.println("read "+read+" ** "+new String(test,0,32));
              
              buf = ByteBuffer.wrap("200 OK HTTP/1.0\r\n\r\n".getBytes());
              ssl.write(buf);
            }
            catch (Exception e)
            {
              logger.log(Level.SEVERE,e.getMessage(),e);
            }
          }
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
      logger.log(Level.SEVERE,e.getMessage(),e);
    }

    logger.info("HTTP(S)Server stopped");
  }
}
