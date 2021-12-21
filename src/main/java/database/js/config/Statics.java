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

import database.js.admin.Client;
import database.js.security.OAuth;
import database.js.database.Database;
import database.js.handlers.CrossOrigin;


public class Statics
{
  static void init(Config config) throws Exception
  {
    OAuth.init(config);
    Database.setUrl(config.getDatabase().url);
    Database.setTestSQL(config.getDatabase().test);
    CrossOrigin.init(config.getHTTP().host,config.getHTTP().corsdomains);
    Client.setConfig(config.getPKIContext(),config.getHTTP().bufsize,Config.clientTimeout());
  }
}