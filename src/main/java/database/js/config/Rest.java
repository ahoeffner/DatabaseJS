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


public class Rest
{
  public final int dump;
  public final int timeout;
  public final int ssotimeout;
  public final String fileroot;
  public final boolean tmpfiles;


  public Rest(JSONObject config)
  {
    this.dump = Config.get(config,"ses.dump",0);
    this.timeout = Config.get(config,"ses.timeout");
    this.ssotimeout = Config.get(config,"sso.timeout");

    String fileroot = Config.get(config,"files.root");

    this.tmpfiles = Config.get(config,"files.tmpnames");
    this.fileroot = Config.getPath(fileroot,Paths.apphome);
  }
}