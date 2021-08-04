package instances;

import java.io.RandomAccessFile;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


public class ShareControl
{
  private final RandomAccessFile file;
  private java.nio.channels.FileLock lock = null;
  
  
  public ShareControl(String file) throws Exception
  {
    this.file = new RandomAccessFile(file,"rw");
  }
  
  
  public InstanceData read(boolean modify) throws Exception
  {
    this.lock();
    InstanceData data = null;
    
    if (file.length() > 0)
    {
      byte[] buf = new byte[(int) this.file.length()];

      this.file.read(buf);
      ByteArrayInputStream bin = new ByteArrayInputStream(buf);
      ObjectInputStream oin = new ObjectInputStream(bin);
      data = (InstanceData) oin.readObject();      
    }
    else data = new InstanceData();
    
    if (!modify) this.release();
    return(data);
  }
  
  
  public void write(InstanceData data) throws Exception
  {
    this.file.seek(0L);
    
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    ObjectOutputStream oout = new ObjectOutputStream(bout);

    oout.writeObject(data);
    this.file.write(bout.toByteArray());

    this.release();
  }
  
  
  private void lock() throws Exception
  {
    lock = this.file.getChannel().lock();
  }
  
  
  private void release() throws Exception
  {
    lock.release();
  }
}
