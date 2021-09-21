package database.js.servers;

import java.nio.ByteBuffer;
import javax.net.ssl.SSLEngine;
import database.js.config.Config;
import javax.net.ssl.SSLEngineResult;
import java.util.concurrent.Executors;
import database.js.security.PKIContext;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;


public class SSLUtils
{
  private ByteBuffer ssl;
  private ByteBuffer buf;
  private final SSLEngine engine;
  private final SocketChannel channel;
  private final ExecutorService worker;
  
  
  public SSLUtils(Config config, SocketChannel channel, boolean twoway) throws Exception
  {
    this.channel = channel;
    
    PKIContext pki = config.getPKIContext();
    this.engine = pki.getSSLContext().createSSLEngine();
    int size = engine.getSession().getApplicationBufferSize();
    
    this.ssl = ByteBuffer.allocate(size);
    this.buf = ByteBuffer.allocate(size);
    
    this.engine.setUseClientMode(false);
    this.engine.setWantClientAuth(twoway);
    
    this.worker = Executors.newSingleThreadExecutor();
  }
  
  
  public int read(ByteBuffer buf) throws Exception
  {
    this.buf.rewind();
    int read = channel.read(this.buf);
    SSLEngineResult result = engine.unwrap(this.buf,buf);
    System.out.println("Read "+result.getStatus());
    return(read);
  }
  
  
  public void write(ByteBuffer buf) throws Exception
  {
    buf.flip();
    this.buf = ByteBuffer.allocate(32*1024);
    SSLEngineResult result = engine.wrap(buf,this.buf);

    this.buf.flip();
    channel.write(this.buf);
    
    if (result.getStatus() == SSLEngineResult.Status.CLOSED)
      channel.close();
    
    System.out.println("Write "+result.getStatus());
  }
  
  
  public boolean accept()
  {
    int read;
    boolean cont = true;
    
    SSLEngineResult result = null;
    HandshakeStatus status = null;
    
    try
    {
      engine.beginHandshake();
      
      while(cont)
      {
        status = engine.getHandshakeStatus();
        
        switch(status)
        {
          case NEED_UNWRAP:
            read = channel.read(ssl);
            if (read < 0) return(close());

            ssl.flip();
            
            result = engine.unwrap(ssl,buf);
            if (!handle(result)) return(close());
            
            ssl.compact();
            
            break;
          
          case NEED_WRAP:
            ssl.clear();
            buf.clear();
            
            result = engine.wrap(buf,ssl);
            if (!handle(result)) return(close());
            
            ssl.flip();
            
            while(ssl.hasRemaining())
              channel.write(ssl);
            
            break;
          
          case NEED_TASK:
            Runnable task = engine.getDelegatedTask();

            while(task != null)
            {
              worker.submit(task);
              task = engine.getDelegatedTask();
            }

            break;
          
          default:
            cont = false;
            break;
        }
      }
    }
    catch (Exception e)
    {
      boolean skip = false;
      String errm = e.getMessage();
      if (errm == null) errm = "An unknown error has occured";
      
      if (errm.startsWith("Illegal server handshake")) skip = true;
      if (!skip) e.printStackTrace();
    }
    
    this.ssl = null;    
    return(true);
  }
  
  
  private boolean handle(SSLEngineResult result)
  {
    switch (result.getStatus())
    {
      case OK: return(true);
      case CLOSED : return(false);
    }
    
    enlarge();
    return(true);
  }
  
  
  private void enlarge()
  {
    int size = 2 * ssl.capacity();
    
    ByteBuffer sslc = ssl;
    ByteBuffer bufc = buf;
    
    ssl = ByteBuffer.allocate(size);
    buf = ByteBuffer.allocate(size);
    
    sslc.flip();
    bufc.flip();
    
    ssl.put(sslc);
    buf.put(bufc);
  }
  
  
  private boolean close()
  {
    try {engine.closeInbound();}
    catch (Exception e) {;}

    try {engine.closeOutbound();}
    catch (Exception e) {;}

    try {channel.close();}
    catch (Exception e) {;}

    return(false);
  }
}
