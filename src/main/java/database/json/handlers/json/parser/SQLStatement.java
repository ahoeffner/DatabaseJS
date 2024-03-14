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
   private final int rows;
   private final Source source;
   private final JSONObject payload;
   private final BindValue[] bindvalues;


   public SQLStatement(JSONObject definition) throws Exception
   {
      int rows = 0;
      String source = null;

      if (definition.has(Parser.ROWS))
         rows = definition.getInt(Parser.ROWS);

      if (definition.has(Parser.SOURCE))
         source = definition.getString(Parser.SOURCE);

      this.rows = rows;
      this.payload = definition;
      this.source = Source.getSource(source);
      this.bindvalues = Parser.getBindValues(definition);

      if (this.source == null)
         throw new Exception("Permission denied, source: '"+this.source+"'");
   }


   public boolean writes()
   {
      return(source.writes);
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
      JSONObject json = Parser.toApi(this);

      json.put("describe",true);
      if (rows > 0) json.put("rows",rows);

      return(json);
   }


   @Override
   public String sql() throws Exception
   {
      return(source.stmt);
   }


   @Override
   public boolean lock() throws Exception
   {
      return(false);
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
