package logger;

import java.io.ByteArrayOutputStream;

import java.io.PrintStream;

import java.util.Date;
import java.util.logging.LogRecord;
import java.text.SimpleDateFormat;


public class Formatter extends java.util.logging.Formatter
{
  private final static String nl = System.lineSeparator();
  private final static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
  
      
  @Override
  public String format(LogRecord record)
  {
    String date = df.format(new Date());
    
    String message = ": "+record.getMessage();
    String level = String.format("%-7s",record.getLevel().toString());
    String source = String.format("%-25s",record.getSourceClassName()+"."+record.getSourceMethodName());
    
    StringBuffer entry = new StringBuffer();
    boolean exception = (record.getThrown() != null);
    
    entry.append(date);
    
    if (!exception)
    {
      entry.append(" "+level);
      entry.append(" "+source);      
    }
    else
    {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      record.getThrown().printStackTrace(new PrintStream(out));
      message = " SEVERE :"+nl+nl+new String(out.toByteArray());
    }

    entry.append(message+nl);    
    return(entry.toString());
  }
}
