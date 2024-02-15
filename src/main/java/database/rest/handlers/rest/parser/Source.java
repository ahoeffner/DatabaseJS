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

package database.rest.handlers.rest.parser;

import java.util.HashMap;


public class Source
{
   private final static HashMap<String,Source> sources =
      new HashMap<String,Source>();

   public static Source getSource(String id)
   {
      return(Source.sources.get(id.toLowerCase()));
   }


   public final String id;
   public final String sql;
   public final String table;
   public final SourceType type;


   public Source(String table)
   {
      this.sql = null;
      this.table = table;
      this.type = SourceType.table;
      this.id = table.toLowerCase();

      Source.sources.put(id,this);
   }


   public Source(String id, String sql)
   {
      id = id.toLowerCase();
      SourceType type = SourceType.table;

      if (sql.length() > 6 && sql.charAt(6) == ' ')
      {
         String start = sql.substring(0,6).toLowerCase();

         switch(start)
         {
            case "select": type = SourceType.query;
         }
      }

      this.id = id;
      this.sql = sql;
      this.type = type;
      this.table = null;

      Source.sources.put(id,this);
   }
}
