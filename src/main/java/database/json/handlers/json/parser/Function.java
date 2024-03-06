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
import java.util.ArrayList;
import org.json.JSONObject;
import database.json.database.BindValue;
import database.json.database.BindValueDef;


public class Function implements SQLObject
{
   private final String source;
   private final String retval;
   private final JSONObject payload;
   private final BindValue[] bindvalues;


   public Function(JSONObject definition) throws Exception
   {
      String source = null;
      String retval = null;

      if (definition.has(Parser.SOURCE))
         source = definition.getString(Parser.SOURCE);

      ArrayList<BindValue> bindvalues = new ArrayList<BindValue>();
      bindvalues.addAll(Arrays.asList(Parser.getBindValues(definition)));

      if (definition.has(Parser.RETURNING))
      {
         JSONObject ret = definition.getJSONObject(Parser.RETURNING);

         retval = ret.getString("name");
         String rtype = ret.getString("type");

         bindvalues.add(0,new BindValue(new BindValueDef(retval,rtype,true),true));
      }

      this.source = source;
      this.retval = retval;
      this.payload = definition;
      this.bindvalues = bindvalues.toArray(new BindValue[0]);
   }


   @Override
   public String path() throws Exception
   {
      return(Parser.INVOKE);
   }


   @Override
   public JSONObject payload()
   {
      return(payload);
   }


   @Override
   public boolean lock() throws Exception
   {
      return(false);
   }


   @Override
   public boolean validate() throws Exception
   {
      Source source = Source.getSource(this.source);
      if (source == null) return(false);
      return(true);
   }


   @Override
   public BindValue[] getAssertions() throws Exception
   {
      return(new BindValue[0]);
   }


   @Override
   public BindValue[] getBindValues() throws Exception
   {
      return(bindvalues);
   }


   @Override
   public String sql() throws Exception
   {
      Source source = Source.getSource(this.source);
      if (source == null) throw new Exception("Permission denied, source: '"+this.source+"'");

      int off = 0;
      String sql = "";

      if (retval != null)
      {
         off++;
         sql += retval+" = ";
      }

      sql += source.function + "(";

      for (int i = off; i < bindvalues.length; i++)
      {
         if (i > off) sql += ",";
         String bind = bindvalues[i].InOut() ? "&" : ":";
         sql += bind + bindvalues[i].getName();
      }

      sql += ")";
      return(sql);
   }


   @Override
   public JSONObject toApi() throws Exception
   {
      JSONObject parsed = Parser.toApi(this);
      return(parsed);
   }
}
