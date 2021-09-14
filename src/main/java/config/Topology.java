package config;

import org.json.JSONObject;


public class Topology
{
  private final Type type;
  private final int servers;
  private final boolean hot;
  
  
  Topology(JSONObject config) throws Exception
  {
    String type = config.getString("type");
    
    if (type.toLowerCase().equals("micro")) this.type = Type.Micro;
    else                                    this.type = Type.Cluster;
    
    if (this.type == Type.Micro) servers = 1;
    else  servers = config.getInt("servers");
    
    this.hot = config.getBoolean("hot-standby");
    System.out.println("hot = "+hot);
  }


  public Type type()
  {
    return(type);
  }

  public int servers()
  {
    return(servers);
  }

  public boolean hotstandby()
  {
    return(hot);
  }


  public static enum Type
  {
    Micro,
    Cluster
  }
}
