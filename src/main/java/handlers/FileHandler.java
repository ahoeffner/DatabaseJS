package handlers;

import config.Config;
import server.HTTPRequest;
import server.HTTPResponse;


public class FileHandler implements Handler
{
  @Override
  public void handle(Config config, HTTPRequest request, HTTPResponse response)
  {
    response.setBody("Hello World".getBytes());
  }
}
