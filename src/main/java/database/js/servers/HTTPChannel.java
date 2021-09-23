/*
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.

 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

package database.js.servers;

import java.nio.ByteBuffer;
import javax.net.ssl.SSLEngine;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Config;
import javax.net.ssl.SSLEngineResult;
import java.util.concurrent.Executors;
import database.js.security.PKIContext;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;


public class HTTPChannel
{
  private final boolean ssl;
  private final Logger logger;
  private final SSLEngine engine;
  private final HTTPBuffers buffers;
  private final SocketChannel channel;
  private final ExecutorService worker;
  
  
  public HTTPChannel(Config config, HTTPBuffers buffers, SocketChannel channel, boolean ssl, boolean twoway) throws Exception
  {
    this.ssl = ssl;
    this.buffers = buffers;
    this.channel = channel;
    
    if (!ssl)
    {
      this.engine = null;
      this.worker = null;
    }
    else
    {
      this.buffers.usessl();

      PKIContext pki = config.getPKIContext();
      this.engine = pki.getSSLContext().createSSLEngine();
      
      this.engine.setUseClientMode(false);
      this.engine.setWantClientAuth(twoway);

      this.worker = Executors.newSingleThreadExecutor();
    }    
    
    this.logger = config.getLogger().logger;
  }
  
  
  public ByteBuffer read() throws Exception
  {
    buffers.plain.clear();
    if (ssl) return(readssl());
    else     return(readplain());
  }
  
  
  private ByteBuffer readplain() throws Exception
  {
    int read = channel.read(buffers.plain);    
    if (read <= 0) return(null);
    buffers.plain.flip();
    return(buffers.plain);
  }
  
  
  private ByteBuffer readssl() throws Exception
  {
    buffers.plain.clear();
    buffers.encpt.clear();
    
    int read = channel.read(buffers.encpt);
    if (read < 0) return(null);
    
    buffers.encpt.flip();
    while(buffers.encpt.hasRemaining())
    {
      SSLEngineResult result = engine.unwrap(buffers.encpt,buffers.plain);
      
      switch(result.getStatus())
      {
        case OK:
          buffers.plain.flip();
          break;
        
        case BUFFER_OVERFLOW:
          buffers.plain = enlarge(buffers.plain);
          break;
        
        case BUFFER_UNDERFLOW:
          buffers.plain = enlarge(buffers.plain);
          break;
        
        case CLOSED:
          return(null);
      }
    }
    
    return(buffers.plain);
  }
  
  
  public void write(byte[] data) throws Exception
  {
    int wrote = 0;
    int max = HTTPBuffers.chunk;
    
    int size = data.length;
    int bsize = buffers.plain.capacity();
    
    while(wrote < size)
    {
      int chunk = bsize;
      buffers.plain.clear();
      
      if (chunk > size - wrote)
        chunk = size - wrote;
      
      if (chunk > max)
        chunk = max;
      
      buffers.plain.put(data,wrote,chunk);
      
      buffers.plain.flip();

      if (ssl) writessl();
      else     writeplain();

      wrote += chunk;
    }
  }
  
  
  private void writeplain() throws Exception
  {
    while(buffers.plain.hasRemaining())
      channel.write(buffers.plain);
  }
  
  
  private void writessl() throws Exception
  {
    while(buffers.plain.hasRemaining())
    {
      buffers.encpt.clear();
      SSLEngineResult result = engine.wrap(buffers.plain,buffers.encpt);
      
      switch(result.getStatus())
      {
        case OK:
          buffers.encpt.flip();
          
          while(buffers.encpt.hasRemaining())
            channel.write(buffers.encpt);
          
          break;
        
        case BUFFER_OVERFLOW:
          buffers.encpt = enlarge(buffers.encpt);
          break;
        
        case BUFFER_UNDERFLOW:
          buffers.encpt = enlarge(buffers.encpt);
          break;
      }
    }
  }
  
  
  public boolean accept()
  {
    if (ssl) return(sslaccept());
    else     return(plainaccept());
  }
  
  
  private boolean plainaccept()
  {
    return(true);
  }
  
  
  private boolean sslaccept()
  {
    int read;
    boolean cont = true;
    
    SSLEngineResult result = null;
    HandshakeStatus status = null;
    
    buffers.plain.clear();
    buffers.encpt.clear();
    
    try
    {
      engine.beginHandshake();
      
      while(cont)
      {
        status = engine.getHandshakeStatus();
        
        switch(status)
        {
          case NEED_UNWRAP:
            read = channel.read(buffers.encpt);
            if (read < 0) return(close());

            buffers.encpt.flip();
            result = engine.unwrap(buffers.encpt,buffers.plain);
                        
            if (result.getStatus() == SSLEngineResult.Status.OK)
              buffers.encpt.compact();
            
            if (!handle(result)) return(close());
            break;
          
          case NEED_WRAP:
            buffers.plain.flip();
            buffers.encpt.clear();
            
            result = engine.wrap(buffers.plain,buffers.encpt);

            if (result.getStatus() == SSLEngineResult.Status.OK)
            {
              buffers.encpt.flip();
              
              while(buffers.encpt.hasRemaining())
                channel.write(buffers.encpt);
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
      
      if (errm.startsWith("Tag mismatch!")) skip = true;
      if (errm.startsWith("Received fatal alert: certificate_unknown")) skip = true;
      
      if (!skip) logger.log(Level.SEVERE,e.getMessage(),e);
      System.out.println(errm);
    }

    buffers.reset();
    if (result == null) return(true);
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
    buffers.plain = enlarge(buffers.plain);
    buffers.encpt = enlarge(buffers.encpt);
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

    return(false);
  }
}
