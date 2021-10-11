package test.transfer;

import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;


public class Server extends Thread
{
  public static void main(String[] args)
  {
    Server server = new Server();
    server.start();
  }
  
  
  public void run()
  {
    try
    {
      int size = 11;      
      Shmmem shmmem = new Shmmem();

      ServerSocket server = new ServerSocket(9002);

      Socket socket = server.accept();
      
      //socket.setTcpNoDelay(true);
      //socket.setSendBufferSize(size);
      //socket.setReceiveBufferSize(size);
      
      InputStream in = socket.getInputStream();
      OutputStream out = socket.getOutputStream();

      SocketReader reader = new SocketReader(in);
      
      while(true)
      {
        byte[] crec = reader.read(8);
        
        byte[] msg = shmmem.readFromInbox(size);
        shmmem.writeToOutbox(msg);

        out.write(crec);
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
