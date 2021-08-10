package server;

import java.util.TimeZone;
import java.util.ArrayList;
import java.text.SimpleDateFormat;

import java.util.Date;


public class HTTPResponse
{
  private byte[] body;
  private String ctype = null;
  private String code = "200 OK";
  private ArrayList<String> headers;
  private static final String newl = "\r\n";
  private static final SimpleDateFormat format = new SimpleDateFormat();

  static
  {
    format.setTimeZone(TimeZone.getTimeZone("GMT"));
  }


  public HTTPResponse(String host)
  {
    headers = new ArrayList<String>();

    setHeader("Host",host);
    setHeader("Server","database.js");
    setHeader("Cache-Control","no-cache");
    setHeader("Connection","keep-alive");
  }


  public void addCorsHeaders(String cors)
  {
    setContentType("text/plain");
    setHeader("Access-Control-Allow-Origin",cors);
    setHeader("Access-Control-Allow-Headers","*");
    setHeader("Access-Control-Request-Headers","*");
    setHeader("Access-Control-Request-Method","*");
  }


  public void setCode(String code)
  {
    this.code = code;
  }


  public void setContentType(String type)
  {
    this.ctype = type;
  }


  public void setBody(byte[] body)
  {
    this.body = body;
  }


  public void setHeader(String header, String value)
  {
    headers.add(header+": "+value);
  }


  public void setCookie(String cookie, String value)
  {
    Integer expires = null;
    setCookie(cookie,value,expires,"/");
  }


  public void setCookie(String cookie, String value, int seconds)
  {
    setCookie(cookie,value,seconds,"/");
  }


  public void setCookie(String cookie, String value, Integer expires, String path)
  {
    String expire = "";
    if (value == null) value = "";
    if (expires != null) expire = "; max-age="+expires;
    headers.add("Set-Cookie: "+cookie+"="+value+expire+"; path="+path);
  }


  public void setCookie(String cookie, String value, Date expires, String path)
  {
    String expire = "";
    if (value == null) value = "";
    if (expires != null) expire = "; expires="+format.format(expires);
    headers.add("Set-Cookie: "+cookie+"="+value+expire+"; path="+path);
  }


  public void removeCookie(String cookie)
  {
    removeCookie(cookie,"/");
  }


  public void removeCookie(String cookie, String path)
  {
    setCookie(cookie,null,-1,path);
  }


  public byte[] getBody()
  {
    if (body == null) return(new byte[0]);
    return(body);
  }


  public String getHeaders()
  {
    int length = 0;
    if (body != null) length = body.length;

    String header = "HTTP/1.1 " + code + newl +
                    "Content-Type: "+ctype + newl +
                    "Content-Length: "+length  + newl;

    for (String hdr : headers) header += hdr + newl;
    return(header);
  }


  public byte[] getPage()
  {
    int length = 0;
    if (body != null) length = body.length;

    String header = getHeaders() + newl;

    byte[] head = header.getBytes();
    if (body == null) return(head);

    byte[] page = new byte[head.length+length];
    System.arraycopy(head,0,page,0,head.length);
    System.arraycopy(body,0,page,head.length,length);
    return(page);
  }


  @Override
  public String toString()
  {
    return(new String(getPage()));
  }
}
