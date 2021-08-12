package config;

import java.util.Arrays;
import java.util.Collections;

import org.json.JSONObject;


public class Cluster
{
  public final int max;
  public final int check;
  public final int instances;
  public final Scale memscale;
  
  
  public Cluster(JSONObject section) throws Exception
  {
    int max = section.getInt("max");
    int check = section.getInt("check"); 

    this.instances = section.getInt("min");
    this.max = max < instances ? instances : max;
    this.check = check < 1 ? 1000 : check * 1000;
    
    String[] arr = null;
    String mem = section.getString("mem");
    String conn = section.getString("conn");
    
    arr = mem.split("[ ,]+");
    
    if (arr.length != 3) 
      throw new Exception("cluster mem must be 3 numbers e.g. 80,60,40");
    
    memscale = new Scale(arr);
  }
  
  
  public static class Scale
  {
    public final int low;
    public final int free;
    public final int high;
    
    public Scale(String[] args)
    {
      int[] n = new int[3];
      
      for (int i = 0; i < 3; i++)
        n[i] = Integer.parseInt(args[i]);

      Arrays.sort(n);
      
      this.low = n[0];
      this.free = n[1];
      this.high = n[2];
    }
  }
}
