package handlers;

import config.Config;
import server.HTTPRequest;
import server.HTTPResponse;


public interface Handler
{
  void handle(Config config, HTTPRequest request, HTTPResponse response) throws Exception;
}
