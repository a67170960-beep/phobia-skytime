package ruhack.phobia.util.aim.neuro;

import com.google.gson.Gson;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class NeuroStore {
   private static final Path DIR = Paths.get("Phobia", "neuro");
   private static final Gson GSON = new Gson();

   private NeuroStore() {
   }

   private static String safe(String name) {
      return name.toLowerCase(Locale.US).replaceAll("[^a-z0-9_\\-]", "_");
   }

   public static Path datasetPath(String name) {
      return DIR.resolve(safe(name) + ".dataset.json");
   }

   public static Path modelPath(String name) {
      return DIR.resolve(safe(name) + ".model.json");
   }

   public static Path backupPath(String name) {
      return DIR.resolve(safe(name) + ".dataset.bak.json");
   }

   public static boolean hasDataset(String name) {
      return Files.exists(datasetPath(name));
   }

   public static boolean hasModel(String name) {
      return Files.exists(modelPath(name));
   }

   public static boolean saveDataset(String name, float[][] data) {
      try {
         Files.createDirectories(DIR);
         Path tmp = DIR.resolve(safe(name) + ".dataset.json.tmp");
         Files.writeString(tmp, GSON.toJson(data));

         try {
            Files.move(tmp, datasetPath(name), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, datasetPath(name), StandardCopyOption.REPLACE_EXISTING);
         }

         return true;
      } catch (Exception e) {
         return false;
      }
   }

   public static void backupDataset(String name) {
      try {
         Path p = datasetPath(name);
         if (Files.exists(p)) {
            Files.copy(p, backupPath(name), StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (Exception var2) {
      }
   }

   public static float[][] loadDataset(String name) {
      float[][] main = readFrames(datasetPath(name));
      return main.length > 0 ? main : readFrames(backupPath(name));
   }

   private static float[][] readFrames(Path p) {
      try {
         if (!Files.exists(p)) {
            return new float[0][];
         }

         float[][] data = (float[][])GSON.fromJson(Files.readString(p), float[][].class);
         return data != null ? data : new float[0][];
      } catch (Exception e) {
         return new float[0][];
      }
   }

   public static List<String> list() {
      List<String> names = new ArrayList<>();
      if (!Files.exists(DIR)) {
         return names;
      }

      try (Stream<Path> files = Files.list(DIR)) {
         files.map(p -> p.getFileName().toString()).forEach(f -> {
            String base = null;
            if (f.endsWith(".dataset.json")) {
               base = f.substring(0, f.length() - ".dataset.json".length());
            } else if (f.endsWith(".model.json")) {
               base = f.substring(0, f.length() - ".model.json".length());
            }

            if (base != null && !names.contains(base)) {
               names.add(base);
            }
         });
      } catch (Exception var6) {
      }

      return names;
   }

   public static void remove(String name) {
      try {
         Files.deleteIfExists(datasetPath(name));
         Files.deleteIfExists(modelPath(name));
         Files.deleteIfExists(backupPath(name));
      } catch (Exception var2) {
      }
   }

   public static void removeAll() {
      for (String name : list()) {
         remove(name);
      }
   }
}
