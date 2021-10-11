package test.transfer;

import java.io.InputStream;


public class SocketReader
{
  private int pos = 0;
  private int size = 0;
  private final InputStream in;
  private final static int MAX = 8192;
  private final byte[] buffer = new byte[MAX];


  public SocketReader(InputStream in)
  {
    this.in = in;
  }


  public byte read() throws Exception
  {
    if (this.pos < this.size) 
      return(this.buffer[pos++]);

    this.pos = 0;
    this.size = in.read(buffer);
    
    if (this.size == -1)
      throw new Exception("Socket closed");

    return(read());
  }


  public byte[] read(int size) throws Exception
  {
    int pos = 0;
    byte[] data = new byte[size];
    int available = this.size - this.pos;

    while(true)
    {
      if (available > 0)
      {
        if (available > this.size - this.pos) 
          available = size - pos;
        
        System.arraycopy(this.buffer,this.pos,data,pos,available);        
        
        pos += available;
        this.pos += available;
      }
      
      if (pos == size)
        break;
      
      this.pos = 0;
      this.size = in.read(buffer);
      available = this.size - this.pos;
      
      if (this.size == -1)
        throw new Exception("Socket closed");      
    }
    
    return(data);
  }
}
