package database.js.servers.http;

import java.util.concurrent.ConcurrentHashMap;


public class HTTPCodes
{
  public static final ConcurrentHashMap<Integer,String> codes =
    new ConcurrentHashMap<Integer,String>();
  
  
  static
  {
    codes.put(200,"OK");
    codes.put(404,"Not Found");
    codes.put(400,"Bad Request");
    codes.put(304,"Not Modified");
    codes.put(301,"Moved Permanently");
    codes.put(503,"Service Unavailable");
  }
  
  
  public static String get(int code)
  {
    String reason = codes.get(code);
    return("HTTP/1.1 " + code + " " +reason);
  }
}
