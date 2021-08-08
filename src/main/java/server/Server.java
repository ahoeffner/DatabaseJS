package server;

import config.Config;
import instances.Cluster;
import java.net.InetAddress;


public class Server extends Thread
{
  private final int inst;
  private final Config config;
  private final Cluster cluster;

  private Listener ssl = null;
  private Listener plain = null;
  private Listener admin = null;


  public Server(Config config, int inst) throws Exception
  {
    this.inst = inst;
    this.config = config;
    this.cluster = new Cluster(config,this,inst);
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

        ssl = new Listener(config,pki,host,pssl,false);
        ssl.start();

        config.log.logger.info("listening on port "+pssl+", elapsed: "+elapsed(time));

        plain = new Listener(config,null,host,pplain,false);
        plain.start();

        config.log.logger.info("listening on port "+pplain+", elapsed: "+elapsed(time));
      }
      else
      {
        plain = new Listener(config,null,host,pplain,false);
        plain.start();

        config.log.logger.info("listening on port "+pplain+", elapsed: "+elapsed(time));

        pki = new PKIContext(config.security.identity,config.security.trust);

        ssl = new Listener(config,pki,host,pssl,false);
        ssl.start();

        config.log.logger.info("listening on port "+pssl+", elapsed: "+elapsed(time));
      }

      admin = new Listener(config,pki,host,padmin,true);
      admin.start();

      config.log.logger.info("listening on port "+padmin+", elapsed: "+elapsed(time));

      Runtime.getRuntime().addShutdownHook(new ShutdownHook(cluster));
      cluster.register();

      this.start();

      config.log.logger.info("instance started, elapsed: "+elapsed(time));

      if (cluster.manager())
        config.log.cluster.info("cluster started, elapsed: "+elapsed(time));
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


  private static class ShutdownHook extends Thread
  {
    private final Cluster cluster;

    public ShutdownHook(Cluster cluster)
    {
      this.cluster = cluster;
    }

    public void run()
    {
      try
      {
        cluster.deregister();
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }
}
