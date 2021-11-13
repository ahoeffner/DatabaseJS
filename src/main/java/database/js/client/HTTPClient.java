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

package database.js.client;

import java.net.Socket;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.OutputStream;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLContext;


public class HTTPClient
{
  private int psize;
  private Socket socket;
  private final int port;
  private final String host;
  private final SSLContext ctx;


  public HTTPClient(String host, int port, SSLContext ctx)
  {
    this.ctx = ctx;
    this.host = host;
    this.port = port;
  }


  public byte[] send(byte[] page) throws Exception
  {
    InputStream in = socket.getInputStream();
    OutputStream out = socket.getOutputStream();
    SocketReader reader = new SocketReader(in);

    int w = 0;
    while(w < page.length)
    {
      int size = psize;

      if (size > page.length - w)
        size = page.length - w;

      byte[] chunk = new byte[size];
      System.arraycopy(page,w,chunk,0,size);

      w += size;
      out.write(chunk);
      out.flush();
    }

    ArrayList<String> headers = reader.getHeader();

    int cl = 0;
    boolean chunked = false;

    for(String header : headers)
    {
      if (header.startsWith("Content-Length"))
        cl = Integer.parseInt(header.split(":")[1].trim());

      if (header.startsWith("Transfer-Encoding") && header.contains("chunked"))
        chunked = true;
    }

    byte[] response = null;

    if (cl > 0) response = reader.getContent(cl);
    else if (chunked) response = reader.getChunkedContent();

    return(response);
  }


  public void connect() throws Exception
  {
    if (ctx == null)
    {
      this.socket = new Socket(host,port);
    }
    else
    {
      this.socket = ctx.getSocketFactory().createSocket(host,port);
      ((SSLSocket) socket).startHandshake();
    }

    this.psize = this.socket.getSendBufferSize();

    this.socket.setSoTimeout(15000);
    this.socket.setSendBufferSize(psize);
    this.socket.getOutputStream().flush();
  }
}