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
    buffers.myAppData.clear();
    if (ssl) return(readssl());
    else     return(readplain());
  }
  
  
  private ByteBuffer readplain() throws Exception
  {
    int read = channel.read(buffers.myAppData);    
    if (read <= 0) return(null);
    buffers.myAppData.flip();
    return(buffers.myAppData);
  }
  
  
  private ByteBuffer readssl() throws Exception
  {
    buffers.peerNetData.clear();
    
    int read = channel.read(buffers.peerNetData);
    if (read < 0) return(null);
    
    buffers.peerNetData.flip();
    while(buffers.peerNetData.hasRemaining())
    {
      buffers.peerAppData.clear();
      SSLEngineResult result = engine.unwrap(buffers.peerNetData,buffers.peerAppData);
      
      switch(result.getStatus())
      {
        case OK:
          buffers.peerAppData.flip();
          break;
        
        case BUFFER_OVERFLOW:
          buffers.peerAppData = enlarge(buffers.peerAppData);
          break;
        
        case BUFFER_UNDERFLOW:
          buffers.peerNetData = enlarge(buffers.peerNetData);
          break;
        
        case CLOSED:
          return(null);
      }
    }
    
    return(buffers.peerAppData);
  }
  
  
  public void write(byte[] data) throws Exception
  {
    int wrote = 0;
    int max = HTTPBuffers.wmax;
    
    int size = data.length;
    int bsize = buffers.myAppData.capacity();
    
    while(wrote < size)
    {
      int chunk = bsize;
      buffers.myAppData.clear();
      
      if (chunk > size - wrote)
        chunk = size - wrote;
      
      if (chunk > max)
        chunk = max;
      
      buffers.myAppData.put(data,wrote,chunk);
      
      buffers.myAppData.flip();

      if (ssl) writessl();
      else     writeplain();

      wrote += chunk;
    }
  }
  
  
  private void writeplain() throws Exception
  {
    while(buffers.myAppData.hasRemaining())
      channel.write(buffers.myAppData);
  }
  
  
  private void writessl() throws Exception
  {
    while(buffers.myAppData.hasRemaining())
    {
      buffers.myNetData.clear();
      SSLEngineResult result = engine.wrap(buffers.myAppData,buffers.myNetData);
      
      switch(result.getStatus())
      {
        case OK:
          buffers.myNetData.flip();
          
          while(buffers.myNetData.hasRemaining())
            channel.write(buffers.myNetData);
          
          break;
        
        case BUFFER_OVERFLOW:
          buffers.myNetData = enlarge(buffers.myNetData);
          break;
        
        case BUFFER_UNDERFLOW:
          throw new IllegalStateException("Unexpected behaivior");    
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
    
    buffers.myNetData.clear();
    buffers.peerNetData.clear();
    
    try
    {
      engine.beginHandshake();
      
      while(cont)
      {
        status = engine.getHandshakeStatus();
        System.out.println(status);
        
        switch(status)
        {
          case NEED_UNWRAP:
            read = channel.read(buffers.peerNetData);
            
            if (read < 0)
            {
              System.out.println("READ "+read);
              if (engine.isInboundDone() && engine.isOutboundDone())
                return(true);
              
              close();
              break;
            }

            buffers.peerNetData.flip();
            
            try
            {
              result = engine.unwrap(buffers.peerNetData,buffers.peerAppData);
              buffers.peerNetData.compact();
            }
            catch (Exception e)
            {
              handle(e);
              engine.closeOutbound();
              return(false);
            }
            
            switch(result.getStatus())
            {
              case OK:
                break;
              
              case BUFFER_OVERFLOW:
                buffers.peerAppData = enlarge(buffers.peerAppData);
                break;

              case BUFFER_UNDERFLOW:
                buffers.peerNetData = enlarge(buffers.peerNetData);
                break;
              
              case CLOSED:
                System.out.println("UNWRAP DONE "+engine.isOutboundDone());
                
                if (engine.isOutboundDone())
                  engine.closeOutbound();
                
                break;
              
              default:
                throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }

            break;
          
          case NEED_WRAP:
            buffers.myNetData.clear();
            
            try
            {
              result = engine.wrap(buffers.myAppData,buffers.myNetData);
            }
            catch (Exception e)
            {
              handle(e);
              engine.closeOutbound();
              return(false);
            }
            
            switch(result.getStatus())
            {
              case OK:
                buffers.myNetData.flip();
                
                while(buffers.myNetData.hasRemaining())
                  channel.write(buffers.myNetData);
                
                break;
              
              case BUFFER_OVERFLOW:
                buffers.myNetData = enlarge(buffers.myNetData);
                break;
              
              case BUFFER_UNDERFLOW:
                throw new IllegalStateException("Unexpected behaivior");    
              
              case CLOSED:
                try
                {
                  buffers.myNetData.flip();
                  
                  while(buffers.myNetData.hasRemaining())
                    channel.write(buffers.myNetData);
                  
                  buffers.myNetData.clear();                  
                  System.out.println("WRAP DONE");
                  
                  break;
                }
                catch (Exception e)
                {
                  logger.warning("Failed to send server's CLOSE message");
                }
                
                break;
              
              default:
                throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }

            break;
          
          case NEED_TASK:
            Runnable task = engine.getDelegatedTask();

            while(task != null)
            {
              worker.submit(task);
              task = engine.getDelegatedTask();
            }

            break;

          case FINISHED:
          case NOT_HANDSHAKING:
            cont = false;
            break;          

          default:
            throw new IllegalStateException("Invalid SSL status: "+status);
        }
      }
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);
    }
    
    System.out.println("DONE");
    
    //buffers.reset();
    if (result == null) return(true);
    return(result.getStatus() == SSLEngineResult.Status.OK);
  }
  
  
  private void handle(Exception e)
  {
    boolean skip = false;
    String errm = e.getMessage();
    if (errm == null) errm = "An unknown error has occured";      
    if (errm.startsWith("Received fatal alert: certificate_unknown")) skip = true;
    if (!skip) logger.log(Level.SEVERE,e.getMessage(),e);
    System.out.println(errm);
  }
  
  
  private ByteBuffer enlarge(ByteBuffer buf)
  {
    ByteBuffer bufc = buf;
    int size = 2 * buf.capacity();

    System.out.println("enlarge "+size);
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
