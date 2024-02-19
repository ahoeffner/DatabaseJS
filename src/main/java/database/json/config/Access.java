/*
  MIT License

  Copyright © 2023 Alex Høffner

  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
  and associated documentation files (the “Software”), to deal in the Software without
  restriction, including without limitation the rights to use, copy, modify, merge, publish,
  distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
  Software is furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all copies or
  substantial portions of the Software.

  THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
  BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package database.json.config;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Access extends Thread
{
   private final int reload;
   private final AccessFile[] files;
   private final static Logger logger = Logger.getLogger("json");


   public static void load(Config config) throws Exception
   {
      Security sec = config.getSecurity();
      new Access(sec.access(),sec.reload());
   }


   private Access(String[] files, int reload)
   {
      this.reload = reload;
      this.files = new AccessFile[files.length];

      for (int i = 0; i < files.length; i++)
         this.files[i] = new AccessFile(files[i]);

      this.setDaemon(true);
      this.setName("Access Monitor");

      if (reload > 0) this.start();
   }


   @Override
   public void run()
   {
      try
      {
         while (true)
         {
            sleep(reload * 1000);
            reload();
         }
      }
      catch (Exception e)
      {
         logger.log(Level.SEVERE,e.getMessage(),e);
      }
   }

   private void reload()
   {
      for (int i = 0; i < files.length; i++)
      {
         logger.info(files[i].file);

         File file = new File(files[i].file);
         long mod = file.lastModified();

         if (mod > files[i].mod)
         {
            files[i].mod = mod;
            logger.info("Reload "+files[i].file);
         }
      }
   }


   private static class AccessFile
   {
      long mod = 0;
      String file = null;

      AccessFile(String file)
      {
         this.file = Config.getPath(file,Paths.apphome);
      }
   }
}
