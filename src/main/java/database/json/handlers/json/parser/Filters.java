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

import java.lang.reflect.Constructor;
import java.util.HashMap;

import org.json.JSONObject;

import database.json.database.filters.Filter;


public class Filters
{
   private static HashMap<String,String> filters =
      new HashMap<String,String>()
      {{
         put("like","database.json.database.filters.Like");
         put("false","database.json.database.filters.False");
         put("equals","database.json.database.filters.Equals");
         put("greaterthan","database.json.database.filters.GreaterThan");
      }};


   @SuppressWarnings("unchecked")
   public static Filter get(String name, JSONObject definition) throws Exception
   {
      if (name == null) throw new Exception("Lookup null?");
      String clazz = Filters.filters.get(name.toLowerCase());
      if (clazz == null)  throw new Exception("Unknown filter '"+name+"'");

      Class<Filter> impl = (Class<Filter>) Class.forName(clazz);

      Constructor<Filter> def = null;
      Constructor<?>[] constrs = impl.getDeclaredConstructors();

      for (int i = 0; i < constrs.length; i++)
      {
         Class<?>[] pars = constrs[i].getParameterTypes();
         Constructor<Filter> constr = (Constructor<Filter>) constrs[i];

         if (pars.length == 0) def = constr;

         else if (pars.length == 1 && pars[0] == JSONObject.class)
            return(constr.newInstance(definition));
      }

      return(def.newInstance());
   }
}