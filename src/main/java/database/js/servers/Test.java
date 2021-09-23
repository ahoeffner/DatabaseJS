package database.js.servers;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.nio.channels.spi.SelectorProvider;

import java.util.Collections;
import java.util.Set;


public class Test extends ServerSocketChannel
{
  public Test(SelectorProvider providor) throws Exception
  {
    super(providor);
  }

  @Override
  public ServerSocketChannel bind(SocketAddress local, int backlog) throws IOException
  {
    System.out.println("bind");
    return null;
  }

  @Override
  public <T> ServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException
  {
    System.out.println("setOption");
    return null;
  }

  @Override
  public ServerSocket socket()
  {
    System.out.println("socket");
    return null;
  }

  @Override
  public SocketChannel accept() throws IOException
  {
    System.out.println("accept");
    return null;
  }

  @Override
  public SocketAddress getLocalAddress() throws IOException
  {
    System.out.println("getLocalAddress");
    return null;
  }

  @Override
  protected void implCloseSelectableChannel() throws IOException
  {
    System.out.println("implCloseSelectableChannel");
  }

  @Override
  protected void implConfigureBlocking(boolean block) throws IOException
  {
    System.out.println("implConfigureBlocking");
  }

  @Override
  public <T> T getOption(SocketOption<T> name) throws IOException
  {
    System.out.println("getOption");
    return null;
  }

  @Override
  public Set<SocketOption<?>> supportedOptions()
  {
    System.out.println("supportedOptions");
    return Collections.emptySet();
  }
}
