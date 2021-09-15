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
import database.js.config.Config;


public class Server implements Listener
{
  public static void main(String[] args) throws Exception
  {
    short id = 0;
    System.out.println("server started");
    Server server = new Server(id);
  }
  
  
  Server(short id) throws Exception
  {    
    Config config = new Config();
    Broker broker = new Broker(config.getIPConfig(),this,id);
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
