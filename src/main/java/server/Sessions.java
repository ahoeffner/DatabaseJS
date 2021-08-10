package server;

import java.util.Date;
import java.text.SimpleDateFormat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Sessions
{
  private static final SimpleDateFormat format = 
    new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");

  private static final ConcurrentHashMap<String,Session> sessions =
    new ConcurrentHashMap<String,Session>();
  
  
  public static void logout(String uid)
  {
  }
  
  
  public static void register(String client, String uid)
  {
    sessions.put(uid,new Session().client(client).uid(uid));
  }
  
  
  public static void list()
  {
    System.out.println();
    for(Map.Entry<String,Session> entry : sessions.entrySet())
      System.out.println(entry.getValue());
    System.out.println();
  }
  
  
  public static class Session
  {
    private String uid = null;
    private String client = null;
    private long touched = System.currentTimeMillis();


    public Session touch()
    {
      this.touched = System.currentTimeMillis();;
      return(this);
    }

    public long touched()
    {
      return(touched);
    }

    public Session uid(String uid)
    {
      this.uid = uid;
      return(this);
    }

    public String uid()
    {
      return(uid);
    }

    public Session client(String client)
    {
      this.client = client;
      return(this);
    }

    public String client()
    {
      return(client);
    }
    
    
    @Override
    public String toString()
    {
      String date = format.format(new Date(touched));
      return(uid+"  ["+client+"] "+date);
    }
  }
}
