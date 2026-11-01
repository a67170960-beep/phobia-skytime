package ruhack.phobia.system.configsystem.impl;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigPath {
   private static final String ROOT_DIR = "Phobia";
   private static final String CONFIG_DIR = "configs";
   private static final String AUTO_DIR = "autocfg";
   private static final String CONFIG_FILE = "autoconfig.file";
   private static Path runDirectory;

   public static void init() {
      runDirectory = Paths.get("").toAbsolutePath();
   }

   public static Path getConfigDirectory() {
      return runDirectory.resolve("Phobia").resolve("configs").resolve("autocfg");
   }

   public static Path getConfigFile() {
      return getConfigDirectory().resolve("autoconfig.file");
   }

   public static Path getNamedConfigFile(String name) {
      String safeName = sanitizeConfigName(name);
      return getConfigDirectory().resolve(safeName + ".file");
   }

   public static String sanitizeConfigName(String name) {
      if (name == null) {
         throw new IllegalArgumentException("Config name is missing");
      }

      String safeName = name.trim();
      if (safeName.toLowerCase().endsWith(".file")) {
         safeName = safeName.substring(0, safeName.length() - 5);
      }

      safeName = safeName.replaceAll("[^\\p{L}\\p{N}_-]", "_");
      if (safeName.isBlank()) {
         throw new IllegalArgumentException("Config name is empty");
      } else {
         return safeName;
      }
   }
}
