package config;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Handler;


public class Handlers
{
  private ArrayList<Entry> entries = new ArrayList<Entry>();
    
  
  public void add(String prefix, String methods, String clazz) throws Exception
  {
    this.entries.add(new Entry(prefix,methods,clazz));
    Collections.sort(this.entries);
  }
  
  
  private static class Entry implements Comparable<Entry>
  {
    public final String prefix;
    public final handlers.Handler handler;
    public final HashSet<String> methods = new HashSet<String>();
    
    Entry(String prefix, String methods, String clazz) throws Exception
    {
      this.prefix = prefix;
      String meth[] = methods.split(",");
      for(String m : meth) if (m.length() > 0) this.methods.add(m.toUpperCase());
      this.handler = (handlers.Handler) Class.forName(clazz).getDeclaredConstructor().newInstance();
    }
    
    
    @Override
    public String toString()
    {
      return(prefix+" "+methods+" "+handler.getClass().getName());
    }


    @Override
    public int compareTo(Entry another)
    {
      return(this.prefix.length() - another.prefix.length());
    }
  }
}
