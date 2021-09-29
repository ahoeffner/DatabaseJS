package test;

import java.net.URL;
import java.io.InputStream;
import java.net.URLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.HttpsURLConnection;


public class Files extends Thread
{
  private long avg;
  private int failed;
  private long elapsed;
  private final int loops;
  private final String url;
  private static final TrustManager[] tmgrs = new TrustManager[] {new FakeTrustManager()};
  
  
  public static void start(String url, int threads, int loops) throws Exception
  {
    Files tests[] = new Files[threads];
    for (int i = 0; i < tests.length; i++) tests[i] = new Files(loops,url);
    
    System.out.println();
    System.out.println("Testing static files interface threads: "+threads+" loops: "+loops+" "+url+" no delay");
    System.out.println();

    long avg = 0;
    int failed = 0;
    long time = System.currentTimeMillis();

    for (int i = 0; i < tests.length; i++) tests[i].start();
    for (int i = 0; i < tests.length; i++) tests[i].join();
    
    for (int i = 0; i < tests.length; i++) {avg += tests[i].avg; failed += tests[i].failed;}
    
    time = System.currentTimeMillis() - time;
    System.out.println(loops*threads+" pages served in "+time/1000+" secs, failed "+failed+", "+(loops*threads*1000)/time+" pages/sec, response time "+avg/(loops*threads*1000000.0)+" ms");
  }
  
  
  private Files(int loops, String url)
  {
    this.url = url;
    this.loops = loops;
  }
  
  
  public void run()
  {
    long time = System.currentTimeMillis();      

    try
    {
      URL url = new URL(this.url);

      if (this.url.startsWith("https"))
      {
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null,tmgrs, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      }
            
      for (int i = 0; i < loops; i++)
      {
        long req = System.nanoTime();

        try
        {
          
          URLConnection conn = url.openConnection();
          int size = conn.getContentLength();
          InputStream in = conn.getInputStream();
          byte[] content = new byte[size];

          int read = 0;
          while (read < content.length) 
            read += in.read(content);
          
          if (read != content.length)
            throw new Exception("server didn't receive all bytes");
        }
        catch (Exception e)
        {
          failed++;
        }
        
        avg += System.nanoTime()-req;
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    
    this.elapsed = System.currentTimeMillis() - time;
  }  
}
