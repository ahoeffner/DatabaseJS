package instances;

import java.util.Map;
import instances.Stats;
import java.util.Hashtable;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


public class InstanceData
{
  private byte[][] sections = null;

  private Cluster cluster = null;
  private Hashtable<String,FileEntry> files = null;
  private Hashtable<Integer,Instance> instances = null;

  private static final int HEADER     = 0;
  private static final int CLUSTER    = 1;
  private static final int FILES      = 2;
  private static final int INSTANCES  = 3;


  public InstanceData(byte[] data) throws Exception
  {
    this.sections = new byte[4][];

    if (data == null)
    {
      cluster = new Cluster();
      files = new Hashtable<String,FileEntry>();
      instances = new Hashtable<Integer,Instance>();
    }
    else
    {
      int[] offset = (int[]) this.deserialize(data);

      sections[ HEADER    ] = new byte[offset[ CLUSTER    ]-offset[ HEADER    ]];
      sections[ CLUSTER   ] = new byte[offset[ FILES      ]-offset[ CLUSTER   ]];
      sections[ FILES     ] = new byte[offset[ INSTANCES  ]-offset[ FILES     ]];
      sections[ INSTANCES ] = new byte[    data.length     -offset[ INSTANCES ]];

      System.arraycopy(data,offset[ HEADER    ],sections[ HEADER    ],0,sections[ HEADER    ].length);
      System.arraycopy(data,offset[ CLUSTER   ],sections[ CLUSTER   ],0,sections[ CLUSTER   ].length);
      System.arraycopy(data,offset[ FILES     ],sections[ FILES     ],0,sections[ FILES     ].length);
      System.arraycopy(data,offset[ INSTANCES ],sections[ INSTANCES ],0,sections[ INSTANCES ].length);

      cluster = (Cluster) this.deserialize(sections[CLUSTER]);
    }
  }


  public String version()
  {
    return(cluster.version());
  }


  public int servers()
  {
    return(cluster.servers());
  }


  public int manager()
  {
    return(cluster.manager());
  }


  public void setManager(int inst)
  {
    cluster.manager(inst);
    this.sections[CLUSTER] = null;
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


  public boolean cluster(int inst, int servers, String version) throws Exception
  {
    Hashtable<Integer,Instance> instances = this.getInstances(false);

    boolean chmgr = true;
    for(int running : instances.keySet())
    {
      if (running == cluster.manager())
      {
        chmgr = false;
        break;
      }
    }

    if (chmgr)
      cluster.manager(inst);

    boolean chsrvs = false;
    if (cluster.servers() < 0)
    {
      chsrvs = true;
      cluster.servers(servers);
    }

    boolean chvers = false;
    if (cluster.version() == null)
    {
      chvers = true;
      cluster.version(version);
    }

    if (chmgr || chsrvs || chvers)
      this.sections[CLUSTER] = null;

    return(chmgr);
  }


  public void setInstance(long pid, long time, int inst, int port, int ssl, int admin, Stats stats)
  {
    try {this.getInstances(true);}
    catch (Exception e) {;}

    Instance entry = new Instance(pid,time,port,ssl,admin,stats);
    this.instances.put(inst,entry);
  }


  public void removeInstance(int inst)
  {
    try {this.getInstances(true);}
    catch (Exception e) {;}

    if (inst == cluster.manager())
    {
      cluster.manager(-1);
      this.sections[CLUSTER] = null;
    }

    this.instances.remove(inst);
    
    if (this.instances.size() == 0)
    {
      cluster.servers(-1);
      cluster.version(null);
      this.sections[CLUSTER] = null;
    }
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
    int[] offset = new int[4];

    if (sections[ HEADER     ] == null) sections[ HEADER    ] = this.serialize(offset    );
    if (sections[ CLUSTER    ] == null) sections[ CLUSTER   ] = this.serialize(cluster    );
    if (sections[ FILES      ] == null) sections[ FILES     ] = this.serialize(files     );
    if (sections[ INSTANCES  ] == null) sections[ INSTANCES ] = this.serialize(instances );

    offset[ HEADER    ] = 0;
    offset[ CLUSTER   ] = offset[ HEADER   ] + sections[ HEADER   ].length;
    offset[ FILES     ] = offset[ CLUSTER  ] + sections[ CLUSTER  ].length;
    offset[ INSTANCES ] = offset[ FILES    ] + sections[ FILES    ].length;

    sections[HEADER] = this.serialize(offset);

    int size = 0;
    for(byte[] section : sections) size += section.length;

    byte[] data = new byte[size];

    System.arraycopy(sections[ HEADER    ],0,data,offset[ HEADER    ],sections[ HEADER    ].length);
    System.arraycopy(sections[ CLUSTER   ],0,data,offset[ CLUSTER   ],sections[ CLUSTER   ].length);
    System.arraycopy(sections[ FILES     ],0,data,offset[ FILES     ],sections[ FILES     ].length);
    System.arraycopy(sections[ INSTANCES ],0,data,offset[ INSTANCES ],sections[ INSTANCES ].length);

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


  private static class Cluster implements Serializable
  {
    @SuppressWarnings("compatibility:-1834868406567923546")
    private static final long serialVersionUID = 5741949964475085825L;

    private int servers = -1;
    private int manager = -1;
    private String version = null;


    public int manager()
    {
      return(manager);
    }


    public int servers()
    {
      return(servers);
    }


    public String version()
    {
      return(version);
    }


    public void manager(int manager)
    {
      this.manager = manager;
    }


    public void servers(int servers)
    {
      this.servers = servers;
    }


    public void version(String version)
    {
      this.version = version;
    }
  }


  public static class Instance implements Serializable
  {
    @SuppressWarnings("compatibility:5955619226857843807")
    private static final long serialVersionUID = 5741949964475085825L;

    public final long pid;
    public final long time;

    public final int ssl;
    public final int port;
    public final int admin;

    public final Stats stats;


    public Instance(long pid, long time, int port, int ssl, int admin, Stats stats)
    {
      this.pid = pid;
      this.ssl = ssl;
      this.port = port;
      this.time = time;
      this.admin = admin;
      this.stats = stats;
    }


    @Override
    public String toString()
    {
      return(""+pid);
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
