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

package database.js.sso;

import database.js.admin.Client;
import database.js.config.SSOConfig;


public class Authenticater
{
  private final Client client;


  public static void main(String[] args) throws Exception
  {
    if (args.length != 2)
    {
      System.out.println("Usage: server:port username");
      System.exit(-1);
    }

    Authenticater auth = new Authenticater(args[0]);
    System.out.println(auth.authenticate(args[1]));
  }


  public Authenticater(String url) throws Exception
  {
    SSOConfig config = new SSOConfig();
    Client.setConfig(config.pkictx,1024,20000);

    if (url.startsWith("http://"))
      url = url.substring(7);

    if (url.startsWith("https://"))
      url = url.substring(8);

    int pos = url.indexOf(':') + 1;
    if (pos < 0) throw new Exception("Server must include port [localhost:443]");

    String host = url.substring(0,pos-1);
    int port = Integer.parseInt(url.substring(pos));

    this.client = new Client(host,port,true);
    this.client.connect();
  }


  public String authenticate(String username) throws Exception
  {
    byte[] response = client.send("authenticate",username);
    return(new String(response));
  }
}