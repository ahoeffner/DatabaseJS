/*
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.

 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

package database.js.servers;

import database.js.config.Config;
import database.js.servers.rest.RESTClient;


class LoadBalancer
{
  private int last = -1;
  private final int htsrvs;
  private final int threads;
  private final int servers;
  private final Config config;
  private final RESTClient[] workers;


  LoadBalancer(Config config) throws Exception
  {
    this.config = config;
    this.servers = config.getTopology().servers;
    this.threads = config.getTopology().workers;

    short htsrvs = 1;
    if (config.getTopology().hot) htsrvs++;

    this.htsrvs = htsrvs;
    this.workers = new RESTClient[servers];
  }


  public RESTClient worker(short id)
  {
    return(workers[id-this.htsrvs]);
  }


  public RESTClient worker() throws Exception
  {
    int tries = 0;
    int next = next();

    while(++tries < 32)
    {
      for (int i = 0; i < workers.length; i++)
      {
        if (workers[next] != null && workers[next].up())
          return(workers[next]);

        next = ++next % workers.length;
      }

      Thread.sleep(250);
    }

    throw new Exception("No available RESTEngines, bailing out");
  }


  public void register(RESTClient client)
  {
    workers[client.id()-this.htsrvs] = client;
  }


  public void deregister(RESTClient client)
  {
    workers[client.id()-this.htsrvs] = null;
  }


  private synchronized int next()
  {
    last = (++last) % workers.length;
    return(last);
  }
}