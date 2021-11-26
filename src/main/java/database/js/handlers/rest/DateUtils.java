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

package database.js.handlers.rest;

import java.time.ZoneId;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class DateUtils
{
  public static void main(String[] args)
  {
    java.util.Date date = new java.util.Date();
    DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-YYYY");
    System.out.println(format(format,date));
  }
  
  
  public static boolean isDate(Object date)
  {
    if (date instanceof java.sql.Date || date instanceof java.util.Date) return(true);
    return(false);
  }
  
  
  public static long getTime(Object date)
  {
    if (date instanceof java.sql.Date) return(((java.sql.Date) date).getTime());
    if (date instanceof java.util.Date) return(((java.util.Date) date).getTime());
    return(0);
  }


  public static String format(DateTimeFormatter formatter, Object object)
  {
    java.util.Date date = null;
    
    if (object instanceof java.util.Date)
      date = (java.util.Date) object;

    if (object instanceof java.sql.Date) 
      date = new java.util.Date(((java.sql.Date) object).getTime());
    
    if (date == null)
      return(null);
    
    LocalDateTime locd = LocalDateTime.ofInstant(date.toInstant(),ZoneId.systemDefault());  
    return(formatter.format(locd));
  }
}
