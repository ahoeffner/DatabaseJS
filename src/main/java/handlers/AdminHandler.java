package handlers;

import config.Config;
import server.HTTPRequest;
import server.HTTPResponse;


public class AdminHandler implements Handler
{
  @Override
  public void handle(Config config, HTTPRequest request, HTTPResponse response) throws Exception
  {
    response.setBody("Admin".getBytes());
  }
}
