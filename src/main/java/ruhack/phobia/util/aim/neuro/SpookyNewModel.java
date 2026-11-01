package ruhack.phobia.util.aim.neuro;

import com.google.gson.Gson;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class SpookyNewModel {
   private static final Gson GSON = new Gson();
   private static final NeuroModel MODEL = new NeuroModel();
   private static volatile boolean ready;
   private static volatile boolean attempted;

   private SpookyNewModel() {
   }

   public static NeuroModel get() {
      ensureLoaded();
      return MODEL;
   }

   public static boolean isReady() {
      ensureLoaded();
      return ready;
   }

   private static void ensureLoaded() {
      if (!attempted) {
         synchronized (SpookyNewModel.class) {
            if (!attempted) {
               attempted = true;
               float[][] raw = NeuroStore.loadDataset("spooky");
               if (raw.length == 0) {
                  raw = NeuroStore.loadDataset("holy8k");
               }

               if (raw.length == 0) {
                  raw = loadEmbedded("spooky");
               }

               if (raw.length == 0) {
                  raw = loadEmbedded("holy8k");
               }

               if (raw.length == 0) {
                  raw = NeuroStore.loadDataset("holy");
               }

               if (raw.length == 0) {
                  raw = loadEmbedded("holy");
               }

               if (raw.length > 0) {
                  MODEL.fit(raw);
                  ready = true;
               }
            }
         }
      }
   }

   private static float[][] loadEmbedded(String name) {
      try (InputStream in = SpookyNewModel.class.getResourceAsStream("/phobia/neuro/" + name + ".dataset.json")) {
         if (in == null) {
            return new float[0][];
         }

         String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
         float[][] data = (float[][])GSON.fromJson(json, float[][].class);
         return data != null ? data : new float[0][];
      } catch (Exception e) {
         return new float[0][];
      }
   }
}
