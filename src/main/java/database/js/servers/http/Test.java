package database.js.servers.http;

import java.nio.ByteBuffer;
import javax.net.ssl.SSLEngine;
import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Config;
import javax.net.ssl.SSLEngineResult;
import java.util.concurrent.Executors;
import database.js.security.PKIContext;

import java.io.IOException;

import java.net.InetSocketAddress;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

public class Test
{
  private ByteBuffer netRecvBuffer;
  private ByteBuffer netSendBuffer;
  private ByteBuffer appBuffer;
  private final Logger logger;
  private final SocketChannel channel;
  private final SSLEngine engine;
  
  
  public static void main(String[] args) throws Exception
  {
    Config config = new Config();
    
    Selector selector = Selector.open();

    ServerSocketChannel server = ServerSocketChannel.open();

    server.configureBlocking(false);
    server.bind(new InetSocketAddress(9003));

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

          Test test = new Test(config,sac);
          test.handshake();          
        }

        else if (key.isReadable())
        {
        }
      }
    }
  }


  public Test(Config config, SocketChannel channel) throws Exception
  {
    this.channel = channel;
    PKIContext pki = config.getPKIContext();
    this.engine = pki.getSSLContext().createSSLEngine();
    this.engine.setUseClientMode(false);
    this.engine.setNeedClientAuth(true);

    this.logger = config.getLogger().logger;
  }
  
  private void handshake() throws IOException {
          if (!channel.isConnected()) {
              throw new IllegalStateException("Channel must be connected");
          }
          logger.info("Beginning DTLS handshake");
          engine.beginHandshake();

          netRecvBuffer = ByteBuffer.allocateDirect(2048);
          appBuffer = ByteBuffer.allocateDirect(engine.getSession().getApplicationBufferSize());
          netSendBuffer = ByteBuffer.allocateDirect(engine.getSession().getPacketBufferSize());

          processEngineLoop(engine);
      }

  private void processEngineLoop(SSLEngine engine) throws IOException 
  {
          HandshakeStatus status = engine.getHandshakeStatus();
          while (status != HandshakeStatus.FINISHED && status != HandshakeStatus.NOT_HANDSHAKING) {
              SSLEngineResult result;
              logger.info("Handshake status: " + status);
              switch (status) {
                  case NEED_UNWRAP:
                      if (netRecvBuffer.position() == 0) {
                          channel.read(netRecvBuffer);
                      }

                      netRecvBuffer.flip();
                      result = engine.unwrap(netRecvBuffer, appBuffer);
                      netRecvBuffer.compact();
                      logger.info("Unwrap result: {} "+result);

                      while (result.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                          netRecvBuffer = ensureCapacity(netRecvBuffer,
                                  engine.getSession().getPacketBufferSize());
                          channel.read(netRecvBuffer);
                          netRecvBuffer.flip();
                          result = engine.unwrap(netRecvBuffer, appBuffer);
                          netRecvBuffer.compact();
                      }

                      status = result.getHandshakeStatus();
                      break;
                  case NEED_TASK:
                      Runnable task;
                      while ((task = engine.getDelegatedTask()) != null) {
                          logger.info("Running task: {} "+task);
                          task.run();
                      }
                      status = engine.getHandshakeStatus();
                      logger.info("Tasks executed");
                      break;
                  case NEED_WRAP:
                      appBuffer.flip();
                      result = engine.wrap(appBuffer, netSendBuffer);
                      appBuffer.compact();
                      logger.info("Wrap result: " + result);

                      netSendBuffer.flip();
                      if (netSendBuffer.hasRemaining()) {
                          channel.write(netSendBuffer);
                          netSendBuffer.compact();
                      }
                      status = result.getHandshakeStatus();
                      break;
                  default:
                      throw new IllegalStateException(
                              "Unexpected handshake state: " + status);
              }
          }
          logger.info("Handshake finished!");
  }

  private ByteBuffer ensureCapacity(ByteBuffer buffer, int requiredSize) {
      int remaining = buffer.remaining();
      if (remaining < requiredSize) {
          ByteBuffer newBuffer = ByteBuffer.allocate(buffer.position() + requiredSize);
        buffer.flip();
          newBuffer.put(buffer);
          return newBuffer;
      }
      return buffer;
  }
}
