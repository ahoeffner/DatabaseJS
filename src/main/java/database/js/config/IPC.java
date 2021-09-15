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

import ipc.config.Config;
import java.util.Properties;


public class IPC
{
  public static Config getConfig(String path, short processes, int extnds, String extsize, short statesize)
  {
    Properties props = new Properties();
    
    props.put("path",path);
    props.put("attachments",path);
    
    props.put("withdraw",1000);
    props.put("heartbeat",250);
    
    props.put("singlemaster",true);
      
    props.put("procguard",true);
    props.put("processes",processes);

    props.put("response.eagerfreq",0);
    props.put("response.eagerruns",10);

    props.put("request.pollfreq",100);
    props.put("response.pollfreq",2);

    props.put("objects",processes);

    props.put("namesize",(short) 3);
    props.put("objectsize",statesize);

    props.put("extends",extnds);
    props.put("extendsize",extsize);

    Config config = new Config(props);
    return(config);
  }
}
