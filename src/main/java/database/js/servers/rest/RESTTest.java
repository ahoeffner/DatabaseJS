package database.js.servers.rest;

import org.json.JSONObject;
import org.json.JSONTokener;


public class RESTTest
{
  private final JSONObject payload;
  
  
  public RESTTest(String payload)
  {
    this.payload = new JSONObject(new JSONTokener(payload));
  }
  
  
  public int getInt(String name)
  {
    return(payload.getInt(name));
  }
  
  
  public long getLong(String name)
  {
    return(payload.getLong(name));
  }
}
