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

package database.js.servers.http;

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
      this.buffers.nossl();
    }
    else
    {
      PKIContext pki = config.getPKIContext();
      this.engine = pki.getSSLContext().createSSLEngine();

      this.engine.setUseClientMode(false);
      this.engine.setNeedClientAuth(twoway);

      this.buffers.setSize(appsize(),packsize());
      this.worker = Executors.newSingleThreadExecutor();
    }

    this.logger = config.getLogger().logger;
  }


  public ByteBuffer read() throws Exception
  {
    if (ssl) return(readssl());
    else     return(readplain());
  }


  private ByteBuffer readplain() throws Exception
  {
    buffers.data.clear();

    int read = channel.read(buffers.data);
    if (read <= 0) return(null);

    buffers.data.flip();
    return(buffers.data);
  }


  private ByteBuffer readssl() throws Exception
  {
    buffers.send.clear();

    int read = channel.read(buffers.send);
    if (read < 0) return(null);

    buffers.send.flip();
    while(buffers.send.hasRemaining())
    {
      buffers.data.clear();
      SSLEngineResult result = engine.unwrap(buffers.send,buffers.data);

      switch(result.getStatus())
      {
        case OK:
          buffers.data.flip();
          break;

        case BUFFER_OVERFLOW:
          buffers.data = enlarge(buffers.data,appsize());
          break;

        case BUFFER_UNDERFLOW:
          if (buffers.send.limit() < packsize())
            buffers.send = enlarge(buffers.send,packsize());
          break;

        case CLOSED:
          return(null);
      }
    }

    return(buffers.data);
  }


  public void write(byte[] data) throws Exception
  {
    int wrote = 0;
    int max = buffers.data.limit();

    int size = data.length;
    int bsize = buffers.data.capacity();

    while(wrote < size)
    {
      int chunk = bsize;
      buffers.data.clear();

      if (chunk > size - wrote)
        chunk = size - wrote;

      if (chunk > max)
        chunk = max;

      buffers.data.put(data,wrote,chunk);

      buffers.data.flip();

      if (ssl) writessl();
      else     writeplain();

      wrote += chunk;
    }
  }


  private void writeplain() throws Exception
  {
    while(buffers.data.hasRemaining())
      channel.write(buffers.data);
  }


  private void writessl() throws Exception
  {
    while(buffers.data.hasRemaining())
    {
      buffers.send.clear();
      SSLEngineResult result = engine.wrap(buffers.data,buffers.send);

      switch(result.getStatus())
      {
        case OK:
          buffers.send.flip();

          while(buffers.send.hasRemaining())
            channel.write(buffers.send);

          break;

        case BUFFER_OVERFLOW:
          buffers.send = enlarge(buffers.send,packsize());
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

    buffers.init();

    try
    {
      engine.beginHandshake();

      while(cont)
      {
        status = engine.getHandshakeStatus();

        switch(status)
        {
          case NEED_UNWRAP:
            read = channel.read(buffers.recv);

            if (read < 0)
            {
              if (engine.isInboundDone() && engine.isOutboundDone())
                return(true);

              try {engine.closeInbound();}
              catch (Exception e) {;}

              engine.closeOutbound();
              break;
            }

            buffers.recv.flip();

            try
            {
              result = engine.unwrap(buffers.recv,buffers.data);
              buffers.recv.compact();
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
                buffers.data = enlarge(buffers.data,appsize());
                break;

              case BUFFER_UNDERFLOW:
                if (buffers.recv.limit() < packsize())
                  buffers.recv = enlarge(buffers.recv,packsize());
                break;

              case CLOSED:
                if (engine.isOutboundDone())
                  engine.closeOutbound();
                break;

              default:
                throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }
            break;

          case NEED_WRAP:
            buffers.send.clear();

            try
            {
              result = engine.wrap(buffers.data,buffers.send);
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
                buffers.send.flip();

                while(buffers.send.hasRemaining())
                  channel.write(buffers.send);
                break;

              case BUFFER_OVERFLOW:
                buffers.send = enlarge(buffers.send,packsize());
                break;

              case BUFFER_UNDERFLOW:
                throw new IllegalStateException("Unexpected behaivior");

              case CLOSED:
                try
                {
                  buffers.send.flip();

                  while(buffers.send.hasRemaining())
                    channel.write(buffers.send);

                  buffers.send.clear();
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
            cont = false;
            break;

          case NOT_HANDSHAKING:
            cont = false;
            logger.warning("SSL not handshaking");
            break;

          default:
            throw new IllegalStateException("Invalid SSL status: "+status);
        }
      }
    }
    catch (Exception e)
    {
      try {this.channel.close();}
      catch (Exception chc) {;}
      logger.log(Level.SEVERE,e.getMessage(),e);
    }

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
  }


  private ByteBuffer enlarge(ByteBuffer buf, int size) throws Exception
  {
    ByteBuffer bufc = buf;
    int left = buf.remaining();
    
    if (left < size)
    {
      buf = ByteBuffer.allocate(buf.position() + size);
      bufc.flip();
      buf.put(bufc);
    }
    
    return(buf);
  }  

  
  public int packsize()
  {
    return(engine.getSession().getPacketBufferSize());
  }
  
  
  public int appsize()
  {
    return(engine.getSession().getApplicationBufferSize());
  }
}
