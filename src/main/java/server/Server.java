package server;

import config.Config;
import java.net.InetAddress;


public class Server extends Thread
{
  public final int inst;
  public final Config config;

  private Listener ssl = null;
  private Listener plain = null;
  private Listener admin = null;


  public Server(Config config, int inst) throws Exception
  {
    this.inst = inst;
    this.config = config;
  }


  public void startup() throws Exception
  {
    try
    {
      long time = System.nanoTime();
      config.log.logger.info("instance starting");

      PKIContext pki = null;
      String host = hostname();

      int pssl = config.http.ssl + inst;
      int pplain = config.http.plain + inst;
      int padmin = config.http.admin + inst;

      if (config.http.requiressl)
      {
        pki = new PKIContext(config.security.identity,config.security.trust);

        ssl = new Listener(this,pki,host,pssl); ssl.start();
        config.log.logger.info("listening on port "+pssl+", elapsed: "+elapsed(time));

        if (pplain > 0)
        {
          plain = new Listener(this,null,host,pplain,pssl); plain.start();
          config.log.logger.info("listening on port "+pplain+", elapsed: "+elapsed(time));
        }
      }
      else
      {
        if (pplain > 0)
        {
          plain = new Listener(this,null,host,pplain); plain.start();
          config.log.logger.info("listening on port "+pplain+", elapsed: "+elapsed(time));
        }

        pki = new PKIContext(config.security.identity,config.security.trust);

        ssl = new Listener(this,pki,host,pssl); ssl.start();
        config.log.logger.info("listening on port "+pssl+", elapsed: "+elapsed(time));
      }

      admin = new Listener(this,pki,host,padmin,true);
      admin.start();

      config.log.logger.info("listening on port "+padmin+", elapsed: "+elapsed(time));

      this.start();

      config.log.logger.info("instance started, elapsed: "+elapsed(time));
    }
    catch (Exception e)
    {
      config.log.exception(e);
      throw e;
    }
  }


  private String hostname()
  {
    String host = "localhost";

    try
    {
      InetAddress ip = InetAddress.getLocalHost();
      host = ip.getHostName();
    }
    catch (Exception e)
    {
      config.log.exception(e);
    }

    return(host);
  }


  private String elapsed(long time)
  {
    double elapsed = (System.nanoTime()-time)/1000000000.0;
    return(String.format("%.3f",elapsed)+" secs");
  }


  @Override
  public void run()
  {
    Thread.currentThread().setName("Server Thread");

    try
    {
      synchronized(this)
      { wait(); }
    }
    catch (Exception e)
    {
      config.log.exception(e);
    }
  }
}
