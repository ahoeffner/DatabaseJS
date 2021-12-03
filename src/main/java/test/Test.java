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

package test;

import java.io.File;
import java.io.FileInputStream;


public class Test
{
  public static void main(String[] args) throws Exception
  {
    if (args.length < 4)
    {
      System.out.println("loadtest url ips threads loops [payload-file]");
      System.exit(-1);
    }
    
    System.out.println("Loops "+args[3]);
    System.out.println("Threads "+args[2]);
    System.out.println("Hits per connect "+args[1]);
    System.out.println("Payload "+args[4]);
    System.out.println("Url "+args[0]);

    String url = args[0];
    int ips = Integer.parseInt(args[1]);
    int loops = Integer.parseInt(args[3]);
    int threads = Integer.parseInt(args[2]);

    String payload = null;
    if (args.length > 4)
    {
      File file = new File(args[4]);
      byte[] buf = new byte[(int) file.length()];

      FileInputStream in = new FileInputStream(file);
      int read = in.read(buf);
      in.close();

      payload = new String(buf,0,read);
    }

    TestThread.start(url,ips,threads,loops,payload);
  }
}