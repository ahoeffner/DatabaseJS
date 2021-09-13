package control;

import java.io.File;
import java.util.HashMap;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.io.ByteArrayOutputStream;

/**
 * 
 * Dynamic ClassLoader that can used directly from within standard code.
 * 
 * Use the entrypoint() to get an instance of the entrypoint with the new
 * ClassLoader, and thereby the extra libraries.
 * 
 * Any class that uses the dynamically loaded libraries must be added to
 * the loader.
 * 
 */
public class Loader extends ClassLoader
{
  private final String entrypoint;
  private final String sep = File.separator;  
  
  private final HashMap<String,Class<?>> classes =
    new HashMap<String,Class<?>>();
  
  
  public Loader(Class entrypoint) throws Exception
  {
    this.entrypoint = entrypoint.getName().replace('.','/');
    load(entrypoint);
  }


  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException
  {
    Class<?> clazz = classes.get(name);
    if (clazz != null) return(clazz);    
    return(super.findClass(name));
  }
  
  
  public Class<?> entrypoint() throws Exception
  {
    return(findClass(entrypoint));
  }
  
  
  public void add(Class clazz) throws Exception
  {
    load(clazz);
  }
  
  
  public void load(String dir) throws Exception
  {
    File jsonlibs = new File(dir);
    
    ArrayList<Definition> failed = 
      new ArrayList<Definition>();
    
    for(String file : jsonlibs.list())
    {
      File test = new File(dir+sep+file);
      
      if (test.isDirectory()) 
        continue;
      
      JarFile jarfile = new JarFile(dir+sep+file);
      Enumeration<JarEntry> flist = jarfile.entries();
      
      while (flist.hasMoreElements())
      {
        JarEntry entry = flist.nextElement();
        
        if (entry.getName().endsWith(".class"))
        {
          String name = entry.getName();
          name = name.substring(0,name.length()-6);
          
          String qname = name.replace('/','.');
          
          InputStream in = jarfile.getInputStream(entry);
          byte[] bcode = new byte[(int) entry.getSize()];
          
          in.read(bcode);
          in.close();

          Definition cdef = new Definition(name,qname,bcode);

          Class<?> clazz = trydefine(cdef);
          
          if (clazz == null) failed.add(cdef);
          else               classes.put(name,clazz);            
        }
      }
      
      for (int i = 0; i < 16 && failed.size() > 0; i++)
      {
        for (int j = 0; j < failed.size(); j++)
        {
          Definition cdef = failed.get(j);
          Class<?> clazz = trydefine(cdef);
          
          if (clazz != null)
          {
            failed.remove(j--);
            classes.put(cdef.name,clazz);
          }
        }
      }
    }
    
    if (failed.size() > 0)
      throw new Exception("Unable to load jars from "+dir);
  }
  
  
  private void load(Class local) throws Exception
  {
    String name = local.getName().replace('.','/');
    
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    InputStream in = local.getResourceAsStream("/"+name+".class");
    
    int read = 0;
    byte[] buf = new byte[4096];
    
    while(read >= 0)
    {
      if (read > 0) out.write(buf,0,read);
      read = in.read(buf);
    }

    in.close();        
    byte[] bcode = out.toByteArray();
    
    Definition cdef = new Definition(name,local.getName(),bcode);
    
    Class<?> clazz = trydefine(cdef);
    
    if (clazz == null)
      throw new Exception("Unable to add "+local.getName()+" definition to Loader");
      
    classes.put(name,clazz);            
  }
  
  
  private Class<?> trydefine(Definition cdef)
  {
    try
    {
      Class<?> clazz = this.defineClass(cdef.qname,cdef.bcode,0,cdef.bcode.length);;
      return(clazz);
    }
    catch (Throwable e) 
    {return(null);}
  }
  
  
  private static class Definition
  {
    private final String name;
    private final String qname;
    private final byte[] bcode;
    
    Definition(String name, String qname, byte[] bcode)
    {
      this.name = name;
      this.qname = qname;
      this.bcode = bcode;
    }
  }  
}
