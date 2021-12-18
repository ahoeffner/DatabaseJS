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

package database.js.logger;

import java.util.Date;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.logging.LogRecord;
import java.io.ByteArrayOutputStream;


public class Formatter extends java.util.logging.Formatter
{
  private final static String nl = System.lineSeparator();
  private final static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


  @Override
  public String format(LogRecord record)
  {
    String date = df.format(new Date());
    String location = record.getSourceClassName()+"."+record.getSourceMethodName();
    location = location.substring("database.js.".length());

    String message = ": "+record.getMessage();
    String level = String.format("%-7s",record.getLevel().toString());
    String source = String.format("%-48s",location);

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
      StackTraceElement[]  elements = record.getThrown().getStackTrace();

      StackTraceElement elem = elements[0];
      //String pos = elem.getClassName()+"."+elem.getMethodName()+"("+elem.getFileName()+":"+elem.getLineNumber()+")";

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      record.getThrown().printStackTrace(new PrintStream(out));
      message = " SEVERE  "+source+":"+nl+nl+new String(out.toByteArray());
    }

    entry.append(message+nl);
    return(entry.toString());
  }
}