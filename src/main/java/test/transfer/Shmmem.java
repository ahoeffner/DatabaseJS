package test.transfer;

import java.io.File;
import java.io.IOException;

import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

public class Shmmem
{
  private MappedByteBuffer inbox;
  private MappedByteBuffer outbox;
  
  
  public static void main(String[] args) throws Exception
  {
    Shmmem shmmem = new Shmmem();
    shmmem.writeToInbox("Hello".getBytes());
    byte[] data = shmmem.readFromInbox(5);
    System.out.println(new String(data));
  }
  
  
  public Shmmem() throws Exception
  {
    this("/Users/alex/Repository/DatabaseJS/ipc");
  }
  
  
  public Shmmem(String folder) throws Exception
  {
    this.inbox = create(folder+File.separator+"mbox0000.in",16*1024);
    this.outbox = create(folder+File.separator+"mbox0000.out",16*1024);
  }
  
  
  public byte[] readFromInbox(int size)
  {
    byte[] data = new byte[size];
    inbox.position(0);
    inbox.get(data);
    return(data);
  }
  
  
  public byte[] readFromOutbox(int size)
  {
    byte[] data = new byte[size];
    outbox.position(0);
    outbox.get(data);
    return(data);
  }
  
  
  public void writeToInbox(byte[] data)
  {
    inbox.position(0);
    inbox.put(data);
  }
  
  
  public void writeToOutbox(byte[] data)
  {
    outbox.position(0);
    outbox.put(data);
  }
  

  private MappedByteBuffer create(String filename, int size) throws Exception
  {
    FileSystem fs = FileSystems.getDefault();
    FileChannel fc = FileChannel.open(fs.getPath(filename),CREATE,READ,WRITE);
    MappedByteBuffer shmmem = fc.map(FileChannel.MapMode.READ_WRITE,0,size);
    return(shmmem);
  }
}
