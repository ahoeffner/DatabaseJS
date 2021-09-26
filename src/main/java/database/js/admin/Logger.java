package database.js.admin;

import database.js.logger.Formatter;
import java.util.logging.FileHandler;


public class Logger
{
  public java.util.logging.Logger getLogger(String logfile, int size, int count) throws Exception
  {
    java.util.logging.Logger logger = java.util.logging.Logger.getLogger("admin");

    Formatter formatter = new Formatter();        
    FileHandler handler = new FileHandler(logfile,size,count,true);
    handler.setFormatter(formatter);

    return(logger);
  }
}
