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

import java.io.File;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.io.FileOutputStream;
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
    
    if (request.method().equals("GET"))
      return(get(request,response));

    JSONFormatter jfmt = new JSONFormatter();

    jfmt.success(true);
    jfmt.add("message","file uploaded");

    response.setBody(jfmt.toString());
    return(response);
  }


  private HTTPResponse get(HTTPRequest request, HTTPResponse response) throws Exception
  {
    String path = request.path();
    String root = config().getREST().fileroot;
    
    int pos = path.indexOf('/');
    System.out.println("Path = "+path);
    
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
    String root = config().getREST().fileroot;
    boolean tmpfiles = config().getREST().tmpfiles;

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

        Field field = new Field(tmpfiles,header,entry);

        if (field.name != null && field.name.equals("options"))
        {
          options = Request.parse(new String(field.content));
          field = null;
        }

        if (field != null)
        {
          if (field.srcfile != null) files.add(field);
          else                       fields.add(field);
        }
      }

      if (next == -1 || next + pattern.length + 4 == body.length)
        break;

      next += pattern.length + 1;
    }

    jfmt.success(true);

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
      String[] attrs = new String[] {"field","srcfile","dstfile","size"};

      for(Field field : files)
      {
        field.write(root,options);
        Object[] values = new Object[] {field.name,field.srcfile,field.dstfile,field.size};
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
    int size = 0;
    String name = null;
    String folder = "/";
    String srcfile = null;
    String dstfile = null;
    byte[] content = null;
    boolean tmpfile = false;


    Field(boolean tmpfile, String header, byte[] content)
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
      this.tmpfile = tmpfile;
      this.content = content;
      this.srcfile = filename;
      this.size = content.length;
    }


    void setOptions(String root, JSONObject options) throws Exception
    {
      if (options == null) return;
      if (!options.has(name)) return;

      options = options.getJSONObject(name);

      if (options.has("tmpfile"))
        tmpfile = options.getBoolean("tmpfile");

      if (options.has("dstfile"))
        dstfile = options.getString("dstfile");

      if (options.has("folder"))
        folder = options.getString("folder");

      if (!folder.startsWith("/"))
        folder = File.separator + folder;

      if (!folder.endsWith("/"))
        folder += File.separator;
      
      String file = srcfile;
      if (dstfile != null) file = dstfile;

      if (!checkpath(root,root+folder+file))
        throw new Exception("Illegal path specification "+folder+file);
    }


    void write(String root, JSONObject options) throws Exception
    {
      File dest = null;
      setOptions(root,options);
      File folder = new File(root+this.folder);

      folder.mkdirs();
      
      if (dstfile == null)
      {
        if (tmpfile)
        {
          String type = "";
          int pos = srcfile.lastIndexOf('.');
          if (pos > 0) type = srcfile.substring(pos);
          dest = File.createTempFile("App",type,folder);
          dstfile = this.folder + dest.getName();
        }
        else
        {
          dstfile = this.folder + srcfile;
          dest = new File(root + dstfile);
        }
      }
      else
      {
        dstfile = this.folder + dstfile;
        dest = new File(root + dstfile);        
      }

      FileOutputStream out = new FileOutputStream(dest);
      out.write(content);
      out.close();
    }


    @Override
    public String toString()
    {
      return("name="+name+" filename="+srcfile+" size="+size);
    }


    boolean checkpath(String root, String path)
    {
      try
      {
        File p = new File(path);

        if (p.getCanonicalPath().startsWith(root))
          return(true);

        return(false);
      }
      catch (Exception e)
      {
        return(false);
      }
    }
  }
}