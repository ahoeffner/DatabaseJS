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

package database.json.handlers.json.parser;

import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import database.json.database.BindValue;


public class Map implements SQLObject
{
   private final Object custom;

   private final HashMap<String,String> mapping =
      new HashMap<String,String>();


   public Map(JSONObject definition) throws Exception
   {
      Object custom = null;

      if (definition.has(Parser.CUSTOM))
         custom = definition.get(Parser.CUSTOM);

      if (definition.has(Parser.MAPPING))
      {
         JSONArray map = definition.getJSONArray(Parser.MAPPING);

         for (int i = 0; i < map.length(); i++)
         {
            JSONObject def = map.getJSONObject(i);
            String[] keys = JSONObject.getNames(def);

            for (int m = 0; m < keys.length; m++)
               mapping.put(keys[m],def.getString(keys[m]));
         }
      }

      this.custom = custom;
   }


   @Override
   public String path() throws Exception
   {
      return("map");
   }


   @Override
   public JSONObject toApi() throws Exception
   {
      JSONObject json = new JSONObject();

      for (String key : mapping.keySet())
         json.put(key,mapping.get(key));

      return(json);
   }


   @Override
   public String sql() throws Exception
   {
      return(null);
   }


   @Override
   public boolean lock() throws Exception
   {
      return(false);
   }


   @Override
   public Object custom() throws Exception
   {
      return(custom);
   }


   @Override
   public String session() throws Exception
   {
      return(null);
   }


   @Override
   public boolean validate() throws Exception
   {
      return(mapping.size() > 0);
   }


   @Override
   public BindValue[] getAssertions() throws Exception
   {
      return(new BindValue[0]);
   }


   @Override
   public BindValue[] getBindValues() throws Exception
   {
      return(new BindValue[0]);
   }
}
