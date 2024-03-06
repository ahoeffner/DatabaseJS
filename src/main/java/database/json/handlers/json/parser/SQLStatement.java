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

import org.json.JSONObject;
import database.json.database.BindValue;


public class SQLStatement implements SQLObject
{
   private final String source;
   private final Object custom;
   private final String session;
   private final JSONObject payload;
   private final BindValue[] bindvalues;


   public SQLStatement(JSONObject definition) throws Exception
   {
      String source = null;
      Object custom = null;
      String session = null;

      if (definition.has(Parser.CUSTOM))
         custom = definition.get(Parser.CUSTOM);

      if (definition.has(Parser.SOURCE))
         source = definition.getString(Parser.SOURCE);

      if (definition.has(Parser.SESSION))
         session = definition.getString(Parser.SESSION);

      this.source = source;
      this.custom = custom;
      this.session = session;
      this.payload = definition;
      this.bindvalues = Parser.getBindValues(definition);
   }


   @Override
   public String path() throws Exception
   {
      return("execute");
   }


   @Override
   public JSONObject payload()
   {
      return(payload);
   }


   @Override
   public JSONObject toApi() throws Exception
   {
      return(Parser.toApi(this));
   }


   @Override
   public String sql() throws Exception
   {
      Source source = Source.getSource(this.source);
      if (source == null) throw new Exception("Permission denied, source: '"+this.source+"'");
      return(source.stmt);
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
      return(session);
   }


   @Override
   public boolean validate()
   {
      return(true);
   }


   @Override
   public BindValue[] getAssertions()
   {
      return(new BindValue[0]);
   }

   
   @Override
   public BindValue[] getBindValues() throws Exception
   {
      return(bindvalues);
   }
}
