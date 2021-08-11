package client;

public class HTTPRequest
{
  private String body;
  private final String host;
  private final String path;
  private static final String nl = "\r\n";
  
  
  public HTTPRequest(String host, String path)
  {
    this.host = host;
    this.path = path;
  }


  public void setBody(String body)
  {
    this.body = body;
  }
  
  
  public byte[] getPage()
  {
    String header = (body == null) ? "GET" : "POST";
    header += " "+path + "HTTP/1.0"+nl+"Host: "+host+nl;
    
    byte[] body = this.body == null ? null : this.body.getBytes();
    if (body != null) header += "Content-Length: "+body.length+nl;
    
    header += nl;
    byte[] head = header.getBytes();
    
    if (body == null) return(head);

    byte[] page = new byte[head.length + body.length];
    
    System.arraycopy(head,0,page,0,head.length);
    System.arraycopy(body,0,page,head.length,body.length);
    
    return(page);
  }
}
