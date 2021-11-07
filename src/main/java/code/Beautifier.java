package code;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;

import java.util.ArrayList;

public class Beautifier
{
  private final String file;
  
  
  public static void main(String[] args) throws Exception
  {
    Beautifier beautifier = new Beautifier("/Users/alex/Repository/DatabaseJS/projects/database.js/src/main/java/code/Beautifier.java");
    String code = beautifier.process();
    
    System.out.println(code);
  }
  
  
  public Beautifier(String file)
  {
    this.file = file;
  }
  
  
  public String process() throws Exception
  {
    String line = null;
    File f = new File(file);
    
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    BufferedReader in = new BufferedReader(new FileReader(f));
    
    PrintStream out = new PrintStream(bout);


    // Skip blanks before import    
    
    boolean skip = true;
    
    while(skip) 
    {
      line = trim(in.readLine());
      if (line.length() > 0) skip = false;
    }
    
    // package + blank
    out.println(trim(line));
    out.println();

    skip = true;
    
    while(skip) 
    {
      line = trim(in.readLine());
      if (line.length() > 0) skip = false;
    }
    
    if (line.startsWith("import"))
    {
      ArrayList<String> imps = new ArrayList<String>();
      imps.add(line);

      while(true) 
      {
        line = trim(in.readLine());
        
        if (line.length() > 0 && !line.startsWith("import"))
          break;
        
        imps.add(line);
      }
      
      if (imps.get(imps.size()-1).length() == 0)
        imps.remove(imps.size()-1);
      
      for(String i : imps)
      {
        System.out.println("<"+i+">");
        if (i.length() == 0)
          System.out.println(file+" has empty lines in import");
      }

      for(String i : imps)
        out.println(i);
    }
    
    return(new String(bout.toByteArray()));
  }
  
  
  private String trim(String str)
  {
    byte[] bytes = str.getBytes();

    int len = bytes.length;    
    for (int i = len-1; i >= 0; i--)
    {
      if (bytes[i] == ' ') len--;
      else break;
    }
        
    if (len < bytes.length)
    {
      str = new String(bytes,0,len);
      System.out.println("<"+str+">");
    }
    
    return(str);
  }
  
  
  private ArrayList<String> sortimps(ArrayList<String> imps)
  {
    ArrayList<String> tmp = new ArrayList<String>();
    ArrayList<String> simps = new ArrayList<String>();
    
    int pos = 0;
    
    while(pos < imps.size())
    {
      for (int i = 0; i < imps.size(); i++)
      {
        ;
      }
    }
    
    return(simps);
  }
}
