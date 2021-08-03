package config;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;


public class RequestMap
{
  private ArrayList<Entry> entries = new ArrayList<Entry>();
    
  
  public void add(String prefix, String methods)
  {
    this.entries.add(new Entry(prefix,methods));
    Collections.sort(this.entries);
  }
  
  
  public void print()
  {
    for(Entry e : entries) System.out.println(e);
  }
  
  
  private static class Entry implements Comparable<Entry>
  {
    public final String prefix;
    public final HashSet<String> methods = new HashSet<String>();
    
    Entry(String prefix, String methods)
    {
      this.prefix = prefix;
      String meth[] = methods.split(",");
      for(String m : meth) if (m.length() > 0) this.methods.add(m.toUpperCase());
    }
    
    
    @Override
    public String toString()
    {
      return(prefix+" "+methods);
    }


    @Override
    public int compareTo(Entry another)
    {
      return(this.prefix.length() - another.prefix.length());
    }
  }
}
