package config;

import org.json.JSONObject;


public class Cluster
{
  public final int check;
  public final int instances;
  
  
  public Cluster(JSONObject section) throws Exception
  {
    check = section.getInt("check") * 1000; 
    instances = section.getInt("instances"); 
  }
}
