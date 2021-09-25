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

package database.js.cluster;

import database.js.servers.Server;


public class Cluster
{
  public static void setStatistics(Server server)
  {
    Statistics.save(server);
  }

  public static Statistics getStatistics(Server server, short id)
  {
    return(Statistics.get(server,id));
  }
}
