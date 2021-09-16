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

package database.js.control;


import ipc.Broker;
import ipc.Message;
import ipc.Listener;
import java.util.ArrayList;
import java.util.logging.Logger;
import database.js.config.Config;
import static database.js.config.Config.*;


public class Server implements Listener
{
  private final Broker broker;
  private final Logger logger;
  private final Config config;
  private final Config.Type type;
  
  
  public static void main(String[] args) throws Exception
  {
    short id = Short.parseShort(args[0]);
    Server server = new Server(id);
    System.out.println("server started");
  }
  
  
  Server(short id) throws Exception
  {    
    this.config = new Config();

    int http = 1;
    if (config.getTopology().hotstandby()) http++;
    
    if (id < http) type = Type.http;
    else           type = Type.rest;
    
    boolean master = type == Type.http;
    this.logger = config.getLogger().logger;
    this.broker = new Broker(config.getIPConfig(),this,id,master);
  }
  
  
  private void start(short id) throws Exception
  {
    int ssl = config.getHTTP().ssl(); 
    int post = config.getHTTP().plain(); 
    int admin = config.getHTTP().admin(); 
  }


  @Override
  public void onServerUp(short s)
  {
  }

  @Override
  public void onServerDown(short s)
  {
  }

  @Override
  public void onNewManager(short s)
  {
  }

  @Override
  public void onMessage(ArrayList<Message> arrayList)
  {
  }
}
