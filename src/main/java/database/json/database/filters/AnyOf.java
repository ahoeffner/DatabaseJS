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

package database.json.database.filters;

import org.json.JSONObject;
import database.json.database.BindValue;
import database.json.handlers.json.parser.Parser;


public class AnyOf implements Filter
{
   protected String column = null;
   protected BindValue[] bindvalues = null;

   @Override
   public String sql()
   {
      String list = "";

      for (int i = 0; i < bindvalues.length; i++)
      {
         if (i > 0) list += ",";
         list += ":"+bindvalues[i].getName();
      }

      return(column+" in ("+list+")");
   }

   @Override
   public BindValue[] getBindValues()
   {
      return(bindvalues);
   }

   @Override
   public void parse(JSONObject definition) throws Exception
   {
      if (definition.has(Parser.COLUMN))
         column = definition.getString(Parser.COLUMN);

      this.bindvalues = Parser.getBindValues(definition);
   }
}