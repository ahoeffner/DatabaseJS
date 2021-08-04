package instances;

import java.util.Hashtable;
import java.io.Serializable;

import java.util.Map;


public class InstanceData implements Serializable
{
  @SuppressWarnings("compatibility:1509762502832570498")
  private static final long serialVersionUID = -5931919197191045608L;

  public final Hashtable<String,FileEntry> files = new Hashtable<String,FileEntry>();
  
  
  public void add(String file, long mod)
  {
    FileEntry entry = new FileEntry(mod);
    this.files.put(file,entry);
  }
  
  
  @Override
  public String toString()
  {
    String str = "";
    
    for(Map.Entry<String,FileEntry> entry : this.files.entrySet())
      str += entry.getKey() + " : " + entry.getValue() + "\n";
    
    return(str);
  }
  
  
  public static class FileEntry implements Serializable
  {
    @SuppressWarnings("compatibility:-3023374735791094862")
    private static final long serialVersionUID = 5741949964475085825L;
    
    private long mod = 0;
    
    
    public FileEntry(long mod)
    {
      this.mod = mod;
    }
    
    
    @Override
    public String toString()
    {
      return(""+mod);
    }
  }
}
