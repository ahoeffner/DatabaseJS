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

package database.js.database;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DatabaseUtils
{
  public static ArrayList<String> parse(String url)
  {
    ArrayList<String> connstr = new ArrayList<String>();
    Pattern pattern = Pattern.compile("\\[(username|password)\\]");
    Matcher matcher = pattern.matcher(url.toLowerCase());

    int pos = 0;
    while(matcher.find())
    {
      int e = matcher.end();
      int b = matcher.start();

      if (b > pos)
        connstr.add(url.substring(pos,b));

      connstr.add(url.substring(b,e).toLowerCase());
      pos = e;
    }

    if (pos < url.length())
      connstr.add(url.substring(pos));

    return(connstr);
  }


  public static String bind(ArrayList<String> urlparts, String username, String password)
  {
    String url = "";

    for(String part : urlparts)
    {
      if (part.equals("[username]")) url += username;
      else if (part.equals("[password]")) url += password;
      else url += part;
    }

    return(url);
  }
}