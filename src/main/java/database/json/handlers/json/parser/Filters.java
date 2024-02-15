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
import database.json.database.filters.Filter;


public class Filters
{
   private static HashMap<String,String> filters =
      new HashMap<String,String>();

   static
   {
      Filters.filters.put("equals","database.json.database.filters.Equals");
   }

   @SuppressWarnings("unchecked")
   public static Filter get(String name) throws Exception
   {
      if (name == null) throw new Exception("Lookup null?");
      String clazz = Filters.filters.get(name.toLowerCase());
      if (clazz == null)  throw new Exception("Unknown filter '"+name+"'");

      Class<Filter> impl = (Class<Filter>) Class.forName(clazz);
      return(impl.getDeclaredConstructor().newInstance());
   }
}