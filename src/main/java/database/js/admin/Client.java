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

package database.js.admin;

import java.net.Socket;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.OutputStream;
import javax.net.ssl.SSLSocket;
import database.js.client.HTTPRequest;
import database.js.security.PKIContext;
import database.js.client.SocketReader;


public class Client
{
  private Socket socket;
  private final int port;
  private final String host;
  private final boolean ssl;

  private static int psize;
  private static int timeout;
  private static PKIContext pki;

  public static void setConfig(PKIContext pki, int psize, int timeout)
  {
    Client.pki = pki;
    Client.psize = psize;
    Client.timeout = timeout;
  }


  public Client(String host, int port, boolean ssl) throws Exception
  {
    this.ssl  = ssl;
    this.host = host;
    this.port = port;
  }


  public byte[] send(String cmd) throws Exception
  {
    return(send(cmd,null));
  }


  public byte[] send(String cmd, String message) throws Exception
  {
    HTTPRequest request = new HTTPRequest(host,"/"+cmd);
    request.setBody(message);

    InputStream in = socket.getInputStream();
    OutputStream out = socket.getOutputStream();
    SocketReader reader = new SocketReader(in);

    byte[] page = request.page();

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
    if (!ssl)
    {
      this.socket = new Socket(host,port);
    }
    else
    {
      this.socket = pki.getSSLContext().getSocketFactory().createSocket(host,port);
      ((SSLSocket) socket).startHandshake();
    }

    this.socket.setSoTimeout(timeout);
    this.socket.getOutputStream().flush();
  }
}