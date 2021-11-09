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

import java.util.logging.Level;
import java.util.logging.Logger;
import database.js.config.Config;
import database.js.control.Process.Type;
import database.js.handlers.file.Deployment;
import database.js.servers.http.HTTPRequest;
import database.js.servers.http.HTTPResponse;
import database.js.config.Handlers.HandlerProperties;
import database.js.handlers.file.Deployment.StaticFile;


public class FileHandler extends Handler
{
  private final PathUtil path;


  public FileHandler(Config config, HandlerProperties properties) throws Exception
  {
    super(config,properties);
    this.path = new PathUtil(this);
  }


  @Override
  public HTTPResponse handle(HTTPRequest request) throws Exception
  {
    request.server().request();
    Logger logger = this.getLogger(Type.http);
    HTTPResponse response = new HTTPResponse();
    String path = this.path.getPath(request.path());

    StaticFile file = Deployment.get().get(path);

    String caching = request.getHeader("Cache-Control");
    String encodings = request.getHeader("Accept-Encoding");
    String modified = request.getHeader("If-Modified-Since");

    if (file == null)
    {
      if (Deployment.isDirectory(path))
        file = Deployment.get().get(path+"/index.html");
    }

    if (file == null)
    {
      String vendp = config().getHTTP().getVirtualEndpoint();
      if (vendp != null) file = Deployment.get().get(vendp);
    }

    if (file == null)
    {
      response.setResponse(403);
      response.setContentType("text/html");
      response.setBody("<b>Page not found</b><br><br>"+
                       "The requested URL \""+request.path()+"\" was not found on this server.");
      return(response);
    }

    boolean reload = true;
    String changed = Deployment.modstring();

    if (modified != null && modified.equals(changed))
    {
      reload = false;

      if (caching != null && (caching.contains("max-age=0") || caching.contains("no-cache")))
        reload = true;
    }

    if (!reload)
    {
      // Send Not modified
      response.setResponse(304);
      log(logger,request,response);
      return(response);
    }


    boolean gzip = false;
    byte[] content = null;

    if (file.compressed)
      gzip = (encodings != null && encodings.contains("gzip"));

    try
    {
      content = file.get(gzip);
      if (gzip) response.setHeader("Content-Encoding","gzip");
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE,e.getMessage(),e);

      response.setResponse(500);
      response.setContentType("text/html");
      response.setBody("<b>Internal Server Error</b>");
      return(response);
    }

    String ext = file.fileext();
    String mimetype = config().getHTTP().mimetypes().get(ext);

    response.setBody(content);
    response.setContentType(mimetype);
    response.setLastModified(Deployment.modstring(),Deployment.modified());

    log(logger,request,response);
    return(response);
  }


  private void log(Logger logger, HTTPRequest request, HTTPResponse response)
  {
    long time = System.nanoTime() - request.start();

    if (logger.getLevel() == Level.FINE)
      logger.log(logger.getLevel(),request.path()+" ["+time/1000000+"]ms");

    if (logger.getLevel() == Level.FINER)
      logger.log(logger.getLevel(),request.path()+" ["+time/1000000+"]ms\n\n"+request.header()+"\n\n"+response.header()+"\n");

    if (logger.getLevel() == Level.FINEST)
      logger.log(logger.getLevel(),request.path()+" ["+time/1000000+"]ms\n\n"+new String(request.page())+"\n\n"+new String(response.page()));
  }
}