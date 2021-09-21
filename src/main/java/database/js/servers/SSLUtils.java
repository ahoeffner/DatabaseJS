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
    buf.flip();
    this.buf.clear();
    
    int read = channel.read(this.buf);
    
    if (read > 0)
    {
      this.buf.flip();
      
      while(this.buf.hasRemaining())
      {
        SSLEngineResult result = engine.unwrap(this.buf,buf);
        
        switch(result.getStatus())
        {
          case OK: 
            buf.flip(); 
            break; 
          
          case BUFFER_OVERFLOW:
            buf = enlarge(buf);
            break;
          
          case BUFFER_UNDERFLOW:
            this.buf = enlarge(this.buf);
            break;
        }
      }
    }
    
    
    System.out.println("Read "+read);
    return(read);
  }
  
  
  public void write(ByteBuffer buf) throws Exception
  {
    buf.flip();
    this.buf.clear();
    
    SSLEngineResult result = engine.wrap(buf,this.buf);
    
    while(buf.hasRemaining())
    {
      switch(result.getStatus())
      {
        case OK:
          this.buf.flip();
          
          while(this.buf.hasRemaining())
            channel.write(this.buf);
          
          break;
        
        case BUFFER_OVERFLOW:
          this.buf = enlarge(this.buf);
          break;
        
        case BUFFER_UNDERFLOW:
          buf = enlarge(buf);
          break;
      }
    }
        
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
            
            if (result.getStatus() == SSLEngineResult.Status.OK)
              ssl.compact();
            
            if (!handle(result)) return(close());
            break;
          
          case NEED_WRAP:
            buf.clear();            
            result = engine.wrap(ssl,buf);

            if (result.getStatus() == SSLEngineResult.Status.OK)
            {
              buf.flip();
              
              while(buf.hasRemaining())
                channel.write(buf);
            }

            if (!handle(result)) return(close());            
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
      
      //if (errm.startsWith("Illegal server handshake")) skip = true;
      if (!skip) e.printStackTrace();
      
      //return(close());
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
    this.buf = enlarge(buf);
    this.ssl = enlarge(ssl);
  }
  
  
  private ByteBuffer enlarge(ByteBuffer buf)
  {
    ByteBuffer bufc = buf;
    int size = 2 * buf.capacity();

    buf = ByteBuffer.allocate(size);
    
    bufc.flip();
    buf.put(bufc);
    
    return(buf);
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
