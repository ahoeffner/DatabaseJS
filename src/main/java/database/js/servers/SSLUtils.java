package database.js.servers;

import java.nio.ByteBuffer;
import javax.net.ssl.SSLEngine;
import database.js.config.Config;
import javax.net.ssl.SSLEngineResult;
import java.util.concurrent.Executors;
import database.js.security.PKIContext;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngineResult.HandshakeStatus;


public class SSLUtils
{
  private ByteBuffer encpt;
  private ByteBuffer plain;
  private final Logger logger;
  private final SSLEngine engine;
  private final SocketChannel channel;
  private final ExecutorService worker;
  
  
  public SSLUtils(Config config, SocketChannel channel, boolean twoway) throws Exception
  {
    this.channel = channel;
    
    PKIContext pki = config.getPKIContext();
    this.engine = pki.getSSLContext().createSSLEngine();
    int size = engine.getSession().getApplicationBufferSize();
    
    this.encpt = ByteBuffer.allocate(size);
    this.plain = ByteBuffer.allocate(size);
    
    this.engine.setUseClientMode(false);
    this.engine.setWantClientAuth(twoway);
    
    this.logger = config.getLogger().logger;
    this.worker = Executors.newSingleThreadExecutor();
  }
  
  
  public ByteBuffer read() throws Exception
  {
    this.encpt.clear();
    this.plain.clear();
    
    int read = channel.read(this.encpt);
    
    this.encpt.flip();
    if (read < 0) return(null);
    
    while(this.encpt.hasRemaining())
    {
      SSLEngineResult result = engine.unwrap(this.encpt,this.plain);
      
      switch(result.getStatus())
      {
        case OK:
          this.plain.flip(); 
          break; 
        
        case BUFFER_OVERFLOW:
          this.plain = enlarge(this.plain);
          break;
        
        case BUFFER_UNDERFLOW:
          this.plain = enlarge(this.plain);
          break;
      }
    }
    
    return(this.plain);
  }
  
  
  public void write(byte[] data) throws Exception
  {
    this.plain.clear();
    this.plain.put(data);
    this.plain.flip();
    
    while(this.plain.hasRemaining())
    {
      this.encpt.clear();
      SSLEngineResult result = engine.wrap(this.plain,this.encpt);
      
      switch(result.getStatus())
      {
        case OK:
          this.encpt.flip();
          
          while(this.encpt.hasRemaining())
            channel.write(this.encpt);
          
          break;
        
        case BUFFER_OVERFLOW:
          this.encpt = enlarge(this.encpt);
          break;
        
        case BUFFER_UNDERFLOW:
          this.encpt = enlarge(this.encpt);
          break;
      }
    }
        
    //System.out.println("Write "+result.getStatus());
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
            read = channel.read(encpt);
            if (read < 0) return(close());

            encpt.flip();            
            result = engine.unwrap(encpt,plain);
            
            if (result.getStatus() == SSLEngineResult.Status.OK)
              encpt.compact();
            
            if (!handle(result)) return(close());
            break;
          
          case NEED_WRAP:
            plain.clear();            
            result = engine.wrap(encpt,plain);

            if (result.getStatus() == SSLEngineResult.Status.OK)
            {
              plain.flip();
              
              while(plain.hasRemaining())
                channel.write(plain);
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
      
      if (errm.startsWith("Received fatal alert: certificate_unknown")) skip = true;
      if (!skip) logger.log(Level.SEVERE,e.getMessage(),e);
    }

    return(result.getStatus() == SSLEngineResult.Status.OK);
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
    this.plain = enlarge(plain);
    this.encpt = enlarge(encpt);
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
