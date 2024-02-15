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

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.FileInputStream;
import database.rest.database.BindValue;
import database.rest.database.BindValueDef;


public class Parser
{
   static final public String CLASS = "type";
   static final public String SOURCE = "source";
   static final public String SWITCH = "function";

   static final public String FILTER = "filter";
   static final public String FILTERS = "filters";
   static final public String OPERATOR = "operator";

   static final public String ORDER = "order";
   static final public String COLUMN = "column";
   static final public String COLUMNS = "columns";
   static final public String BINDVALUE = "bindvalue";
   static final public String BINDVALUES = "bindvalues";

   static String fld = "/Users/alhof/Repository/Test/";

   public static void main(String[] args) throws Exception
   {
      int read = 0;
      byte[] buf = new byte[8192];

      String file = "test2.json";

      FileInputStream in = new FileInputStream(Parser.fld+file);
      read = in.read(buf); in.close();
      String json = new String(buf,0,read);

      new Source("countries");

      SQLObject parsed = Parser.parse(json);
      System.out.println(parsed.sql());
   }

   public static SQLObject parse(String request) throws Exception
   {
      SQLObject object = null;
      JSONObject json = new JSONObject(request);
      if (!json.has(SWITCH)) throw new Exception("Unknown request type");

      switch(json.getString(SWITCH).toLowerCase())
      {
         case "retrieve" : object = new Query(json); break;
      }

      return(object);
   }


   public static BindValue[] getBindValues(JSONObject definition)
   {
      JSONArray bindings = null;
      BindValue[] bindvalues = new BindValue[0];

      if (definition.has(Parser.BINDVALUES))
      {
         bindings = definition.getJSONArray(Parser.BINDVALUES);
         bindvalues = new BindValue[bindings.length()];

         for (int i = 0; i < bindvalues.length; i++)
         {
            String name = null;
            String type = null;
            Object value = null;
            boolean out = false;

            JSONObject bdef = bindings.getJSONObject(i);

            if (bdef.has("out")) out = bdef.getBoolean("out");
            if (bdef.has("name")) name = bdef.getString("name");
            if (bdef.has("type")) type = bdef.getString("type");
            if (bdef.has("value")) value = bdef.get("value");

            bindvalues[i] = new BindValue(new BindValueDef(name,type,out,value),out);
         }
      }

      return(bindvalues);
   }
}
