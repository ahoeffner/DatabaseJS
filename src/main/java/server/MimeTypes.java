package server;

import java.util.HashMap;


public class MimeTypes
{
  private static final HashMap<String,String> contentTypes = new HashMap<String,String>();
  
  static
  {
    contentTypes.put("htm","text/html");
    contentTypes.put("html","text/html");
    contentTypes.put("txt","text/plain");
    contentTypes.put("gif","image/gif");
    contentTypes.put("jpg","image/jpg");
    contentTypes.put("jpeg","image/jpg");
    contentTypes.put("png","image/png");
    contentTypes.put("ico","image/x-icon");
    contentTypes.put("gzip","application/gzip");
    contentTypes.put("json","application/json; charset=utf-8");
    contentTypes.put("js","application/javascript; charset=UTF-8");
  };
  
  
  public static String getContentType(String type)
  {
    if (type == null) return("text/html");
    return(contentTypes.get(type));
  }
}