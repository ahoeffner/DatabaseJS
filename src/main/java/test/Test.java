package test;

import java.io.File;
import java.io.FileInputStream;


public class Test
{
  public static void main(String[] args) throws Exception
  {
    if (args.length < 3)
    {
      System.out.println("loadtest url threads loops [payload-file]");
      System.exit(-1);
    }
    
    String url = args[0];
    int loops = Integer.parseInt(args[2]);
    int threads = Integer.parseInt(args[1]);
    
    String payload = null;
    if (args.length > 3)
    {
      File file = new File(args[3]);
      byte[] buf = new byte[(int) file.length()];

      FileInputStream in = new FileInputStream(file);
      int read = in.read(buf);
      in.close();
      
      payload = new String(buf,0,read);
    }
    
    TestThread.start(url,threads,loops,payload);
  }
}
