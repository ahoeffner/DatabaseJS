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
}
