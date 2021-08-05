package instances;

import java.io.RandomAccessFile;


public class SharedData
{
  private final RandomAccessFile file;
  private java.nio.channels.FileLock lock = null;


  public SharedData(String file) throws Exception
  {
    this.file = new RandomAccessFile(file,"rw");
  }


  @SuppressWarnings("unchecked")
  public InstanceData read(boolean modify) throws Exception
  {
    this.lock();
    this.file.seek(0L);

    byte[] buf = null;

    if (file.length() > 0)
    {
      buf = new byte[(int) this.file.length()];
      this.file.read(buf);
    }

    InstanceData data = new InstanceData(buf);

    if (!modify) this.release();
    return(data);
  }


  public void write(InstanceData data) throws Exception
  {
    this.file.seek(0L);
    this.file.write(data.serialize());
    this.release();
  }


  private void lock() throws Exception
  {
    synchronized(this)
    {
      while(lock != null) this.wait();
      lock = this.file.getChannel().lock();
    }
  }


  private void release() throws Exception
  {
    lock.release();
    synchronized(this)
    {
      lock = null;
      this.notifyAll();
    }
  }
}
