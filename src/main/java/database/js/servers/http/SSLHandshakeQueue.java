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

package database.js.servers.http;

import java.util.ArrayList;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ConcurrentHashMap;


class SSLHandshakeQueue
{
  private final ArrayList<SSLHandshake> done =
    new ArrayList<SSLHandshake>();

  private final ConcurrentHashMap<SelectionKey,SSLHandshake> incomplete =
    new ConcurrentHashMap<SelectionKey,SSLHandshake>();


  boolean next()
  {
    return(done.size() > 0);
  }


  void register(SSLHandshake ses)
  {
    incomplete.put(ses.key(),ses);
  }


  boolean isWaiting()
  {
    return(incomplete.size() + done.size() > 0);
  }


  SSLHandshake getSession()
  {
    return(done.remove(0));
  }


  void done(SSLHandshake ses)
  {
    done.add(ses);
    incomplete.remove(ses.key());
  }


  void remove(SSLHandshake ses)
  {
    incomplete.remove(ses.key());
  }
}
