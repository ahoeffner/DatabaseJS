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
   private long last = 0;
   private final int reload;
   private final String folder;
   private static boolean unsigned = false;

   private static HashMap<String,Trustee> trustees =
      new HashMap<String,Trustee>();

   private final static Logger logger = Logger.getLogger("json");


   public static void load(Config config) throws Exception
   {
      Security sec = config.getSecurity();

      int reload = sec.reload();
      String folder = sec.access();

      if (folder != null) new Access(folder,reload);
   }


   private Access(String folder, int reload)
   {
      this.reload = reload;
      this.folder = folder;

      this.setDaemon(true);
      this.setName("Access Monitor");

      reload();
      if (reload > 0) this.start();
   }


   public static boolean acceptUnsigned()
   {
      return(unsigned);
   }


   public static Trustee getTrustee(JSONObject msg)
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
      boolean reload = false;

      HashMap<String,Source> sources =
         new HashMap<String,Source>();

      HashMap<String,Trustee> trustees =
         new HashMap<String,Trustee>();

      File folder = new File(this.folder);
      String[] files = folder.list();

      for (int i = 0; i < files.length; i++)
      {
         File file = file(files[i]);
         long mod = file.lastModified();

         if (mod > this.last)
         {
            reload = true;
            this.last = mod;
         }
      }

      if (reload)
      {
         for (int i = 0; i < files.length; i++)
         {
            logger.info("Reload "+files[i]);
            JSONObject entries = load(files[i]);

            if (entries != null)
            {
               if (entries.has("access"))
               {
                  JSONObject access = entries.getJSONObject("access");

                  if (access.has("unsigned"))
                     Access.unsigned = access.getBoolean("unsigned");

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
                        logger.log(Level.SEVERE,files[i],e);
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
                        logger.log(Level.SEVERE,files[i],e);
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
                        logger.log(Level.SEVERE,files[i],e);
                        continue;
                     }
                  }
               }

               if (entries.has("sql"))
               {
                  JSONArray ds = entries.getJSONArray("sql");

                  for (int f = 0; f < ds.length(); f++)
                  {
                     JSONObject definition = ds.getJSONObject(f);

                     try
                     {
                        Source source = new Source(SourceType.sql,definition);
                        sources.put(source.id,source);
                     }
                     catch (Exception e)
                     {
                        logger.log(Level.SEVERE,files[i],e);
                        continue;
                     }
                  }
               }
            }
         }

         Access.trustees = trustees;
         Source.setSources(sources);
      }
   }


   private JSONObject load(String file)
   {
      JSONObject sources = null;

      BufferedReader in = null;
      ByteArrayOutputStream out = null;

      try
      {
         String line = "";

         out = new ByteArrayOutputStream();
         in = new BufferedReader(new FileReader(file(file)));

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


   private File file(String file)
   {
      file = folder + File.separator + file;
      return(new File(file));
   }
}
