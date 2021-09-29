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

import java.util.ArrayList;
import java.io.InputStream;
import java.io.OutputStream;
import javax.net.ssl.SSLSocket;
import database.js.config.Config;
import database.js.security.PKIContext;


public class Client
{
  private int port;
  private String host;
  private SSLSocket socket;
  private final PKIContext pki;


  public Client(String host, int port) throws Exception
  {
    this.host = host;
    this.port = port;
    this.pki = Config.PKIContext();
  }


  public String send(String cmd) throws Exception
  {
    return(send(cmd,null));
  }


  public String send(String cmd, String message) throws Exception
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

    String response = null;
    if (cl > 0) response = new String(reader.getContent(cl));

    return(response);
  }


  public void connect() throws Exception
  {
    this.socket = (SSLSocket) pki.getSSLContext().getSocketFactory().createSocket(host,port);
  }
}