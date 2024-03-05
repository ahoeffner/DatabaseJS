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
import java.util.HashMap;
import java.io.FileReader;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.ByteArrayOutputStream;
import database.json.handlers.json.parser.Source;
import database.json.handlers.json.parser.SourceType;


public class Access extends Thread
{
   private final int reload;
   private final AccessFile[] files;

   private boolean unsigned = false;

   private HashMap<String,Trustee> trustees =
      new HashMap<String,Trustee>();

   private final static Logger logger = Logger.getLogger("json");


   public static void load(Config config) throws Exception
   {
      Security sec = config.getSecurity();

      int reload = sec.reload();
      String[] files = sec.access();

      if (files != null) new Access(files,reload);
   }


   private Access(String[] files, int reload)
   {
      this.reload = reload;
      this.files = new AccessFile[files.length];

      for (int i = 0; i < files.length; i++)
         this.files[i] = new AccessFile(files[i]);

      this.setDaemon(true);
      this.setName("Access Monitor");

      reload();
      if (reload > 0) this.start();
   }


   public boolean acceptUnsigned()
   {
      return(unsigned);
   }


   public Trustee getTrustee(JSONObject msg)
   {
      if (!msg.has("signature")) return(null);
      String sign = msg.getString("signature");
      return(trustees.get(sign));
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
      boolean changed = false;

      HashMap<String,Source> sources =
         new HashMap<String,Source>();

      HashMap<String,Trustee> trustees =
         new HashMap<String,Trustee>();

      for (int i = 0; i < files.length; i++)
      {
         File file = new File(files[i].file);
         long mod = file.lastModified();

         if (mod > files[i].mod)
         {
            logger.info("Reload "+files[i].file);
            JSONObject entries = load(files[i]);

            if (entries != null)
            {
               if (entries.has("access"))
               {
                  JSONObject access = entries.getJSONObject("access");

                  if (access.has("unsigned"))
                     this.unsigned = access.getBoolean("unsigned");

                  if (access.has("trustees"))
                  {
                     JSONArray trust = access.getJSONArray("trustees");
                     for (int t = 0; t < trust.length(); t++)
                     {
                        JSONObject trustee = trust.getJSONObject(t);

                        String name = trustee.getString("name");
                        String sign = trustee.getString("signature");
                        trustees.put(sign,new Trustee(name,sign));
                     }
                  }
               }

               if (entries.has("datasources"))
               {
                  JSONArray ds = entries.getJSONArray("datasources");

                  for (int s = 0; s < ds.length(); s++)
                  {
                     JSONObject definition = ds.getJSONObject(s);

                     try
                     {
                        Source source = new Source(SourceType.table,definition);
                        sources.put(source.id,source);
                     }
                     catch (Exception e)
                     {
                        logger.log(Level.SEVERE,files[i].file,e);
                        continue;
                     }
                  }
               }

               if (entries.has("functions"))
               {
                  JSONArray ds = entries.getJSONArray("functions");

                  for (int f = 0; f < ds.length(); f++)
                  {
                     JSONObject definition = ds.getJSONObject(f);

                     try
                     {
                        Source source = new Source(SourceType.function,definition);
                        sources.put(source.id,source);
                     }
                     catch (Exception e)
                     {
                        logger.log(Level.SEVERE,files[i].file,e);
                        continue;
                     }
                  }
               }

               if (entries.has("procedures"))
               {
                  JSONArray ds = entries.getJSONArray("procedures");

                  for (int f = 0; f < ds.length(); f++)
                  {
                     JSONObject definition = ds.getJSONObject(f);

                     try
                     {
                        Source source = new Source(SourceType.function,definition);
                        sources.put(source.id,source);
                     }
                     catch (Exception e)
                     {
                        logger.log(Level.SEVERE,files[i].file,e);
                        continue;
                     }
                  }
               }

               changed = true;
               files[i].mod = mod;
            }
         }
      }

      if (changed)
         Source.setSources(sources);
   }


   private JSONObject load(AccessFile file)
   {
      JSONObject sources = null;

      BufferedReader in = null;
      ByteArrayOutputStream out = null;

      try
      {
         String line = "";

         out = new ByteArrayOutputStream();
         in = new BufferedReader(new FileReader(file.file));

         while (line != null)
         {
            line = in.readLine();

            if (line != null && !line.trim().startsWith("//"))
               out.write((line+System.lineSeparator()).getBytes());
         }

         in.close();
         sources = new JSONObject(out.toString());
      }
      catch (Exception e)
      {
         try {in.close();}
         catch(Exception c) {};
      }

      return(sources);
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
