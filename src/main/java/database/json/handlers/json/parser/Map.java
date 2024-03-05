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
import java.util.ArrayList;
import database.json.database.BindValue;


public class Map implements SQLObject
{
   private final ArrayList<String[]> mapping =
      new ArrayList<String[]>();


   public Map(JSONObject definition) throws Exception
   {
      if (definition.has(Parser.MAPPING))
      {

      }
   }


   @Override
   public String path() throws Exception {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("Unimplemented method 'path'");
   }


   @Override
   public JSONObject toApi() throws Exception {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("Unimplemented method 'toApi'");
   }


   @Override
   public String sql() throws Exception {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("Unimplemented method 'sql'");
   }


   @Override
   public boolean lock() throws Exception {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("Unimplemented method 'lock'");
   }


   @Override
   public Object custom() throws Exception {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("Unimplemented method 'custom'");
   }


   @Override
   public String session() throws Exception {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("Unimplemented method 'session'");
   }


   @Override
   public boolean validate() throws Exception {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("Unimplemented method 'validate'");
   }


   @Override
   public BindValue[] getAssertions() throws Exception {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("Unimplemented method 'getAssertions'");
   }


   @Override
   public BindValue[] getBindValues() throws Exception {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException("Unimplemented method 'getBindValues'");
   }
}
