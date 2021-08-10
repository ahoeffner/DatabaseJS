package handlers;

import java.io.File;
import config.Config;
import java.util.Date;
import server.MimeTypes;
import java.util.TimeZone;
import server.HTTPRequest;
import server.HTTPResponse;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;


public class FileHandler implements Handler
{
  private static final SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
  static {format.setTimeZone(TimeZone.getTimeZone("GMT"));}


  @Override
  public void handle(Config config, HTTPRequest request, HTTPResponse response) throws Exception
  {
    String path = config.http.path;
    String vers = config.http.version;
    
    if (vers.length() > 0) 
      path += "/" + vers;
    
    path += request.getPath();
    File file = new File(path);
    
    if (file.isDirectory()) 
      file = new File(path+"/index.html");

    String type = null;
    int pos = file.getName().lastIndexOf('.');
    if (pos > 0) type = file.getName().substring(pos+1);
    
    // Javascript apps only has index.html
    if (!file.exists() && type == null)
    {
      String idxpath = request.getPath();

      while(true)
      {
        pos = idxpath.lastIndexOf('/');
        if (pos < 0) break;

        idxpath = idxpath.substring(0,pos);
        file = new File(path+idxpath+"/index.html");
        
        if (file.exists()) 
        {
          pos = file.getName().lastIndexOf('.');
          if (pos > 0) type = file.getName().substring(pos+1);
          break;          
        }
      }
    }

    if (!file.exists())
    {
      String body = "<html><head>\n" +
      "<title>404 Not Found</title>\n" +
      "</head><body>\n" +
      "<h1>Not Found</h1>\n" +
      "<p>The requested URL was not found on this server.</p>\n" +
      "</body></html>\n";

      response.setBody(body.getBytes());
      response.setCode("404 Page Not Found");
      response.setHeader("Content-Type","text/html");
      return;
    }

    FileInfo info = new FileInfo(file);
    String last = request.getHeader("If-Modified-Since");
    
    if (last != null && last.equals(info.moddate))
    {
      response.setCode("304 Not Modified");
      return;
    }

    response.setHeader("Last-Modified",info.moddate);
    response.setHeader("Content-Type",MimeTypes.getContentType(type));

    int len = (int) file.length();
    byte[] body = new byte[len];

    FileInputStream in = new FileInputStream(file);

    in.read(body);
    in.close();

    response.setBody(body);
  }


  private static class FileInfo
  {
    final long mod;
    final String moddate;

    FileInfo(File file)
    {
      mod = file.lastModified();
      moddate = format.format(new Date(mod))+" GMT";
    }
  }
}
