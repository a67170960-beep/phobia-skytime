package ruhack.phobia.system.configsystem;

import com.adl.nativeprotect.Native;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import ruhack.phobia.system.configsystem.impl.ConfigFileHandler;
import ruhack.phobia.system.configsystem.impl.ConfigPath;
import ruhack.phobia.system.configsystem.impl.ConfigSerializer;
import ruhack.phobia.system.configsystem.impl.autosaver.ConfigAutoSaver;
import ruhack.phobia.system.configsystem.impl.consolelogger.Logger;

public class ConfigSystem {
   private static ConfigSystem instance;
   private final ConfigSerializer serializer;
   private final ConfigFileHandler fileHandler;
   private final ConfigAutoSaver autoSaver;
   private final AtomicBoolean initialized;
   private final AtomicBoolean saving;

   public ConfigSystem() {
      instance = this;
      this.serializer = new ConfigSerializer();
      this.fileHandler = new ConfigFileHandler();
      this.autoSaver = new ConfigAutoSaver(this::save);
      this.initialized = new AtomicBoolean(false);
      this.saving = new AtomicBoolean(false);
   }

   public static ConfigSystem getInstance() {
      return instance;
   }

   @Native
   public void init() {
      if (this.initialized.compareAndSet(false, true)) {
         ConfigPath.init();
         this.fileHandler.createDirectories();
         this.load();
         this.autoSaver.start();
         this.registerShutdownHook();
         Logger.success("AutoConfiguration: System initialized!");
      }
   }

   private void registerShutdownHook() {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         Logger.info("AutoConfiguration: Shutdown detected, saving...");
         this.shutdown();
      }, "Rich-ConfigShutdown"));
   }

   public void save() {
      if (this.initialized.get()) {
         if (this.saving.compareAndSet(false, true)) {
            try {
               String data = this.serializer.serialize();
               boolean success = this.fileHandler.write(data);
               if (success) {
                  Logger.success("AutoConfiguration: autoconfig.json saved successfully!");
               } else {
                  Logger.error("AutoConfiguration: autoconfig.json save failed!");
               }
            } catch (Exception e) {
               Logger.error("AutoConfiguration: Save error! " + e.getMessage());
            } finally {
               this.saving.set(false);
            }
         }
      }
   }

   public CompletableFuture<Void> saveAsync() {
      return CompletableFuture.runAsync(this::save);
   }

   public boolean saveNamed(String name) {
      if (!this.initialized.get()) {
         return false;
      }

      try {
         String data = this.serializer.serialize();
         Path target = ConfigPath.getNamedConfigFile(name);
         boolean namedSaved = this.fileHandler.write(target, data);
         if (namedSaved) {
            this.fileHandler.write(data);
         }

         return namedSaved;
      } catch (Exception e) {
         Logger.error("Configuration: Named save error! " + e.getMessage());
         return false;
      }
   }

   public boolean loadNamed(String name) {
      try {
         Path target = ConfigPath.getNamedConfigFile(name);
         String data = this.fileHandler.read(target);
         if (data != null && !data.isBlank()) {
            this.serializer.deserialize(data);
            this.fileHandler.write(data);
            return true;
         } else {
            return false;
         }
      } catch (Exception e) {
         Logger.error("Configuration: Named load error! " + e.getMessage());
         return false;
      }
   }

   public boolean deleteNamed(String name) {
      try {
         return Files.deleteIfExists(ConfigPath.getNamedConfigFile(name));
      } catch (IOException | IllegalArgumentException e) {
         Logger.error("Configuration: Named delete error! " + e.getMessage());
         return false;
      }
   }

   public List<String> listNamedConfigs() {
      Path directory = ConfigPath.getConfigDirectory();
      if (!Files.isDirectory(directory)) {
         return List.of();
      }

      try (Stream<Path> files = Files.list(directory)) {
         return files.filter(x$0 -> Files.isRegularFile(x$0))
            .map(path -> path.getFileName().toString())
            .filter(fileName -> fileName.endsWith(".file"))
            .filter(fileName -> !fileName.equals("autoconfig.file"))
            .map(fileName -> fileName.substring(0, fileName.length() - 5))
            .sorted(Comparator.comparing(String::toLowerCase))
            .toList();
      } catch (IOException e) {
         Logger.error("Configuration: Failed to list configs! " + e.getMessage());
         return List.of();
      }
   }

   public void load() {
      if (!this.fileHandler.exists()) {
         Logger.info("AutoConfiguration: No config found, creating new...");
         this.save();
      } else {
         try {
            String data = this.fileHandler.read();
            if (data != null && !data.isEmpty()) {
               this.serializer.deserialize(data);
               Logger.success("AutoConfiguration: autoconfig.json loaded successfully!");
            }
         } catch (Exception e) {
            Logger.error("AutoConfiguration: Load error! " + e.getMessage());
         }
      }
   }

   public void shutdown() {
      if (this.initialized.get()) {
         this.autoSaver.shutdown();
         this.save();
         Logger.success("AutoConfiguration: Shutdown complete!");
      }
   }

   public void reload() {
      this.load();
      Logger.success("AutoConfiguration: Config reloaded!");
   }

   public boolean isInitialized() {
      return this.initialized.get();
   }

   public boolean isSaving() {
      return this.saving.get();
   }

   public ConfigAutoSaver getAutoSaver() {
      return this.autoSaver;
   }
}
