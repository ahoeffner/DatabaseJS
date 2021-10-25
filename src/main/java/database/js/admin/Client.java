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
import database.js.config.Config;
import database.js.client.HTTPRequest;
import database.js.security.PKIContext;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;


public class Client
{
  private Socket socket;
  private final int port;
  private final int psize;
  private final String host;
  private final PKIContext pki;


  public Client(String host, int port, boolean ssl) throws Exception
  {
    this.host = host;
    this.port = port;

    if (!ssl) 
    {
      this.pki = null;
      this.psize = 4096;
    }
    else 
    {
      this.pki = Config.PKIContext();
      SSLEngine engine = pki.getSSLContext().createSSLEngine();
      this.psize = engine.getSession().getPacketBufferSize();
    }
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

    out.write(request.getPage());    
    ArrayList<String> headers = reader.getHeader();

    int cl = 0;
    for(String header : headers)
    {
      if (header.startsWith("Content-Length"))
        cl = Integer.parseInt(header.split(":")[1].trim());
    }

    byte[] response = null;
    if (cl > 0) response = reader.getContent(cl);

    return(response);
  }


  public void connect() throws Exception
  {
    if (pki == null) this.socket = new Socket(host,port);
    else this.socket = pki.getSSLContext().getSocketFactory().createSocket(host,port);    
    if (pki != null) ((SSLSocket) socket).startHandshake();    
    this.socket.setSendBufferSize(psize);
    this.socket.setSoTimeout(15000);
  }
}