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
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.io.IOException;
import java.util.Comparator;
import java.util.Collections;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;


public class Beautifier
{
  private final String file;
  private static int lines = 0;
  private static int blanks = 0;


  public static void main(String[] args) throws Exception
  {
    Beautifier beautifier = new Beautifier("/Users/alex/Repository/DatabaseJS/projects/database.js/src/main/java/code/Beautifier.java");
    String code = beautifier.process();
    if (code != null) beautifier.save(code);
    
    System.out.println("lines: "+lines+" blanks: "+blanks);
  }


  public Beautifier(String file)
  {
    this.file = file;
  }


  public void save(String code) throws Exception
  {
    FileOutputStream out = new FileOutputStream(file);
    out.write(code.getBytes());
    out.close();
  }


  public String process() throws Exception
  {
    String line = null;
    File f = new File(file);

    TrimmedOutputStream bout = new TrimmedOutputStream();
    BufferedReader in = new BufferedReader(new FileReader(f));

    PrintStream out = new PrintStream(bout);

    // Skip blanks before open-source header

    while(true)
    {
      line = in.readLine();
      if (line.trim().length() > 0) break;
    }

    if (!line.trim().startsWith("/*"))
    {
      System.out.println("No open-source header "+file);
      return(null);
    }

    out.println(line);

    while(true)
    {
      line = in.readLine(); out.println(line);
      if (line.trim().endsWith("*/")) break;
    }

    out.println();

    // Skip blanks before package

    while(true)
    {
      line = in.readLine();
      if (line.trim().length() > 0) break;
    }

    // package + blank
    out.println(line);
    out.println();

    // Skip blanks before import

    while(true)
    {
      line = in.readLine();
      if (line.trim().length() > 0) break;
    }

    if (line.startsWith("import"))
    {
      ArrayList<String> imps = new ArrayList<String>();
      imps.add(line);

      while(true)
      {
        line = in.readLine().trim();

        if (line.length() > 0 && !line.startsWith("import"))
          break;

        imps.add(line);
      }

      while(imps.get(imps.size()-1).length() == 0)
        imps.remove(imps.size()-1);

      imps = sortimps(imps);

      for(String imp : imps)
      {
        out.println(imp);
        if (imp.length() == 0)
          System.out.println("Blank lines in import section "+file);
      }
    }

    out.println();
    out.println();
    out.println(line);

    while(true)
    {
      line = in.readLine();
      if (line == null) break;
      out.println(line);
    }

    in.close();
    return(new String(bout.toByteArray()));
  }


  private String trimx(String str)
  {
    if (str == null) return(null);
    byte[] bytes = str.getBytes();

    int len = bytes.length;
    for (int i = len-1; i >= 0; i--)
    {
      if (bytes[i] == ' ') len--;
      else break;
    }

    if (len < bytes.length)
      str = new String(bytes,0,len);

    return(str);
  }


  private ArrayList<String> sortimps(ArrayList<String> imps)
  {
    LengthSorter sorter = new LengthSorter();
    ArrayList<String> tmp = new ArrayList<String>();
    ArrayList<String> simps = new ArrayList<String>();

    for (int i = 0; i < imps.size(); i++)
    {
      String imp = imps.get(i);

      if (imp.length() > 0)
      {
        tmp.add(imp);
      }
      else
      {
        Collections.sort(tmp,sorter);
        simps.addAll(tmp);
        simps.add(imp);
        tmp.clear();
      }
    }

    Collections.sort(tmp,sorter);
    simps.addAll(tmp);
    tmp.clear();

    return(simps);
  }


  class LengthSorter implements Comparator<String>
  {
    @Override
    public int compare(String s1, String s2)
    {
      return(s1.length()-s2.length());
    }
  }
  
  
  private static class TrimmedOutputStream extends ByteArrayOutputStream
  {
    @Override
    public synchronized void write(byte[] buf, int off, int len)
    {
      for (int i = len-1; i >= 0; i--)
      {
        if (buf[i+off] == ' ') len--;
        else break;
      }
      
      lines++;
      if (len == 1 && buf[0] == '\n') blanks++;

      super.write(buf, off, len);
    }
  }
}