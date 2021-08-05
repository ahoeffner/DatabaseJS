package instances;

import java.util.Map;
import java.util.Hashtable;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


public class InstanceData
{
  private byte[][] sections = null;
  private Hashtable<String,FileEntry> files = null;
  private Hashtable<Integer,Instance> instances = null;

  private static final int HEADER     = 0;
  private static final int FILES      = 1;
  private static final int INSTANCES  = 2;


  public InstanceData(byte[] data) throws Exception
  {
    this.sections = new byte[3][];

    if (data == null)
    {
      files = new Hashtable<String,FileEntry>();
      instances = new Hashtable<Integer,Instance>();
    }
    else
    {
      int[] offset = (int[]) this.deserialize(data);

      sections[HEADER   ] = new byte[offset[FILES]-offset[HEADER]];
      sections[FILES    ] = new byte[offset[INSTANCES]-offset[FILES]];
      sections[INSTANCES] = new byte[data.length - offset[INSTANCES]];

      System.arraycopy(data,offset[HEADER   ],sections[HEADER   ],0,sections[HEADER   ].length);
      System.arraycopy(data,offset[FILES    ],sections[FILES    ],0,sections[FILES    ].length);
      System.arraycopy(data,offset[INSTANCES],sections[INSTANCES],0,sections[INSTANCES].length);
    }
  }


  @SuppressWarnings("unchecked")
  public Hashtable<Integer,Instance> getInstances(boolean mod) throws Exception
  {
    if (instances == null) instances = (Hashtable<Integer,Instance>) this.deserialize(sections[INSTANCES]);
    if (mod) sections[INSTANCES] = null;
    return(instances);
  }


  @SuppressWarnings("unchecked")
  public Hashtable<String,FileEntry> getFiles(boolean mod) throws Exception
  {
    if (files == null) files = (Hashtable<String,FileEntry>) this.deserialize(sections[FILES]);
    if (mod) sections[FILES] = null;
    return(files);
  }


  public void setInstance(int inst)
  {
    try {this.getInstances(true);}
    catch (Exception e) {;}

    Instance entry = new Instance(inst);
    this.instances.put(inst,entry);
  }


  public void removeInstance(int inst)
  {
    try {this.getInstances(true);}
    catch (Exception e) {;}

    this.instances.remove(inst);
  }


  public void setFile(String file, long mod)
  {
    try {this.getFiles(true);}
    catch (Exception e) {;}

    FileEntry entry = new FileEntry(mod);
    this.files.put(file,entry);
  }


  public byte[] serialize() throws Exception
  {
    int[] offset = new int[3];

    if (sections[HEADER] == null)     sections[HEADER]    = this.serialize(offset    );
    if (sections[FILES ] == null)     sections[FILES]     = this.serialize(files     );
    if (sections[INSTANCES] == null)  sections[INSTANCES] = this.serialize(instances );

    offset[HEADER] = 0;
    offset[FILES] = offset[HEADER] + sections[HEADER].length;
    offset[INSTANCES] = offset[FILES] + sections[FILES].length;

    sections[0] = this.serialize(offset);

    int size = 0;
    for(byte[] section : sections) size += section.length;

    byte[] data = new byte[size];

    System.arraycopy(sections[HEADER],0,data,offset[HEADER],sections[0].length);
    System.arraycopy(sections[FILES],0,data,offset[FILES],sections[FILES].length);
    System.arraycopy(sections[INSTANCES],0,data,offset[INSTANCES],sections[INSTANCES].length);

    return(data);
  }


  @Override
  public String toString()
  {
    String str = "";

    if (this.files == null)
    {
      try {this.getFiles(false);}
      catch (Exception e) {;}
    }

    if (this.instances == null)
    {
      try {this.getInstances(false);}
      catch (Exception e) {;}
    }

    for(Map.Entry<Integer,Instance> entry : this.instances.entrySet())
      str += entry.getKey() + " : " + entry.getValue() + "\n";

    for(Map.Entry<String,FileEntry> entry : this.files.entrySet())
      str += entry.getKey() + " : " + entry.getValue() + "\n";

    return(str);
  }


  private byte[] serialize(Object obj) throws Exception
  {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    ObjectOutputStream oout = new ObjectOutputStream(bout);
    oout.writeObject(obj);
    return(bout.toByteArray());
  }


  private Object deserialize(byte[] data) throws Exception
  {
    ByteArrayInputStream bin = new ByteArrayInputStream(data);
    ObjectInputStream oin = new ObjectInputStream(bin);
    Object obj = oin.readObject();
    return(obj);
  }


  public static class Instance implements Serializable
  {
    @SuppressWarnings("compatibility:3930941963720525578")
    private static final long serialVersionUID = 5741949964475085825L;

    private int inst = 0;


    public Instance(int inst)
    {
      this.inst = inst;
    }


    @Override
    public String toString()
    {
      return(""+inst);
    }
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
