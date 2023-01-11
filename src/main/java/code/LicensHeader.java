/*
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.

 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

package code;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class LicensHeader
{
  private String file = null;
  
  private static final String header =
  "Copyright © 2023 Alex Høffner\n\n" + 
  "Permission is hereby granted, free of charge, to any person obtaining a copy of this software\n" +
  "and associated documentation files (the “Software”), to deal in the Software without\n"+
  "restriction, including without limitation the rights to use, copy, modify, merge, publish,\n" +
  "distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the\n" +
  "Software is furnished to do so, subject to the following conditions:\n\n" + 
  "The above copyright notice and this permission notice shall be included in all copies or\n" +
  "substantial portions of the Software.\n\n" + 
  "THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING\n" +
  "BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND\n" +
  "NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,\n" +
  "DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING\n" +
  "FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.";


  @SuppressWarnings("unused")
  public static void main(String[] args) throws Exception
  {
    String root = "/Users/alex/Repository/DatabaseJS/projects/database.js/src";
    next(new File(root));
  }


  private static void next(File folder) throws Exception
  {
    File[] content = folder.listFiles();

    for(File file : content)
    {
      if (file.isDirectory()) next(file);
      else
      {
        LicensHeader lichead = new LicensHeader(file.getPath());
        String code = lichead.process();
        if (code != null) lichead.save(code);
      }
    }
  }


  public LicensHeader(String file)
  {
    if (file.endsWith(".ts") || file.endsWith("java"))
      this.file = file;
  }
  
  
  public String process() throws Exception
  {
    if (file == null)
      return(null);
    
    if (!file.endsWith("LicensHeader.java"))
      return(null);

    return(this.replace());
  }
  
  
  public String replace() throws Exception
  {
    File file = new File(this.file);
    byte[] buf = new byte[(int) file.length()];

    FileInputStream in = new FileInputStream(file);
    
    if (in.read(buf) != buf.length)
    {
      System.err.println("Could not read "+this.file);
      return(null);
    }
    
    String code = new String(buf).trim();
    
    if (!code.startsWith("/*"))
    {
      System.err.println("No header in "+this.file);
      return(null);      
    }
    
    int start = code.indexOf("*/") + 2;
    code = "/*\n"+LicensHeader.header + "\n*/\n" + code.substring(start);
    
    return(code);
  }


  public void save(String code) throws Exception
  {
    FileOutputStream out = new FileOutputStream(file);
    out.write(code.getBytes());
    out.close();
  }
}
