package ruhack.phobia.system.configsystem.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import ruhack.phobia.system.configsystem.impl.consolelogger.Logger;

public class ConfigFileHandler {
   private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

   public void createDirectories() {
      try {
         Files.createDirectories(ConfigPath.getConfigDirectory());
      } catch (IOException e) {
         Logger.error("AutoConfiguration: Failed to create directories!");
      }
   }

   public boolean write(String content) {
      return this.write(ConfigPath.getConfigFile(), content);
   }

   public boolean write(Path configFile, String content) {
      this.lock.writeLock().lock();

      try {
         Files.createDirectories(configFile.getParent());
         Path tempFile = configFile.resolveSibling(configFile.getFileName() + ".tmp");
         Files.writeString(tempFile, content, StandardCharsets.UTF_8);

         try {
            Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING);
         }

         return true;
      } catch (IOException e) {
         Logger.error("AutoConfiguration: Write failed! " + e.getMessage());
         return false;
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   public String read() {
      return this.read(ConfigPath.getConfigFile());
   }

   public String read(Path configFile) {
      this.lock.readLock().lock();

      try {
         return !Files.exists(configFile) ? null : Files.readString(configFile, StandardCharsets.UTF_8);
      } catch (IOException e) {
         Logger.error("AutoConfiguration: Read failed! " + e.getMessage());
         return null;
      } finally {
         this.lock.readLock().unlock();
      }
   }

   public boolean exists() {
      return Files.exists(ConfigPath.getConfigFile());
   }

   public boolean exists(Path path) {
      return Files.exists(path);
   }
}
