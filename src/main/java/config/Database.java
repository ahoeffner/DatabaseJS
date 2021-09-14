package config;

import org.json.JSONObject;


public class Database
{
  private final Type type;
  
  private final int port;
  private final String host;
  private final String database;
  private final String teststmt;
  
  
  Database(JSONObject config) throws Exception
  {
    String type = config.getString("type");
    
    switch(type.toLowerCase())
    {
      case "oracle" : this.type = Type.Oracle; break;
      case "postgres" : this.type = Type.Postgres; break;
      default: throw new Exception("Unknown database type "+type);
    }
    
    this.port = config.getInt("port");
    this.host = config.getString("host");
    this.teststmt = config.getString("test");
    
    if (!config.has("database")) this.database = null;
    else                         this.database = config.getString("database");
  }


  public Type type()
  {
    return(type);
  }

  public String host()
  {
    return(host);
  }

  public int port()
  {
    return(port);
  }

  public String database()
  {
    return(database);
  }

  public String teststmt()
  {
    return(teststmt);
  }


  public static enum Type
  {
    Oracle,
    Postgres
  }
}
