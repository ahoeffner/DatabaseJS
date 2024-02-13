/*
  MIT License

  Copyright © 2023 Alex Høffner

  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
  and associated documentation files (the “Software”), to deal in the Software without
  restriction, including without limitation the rights to use, copy, modify, merge, publish,
  distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
  Software is furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all copies or
  substantial portions of the Software.

  THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
  BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package database.rest.handlers.rest.parser;

import org.json.JSONObject;
import java.io.FileInputStream;


public class Parser
{
   static final String CLASS = "type";
   static final String SWITCH = "object";
   static final String FILTER = "filter";
   static final String FILTERS = "filters";
   static final String OPERATOR = "operator";

   static String fld = "/Users/alhof/Repository/Test/";

   public static void main(String[] args) throws Exception
   {
      int read = 0;
      byte[] buf = new byte[8192];

      String file = "test2.json";

      FileInputStream in = new FileInputStream(Parser.fld+file);
      read = in.read(buf); in.close();
      String json = new String(buf,0,read);

      Parser.parse(json);
   }

   public static SQLObject parse(String request) throws Exception
   {
      SQLObject object = null;
      JSONObject json = new JSONObject(request);
      //System.out.println(json.toString(2));
      if (!json.has(SWITCH)) throw new Exception("Unknown request type");

      switch(json.getString(SWITCH))
      {
         case "Reterieve" : object = new Query(json); break;
      }

      if (json.has(FILTERS))
      {
         WhereClause whcl = new WhereClause(json.getJSONArray(FILTERS));
      }

      return(object);
   }
}
