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

import java.util.Arrays;
import java.util.HashSet;
import org.json.JSONObject;


public class Authenticator implements APIObject
{
   private final JSONObject definition;

   private static final HashSet<String> attrs =
      new HashSet<>(Arrays.asList("scope","token","method","username","password","secret"));


   public Authenticator(JSONObject definition)
   {
      this.definition = definition;
   }


   @Override
   public String path()
   {
      return("connect");
   }


   @Override
   public JSONObject toApi()
   {
      JSONObject custom = new JSONObject();
      JSONObject request = new JSONObject();

      String[] props = JSONObject.getNames(definition);

      for (int i = 0; i < props.length; i++)
      {
         String name = props[i].toLowerCase();

         if (attrs.contains(name)) request.put(name,definition.get(name));
         else custom.put(props[i],definition.get(props[i]));
      }

      if (!custom.isEmpty())
         request.put("custom",custom);

      return(request);
   }
}
