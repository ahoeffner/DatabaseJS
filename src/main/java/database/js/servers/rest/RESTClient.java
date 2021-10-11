package database.js.servers.rest;

import java.util.logging.Logger;
import database.js.servers.Server;
import database.js.servers.http.HTTPChannel;


public class RESTClient
{
  private short id = -1;
  private long started = -1;
  
  private final Logger logger;
  private final Server server;
  private final HTTPChannel channel;


  public RESTClient(HTTPChannel channel, short id) throws Exception
  {
    this.id = id;
    this.channel = channel;
    this.server = channel.server();
    this.started = System.currentTimeMillis();
    this.logger = server.config().getLogger().rest;
  }
  
  
  public short id()
  {
    return(id);
  }
  
  
  Logger logger()
  {
    return(logger);
  }
  
  
  public long started()
  {
    return(started);
  }
}
