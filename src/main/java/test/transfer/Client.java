package test.transfer;

import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;


public class Client
{
  public static void main(String[] args) throws Exception
  {
    Client client = new Client();
    client.test();
  }
  
  
  public void test() throws Exception
  {
    int size = 11;
    String test = "Hello world";
    //String test = String.format("%"+size+"s","x").replace(" ","x");
    
    Shmmem shmmem = new Shmmem();

    int read = 0;
    int loops = 100000;
    byte[] wbuf = test.getBytes();
    Socket socket = new Socket("localhost",9002);

    //socket.setTcpNoDelay(true);
    //socket.setSendBufferSize(size);
    //socket.setReceiveBufferSize(size);
    
    InputStream in = socket.getInputStream();
    OutputStream out = socket.getOutputStream();

    byte[] crec = "12345678".getBytes();
    SocketReader reader = new SocketReader(in);
    
    long time = System.nanoTime();

    for (int i = 0; i < loops; i++)
    {
      shmmem.writeToInbox(wbuf);
      
      out.write(crec);
      crec = reader.read(8);

      byte[] msg = shmmem.readFromOutbox(size);
    }
    
    long elapsed = (System.nanoTime()-time)/1000000;
    System.out.println("wrote="+(loops*size)+" read="+read+" elapsed="+elapsed+" hits/sec="+(loops*1000/elapsed));
  }
}
