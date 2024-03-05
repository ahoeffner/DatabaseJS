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

import org.json.JSONArray;
import org.json.JSONObject;
import database.json.database.BindValue;
import database.json.database.BindValueDef;


public class Parser
{
   static final public String PATH = "path";
   static final public String CLASS = "type";
   static final public String SOURCE = "source";
   static final public String CUSTOM = "custom";
   static final public String SWITCH = "request";
   static final public String SESSION = "session";
   static final public String PAYLOAD = "payload";
   static final public String MAPPING = "mapping";

   static final public String MAP = "map";
   static final public String QUERY = "query";
   static final public String BATCH = "batch";
   static final public String SCRIPT = "script";
   static final public String INSERT = "insert";
   static final public String UPDATE = "update";
   static final public String DELETE = "delete";
   static final public String INVOKE = "invoke";
   static final public String EXECUTE = "execute";
   static final public String DESCRIBE = "describe";

   static final public String FILTER = "filter";
   static final public String COMPACT = "compact";
   static final public String FILTERS = "filters";
   static final public String OPERATOR = "operator";
   static final public String PRIMARYKEY = "primarykey";

   static final public String SQL = "sql";
   static final public String ROWS = "rows";
   static final public String SKIP = "skip";
   static final public String LOCK = "lock";
   static final public String STEPS = "steps";
   static final public String ORDER = "order";
   static final public String VALUE = "value";
   static final public String CURSOR = "cursor";
   static final public String COLUMN = "column";
   static final public String VALUES = "values";
   static final public String COLUMNS = "columns";
   static final public String RETURNING = "returning";
   static final public String BINDVALUE = "bindvalue";
   static final public String BINDVALUES = "bindvalues";
   static final public String PARAMETERS = "parameters";
   static final public String ASSERTIONS = "assertions";


   public static APIObject parse(String request) throws Exception
   {
      return(Parser.parse(new JSONObject(request)));
   }

   public static APIObject parse(JSONObject request) throws Exception
   {
      APIObject object = null;

      if (!request.has(SWITCH))
         throw new Exception("Permission denied, Unknown request");

      String type = ((String) request.remove(SWITCH)).toLowerCase();

      switch(type)
      {
         case Parser.MAP      : object = new Map(request);                 break;
         case Parser.QUERY    : object = new Query(request);               break;
         case Parser.BATCH    : object = new Batch(request);               break;
         case Parser.SCRIPT   : object = new Script(request);              break;
         case Parser.INSERT   : object = new Insert(request);              break;
         case Parser.UPDATE   : object = new Update(request);              break;
         case Parser.DELETE   : object = new Delete(request);              break;
         case Parser.CURSOR   : object = new Cursor(request);              break;
         case Parser.SESSION  : object = new Session(request);             break;
         case Parser.INVOKE   : object = new Function(request);            break;
         case Parser.DESCRIBE : object = Query.describe(request);          break;
         case Parser.EXECUTE  : object = new SQLStatement(request);        break;
      }

      if (object == null)
         throw new Exception("Permission denied, Unknown request '"+type+"'");

      return(object);
   }


   public static JSONObject toApi(SQLObject func) throws Exception
   {
      JSONObject binding = null;

      BindValue[] assertions = null;
      BindValue[] bindvalues = null;

      JSONArray bindings = new JSONArray();
      JSONObject request = new JSONObject();

      request.put(Parser.SQL,func.sql());
      request.put(Parser.SESSION,func.session());

      bindvalues = func.getBindValues();
      for (int i = 0; i < bindvalues.length; i++)
      {
         binding = new JSONObject();
         binding.put("name",bindvalues[i].getName());
         binding.put("type",bindvalues[i].getTypeName());
         binding.put("value",bindvalues[i].getValue());

         if (bindvalues[i].InOut() || bindvalues[i].OutOnly())
            binding.put("out",true);

         bindings.put(binding);
      }

      if (bindings.length() > 0)
         request.put(Parser.BINDVALUES,bindings);

      bindings = new JSONArray();
      assertions = func.getAssertions();

      for (int i = 0; i < assertions.length; i++)
      {
         binding = new JSONObject();
         binding.put("name",assertions[i].getName());
         binding.put("type",assertions[i].getTypeName());
         binding.put("value",assertions[i].getValue());
         bindings.put(binding);
      }

      if (bindings.length() > 0)
         request.put(Parser.ASSERTIONS,bindings);

      if (func.custom() != null)
         request.put(Parser.CUSTOM,func.custom());

      if (func.lock())
         request.put(Parser.LOCK,true);

      return(request);
   }


   public static BindValue[] getBindValues(JSONObject definition)
   {
      JSONArray bindings = null;
      String keyword = BINDVALUES;
      BindValue[] bindvalues = new BindValue[0];
      if (!definition.has(BINDVALUES)) keyword = PARAMETERS;

      if (definition.has(keyword))
      {
         bindings = definition.getJSONArray(keyword);
         bindvalues = new BindValue[bindings.length()];

         for (int i = 0; i < bindvalues.length; i++)
         {
            String name = null;
            String type = null;
            Object value = null;
            String ptype = "in";

            JSONObject bdef = bindings.getJSONObject(i);

            if (bdef.has("value")) value = bdef.get("value");
            if (bdef.has("name")) name = bdef.getString("name");
            if (bdef.has("type")) type = bdef.getString("type");
            if (bdef.has("inout")) ptype = bdef.getString("inout");

            boolean out = ptype.equals("out");
            boolean inout = !ptype.equals("in");

            bindvalues[i] = new BindValue(new BindValueDef(name,type,out,value),inout);
         }
      }

      return(bindvalues);
   }


   public static BindValue[] getAssertions(JSONObject definition)
   {
      JSONArray asserts = null;
      BindValue[] bindvalues = new BindValue[0];

      if (definition.has(ASSERTIONS))
      {
         asserts = definition.getJSONArray(ASSERTIONS);
         bindvalues = new BindValue[asserts.length()];

         for (int i = 0; i < bindvalues.length; i++)
         {
            String type = null;
            Object value = null;
            String column = null;

            JSONObject adef = asserts.getJSONObject(i);

            if (adef.has("value")) value = adef.get("value");
            if (adef.has("type")) type = adef.getString("type");
            if (adef.has("column")) column = adef.getString("column");

            bindvalues[i] = new BindValue(new BindValueDef(column,type,false,value),false);
         }
      }

      return(bindvalues);
   }
}