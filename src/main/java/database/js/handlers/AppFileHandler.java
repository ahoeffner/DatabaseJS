/*
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.

 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

package database.js.handlers;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.servers.Server;
import database.js.handlers.rest.Request;
import database.js.handlers.file.PathUtil;
import database.js.servers.rest.RESTClient;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;
import database.js.handlers.rest.JSONFormatter;
import database.js.config.Handlers.HandlerProperties;
import static database.js.handlers.rest.JSONFormatter.Type.*;


public class AppFileHandler extends Handler
{
  private final PathUtil path;
  private final CrossOrigin cors;
  private final Logger logger = Logger.getLogger("rest");


  public AppFileHandler(Config config, HandlerProperties properties) throws Exception
  {
    super(config,properties);
    this.cors = new CrossOrigin();
    this.path = new PathUtil(this);
  }


  @Override
  public HTTPResponse handle(HTTPRequest request) throws Exception
  {
    request.server().request();
    Server server = request.server();
    HTTPResponse response = new HTTPResponse();
    String path = this.path.getPath(request.path());
    String json = config().getHTTP().mimetypes.get("json");

    if (path == null)
    {
      response.setContentType(json);
      JSONFormatter jfmt = new JSONFormatter();

      jfmt.success(false);
      jfmt.add("message","Path not mapped to any resource");

      response.setBody(jfmt.toString());
      return(response);
    }

    server.request();
    String session = path.substring(1).split("/")[0];
    logger.finest("AppFile request received: "+request.path());

    response.setContentType(json);
    String errm = cors.allow(request);

    if (errm != null)
    {
      response.setBody(errm);
      return(response);
    }

    cors.addHeaders(request,response);

    if (!server.embedded())
    {
      errm = ensure(request,session);

      if (errm != null)
      {
        JSONFormatter jfmt = new JSONFormatter();

        jfmt.success(false);
        jfmt.add("message",errm);

        response.setBody(jfmt.toString());
        logger.warning(errm);
        return(response);
      }
    }

    if (request.getHeader("Content-Type").startsWith("multipart/form-data"))
      return(upload(request,response));

    JSONFormatter jfmt = new JSONFormatter();

    jfmt.success(true);
    jfmt.add("message","file uploaded");

    response.setBody(jfmt.toString());
    return(response);
  }


  private HTTPResponse upload(HTTPRequest request, HTTPResponse response) throws Exception
  {
    JSONFormatter jfmt = new JSONFormatter();
    String ctype = request.getHeader("Content-Type");
    String boundary = "--"+ctype.substring(ctype.indexOf("boundary=")+9);
    
    int next = 0;
    byte[] body = request.body();
    byte[] eoh = "\r\n\r\n".getBytes();
    byte[] pattern = boundary.getBytes();

    JSONObject options = null;
    ArrayList<Field> files = new ArrayList<Field>();
    ArrayList<Field> fields = new ArrayList<Field>();

    while(true)
    {
      int last = next + 1;
      next = indexOf(body,pattern,next);

      if (next > last)
      {
        // newline is \r\n
        // 2 newlines after header
        // 1 newline after content

        int head = indexOf(body,eoh,last+pattern.length);
        String header = new String(body,last,head-last);

        head += 4;
        byte[] entry = new byte[next-head-2];
        System.arraycopy(body,head,entry,0,entry.length);

        Field field = new Field(header,entry);
        
        if (field.name != null && field.name.equals("options"))
        {
          options = Request.parse(new String(field.content));
          field = null;
        }
        
        if (field != null)
        {
          if (field.filename != null) files.add(field);
          else                        fields.add(field);
        }
      }

      if (next == -1 || next + pattern.length + 4 == body.length)
        break;

      next += pattern.length + 1;
    }

    jfmt.success(true);
    System.out.println("options="+options);

    if (fields.size() > 0)
    {
      jfmt.push("fields",ObjectArray);
      String[] attrs = new String[] {"field","value"};

      for(Field field : fields)
      {
        String[] values = new String[] {field.name,new String(field.content)};
        jfmt.add(attrs,values);
      }
      
      jfmt.pop();
    }

    if (files.size() > 0)
    {
      jfmt.push("files",ObjectArray);
      String[] attrs = new String[] {"field","filename","size"};

      for(Field field : files)
      {
        Object[] values = new Object[] {field.name,field.filename,field.size};
        jfmt.add(attrs,values);
      }
      
      jfmt.pop();
    }

    response.setBody(jfmt.toString());
    return(response);
  }


  private String ensure(HTTPRequest request, String session) throws Exception
  {
    if (session == null || session.length() == 0)
      return("Not connected");

    short rsrv = RestHandler.getClient(config(),request);
    logger.info("Restserver = "+rsrv); rsrv = 2;

    if (rsrv < 0)
      return("Not connected");

    RESTClient client = request.server().worker(rsrv);

    if (client == null)
      return("Could not connect to RESTServer");

    String ensure = "";
    String nl = "\r\n";

    ensure += "POST /"+session+"/status HTTP/1.1"+nl+"Host: localhost"+nl+nl;
    byte[] data = client.send("localhost",ensure.getBytes());

    HTTPResponse response = new HTTPResponse(data);
    JSONObject status = Request.parse(new String(response.body()));

    logger.info("status: "+status.getBoolean("success"));
    return(null);
  }


  public static int indexOf(byte[] data, byte[] pattern, int start)
  {
    for(int i = start; i < data.length - pattern.length + 1; i++)
    {
        boolean found = true;

        for(int j = 0; j < pattern.length; ++j)
        {
           if (data[i+j] != pattern[j])
           {
               found = false;
               break;
           }
        }

        if (found) return(i);
     }

    return(-1);
  }


  private static class Field
  {
    final int size;
    final String name;
    final byte[] content;
    final String filename;


    Field(String header, byte[] content)
    {
      int pos1 = 0;
      int pos2 = 0;
      String name = null;
      String filename = null;

      pos1 = header.indexOf("name=");

      if (pos1 >= 0)
      {
        pos1 += 6;
        pos2 = header.indexOf('"',pos1);
        if (pos2 >= 0) name = header.substring(pos1,pos2);
      }

      pos1 = header.indexOf("filename=");

      if (pos1 >= 0)
      {
        pos1 += 10;
        pos2 = header.indexOf('"',pos1);
        if (pos2 >= 0) filename = header.substring(pos1,pos2);
      }

      this.name = name;
      this.content = content;
      this.filename = filename;
      this.size = content.length;
    }


    @Override
    public String toString()
    {
      return("name="+name+" filename="+filename+" size="+size+" <"+new String(content)+">");
    }
  }
}