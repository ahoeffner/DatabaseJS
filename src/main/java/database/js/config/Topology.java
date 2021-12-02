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

package database.js.config;

import org.json.JSONObject;


public class Topology
{
  public final boolean hot;
  public final short workers;
  public final short waiters;
  public final short servers;

  public final int heartbeat;

  public final int extnds;
  public final int extsize;

  public static final int cores = Runtime.getRuntime().availableProcessors();


  @SuppressWarnings("cast")
  Topology(JSONObject config) throws Exception
  {
    this.servers = Config.get(config,"servers",0).shortValue();

    short waiters = Config.get(config,"waiters",0).shortValue();
    short workers = Config.get(config,"workers",0).shortValue();

    short multi = servers > 0 ? servers : 1;

    if (waiters == 0)
    {
      waiters = (short) cores;
      if (waiters < 4) waiters = (short) 4;
    }

    this.waiters = waiters;

    if (workers > 0) this.workers = workers;
    else             this.workers = (short) (multi * 8 * cores);

    this.hot = Config.get(config,"hot-standby");

    JSONObject ipc = config.getJSONObject("ipc");

    this.extnds = this.workers * 2;

    String extsz = Config.get(ipc,"extsize").toString();
    extsz = extsz.replaceAll(" ","").trim().toUpperCase();

    int mfac = 1;

    if (extsz.endsWith("K"))
    {
      mfac = 1024;
      extsz = extsz.substring(0,extsz.length()-1);
    }
    else if (extsz.endsWith("M"))
    {
      mfac = 1024 * 1024;
      extsz = extsz.substring(0,extsz.length()-1);
    }

    this.extsize = Integer.parseInt(extsz) * mfac;

    this.heartbeat = Config.get(ipc,"heartbeat");
  }
}