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
import java.util.HashMap;
import java.util.HashSet;
import org.json.JSONObject;


public class Session implements APIObject
{
   private String type = null;
   private String session = null;
   private final JSONObject payload;
   private boolean keepalive = false;

   private HashMap<String,Object> attrs =
      new HashMap<String,Object>();

   private static final HashSet<String> tokens =
      new HashSet<String>(Arrays.asList
      (
         "scope",
         "token",
         "method",
         "secret",
         "username",
         "password",
         "clientinfo",
         "custom"
      ));


   public Session(JSONObject definition)
   {
      type = "ping";
      this.payload = definition;

      if (definition.has("type"))
         type = definition.getString("type").toLowerCase();

      if (definition.has(Parser.SESSION))
         session = definition.getString(Parser.SESSION);

      if (definition.has("keepalive"))
         keepalive = definition.getBoolean("keepalive");

      String[] props = JSONObject.getNames(definition);

      for (int i = 0; i < props.length; i++)
      {
         String name = props[i].toLowerCase();
         if (tokens.contains(name)) attrs.put(name,definition.get(name));
      }
   }


   @Override
   public String path()
   {
      return(type);
   }


   @Override
   public JSONObject payload()
   {
      return(payload);
   }


   @Override
   public JSONObject toApi() throws Exception
   {
      JSONObject request = new JSONObject();

      request.put("session",session);
      if (keepalive) request.put("keepalive",true);

      for (String attr : attrs.keySet())
         request.put(attr,attrs.get(attr));

      return(request);
   }
}
