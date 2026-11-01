package ruhack.phobia.system.configsystem.impl.userconfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import ruhack.phobia.system.configsystem.ConfigSystem;
import ruhack.phobia.system.configsystem.impl.ConfigPath;
import ruhack.phobia.system.configsystem.impl.consolelogger.Logger;
import ruhack.phobia.system.modulesystem.impl.display.Notifications;

public class UserConfigManager {
   private static UserConfigManager instance;
   private final List<UserConfig> configs = new ArrayList<>();
   private UserConfig activeConfig;

   public UserConfigManager() {
      instance = this;
   }

   public static UserConfigManager getInstance() {
      if (instance == null) {
         new UserConfigManager();
      }

      return instance;
   }

   public void init() {
      try {
         Files.createDirectories(ConfigPath.getConfigDirectory());
      } catch (IOException e) {
         Logger.error("UserConfig: Failed to create directories!");
      }

      this.loadConfigList();
   }

   public void loadConfigList() {
      String activeName = this.activeConfig == null ? null : this.activeConfig.getName();
      this.configs.clear();
      ConfigSystem system = ConfigSystem.getInstance();
      if (system == null) {
         this.activeConfig = null;
      } else {
         for (String name : system.listNamedConfigs()) {
            Path file = ConfigPath.getNamedConfigFile(name);
            long updatedAt = lastModified(file);
            this.configs.add(new UserConfig(name, "Local", file.getFileName().toString(), updatedAt, updatedAt));
         }

         this.activeConfig = activeName == null ? null : this.find(activeName);
      }
   }

   public void saveConfig(String name) {
      ConfigSystem system = ConfigSystem.getInstance();
      if (system != null && system.saveNamed(name)) {
         String safeName = ConfigPath.sanitizeConfigName(name);
         this.loadConfigList();
         this.activeConfig = this.find(safeName);
         Logger.success("UserConfig: Saved config '" + safeName + "'");
      } else {
         Logger.error("UserConfig: Failed to save config!");
      }
   }

   public void loadConfig(UserConfig config) {
      ConfigSystem system = ConfigSystem.getInstance();
      if (system != null && config != null && system.loadNamed(config.getName())) {
         this.activeConfig = config;
         Logger.success("UserConfig: Loaded config '" + config.getName() + "'");
         Notifications.config(config.getName());
      } else {
         Logger.error("UserConfig: Failed to load config!");
      }
   }

   public void deleteConfig(UserConfig config) {
      ConfigSystem system = ConfigSystem.getInstance();
      if (system != null && config != null && system.deleteNamed(config.getName())) {
         if (this.activeConfig != null && this.activeConfig.getName().equalsIgnoreCase(config.getName())) {
            this.activeConfig = null;
         }

         this.loadConfigList();
         Logger.success("UserConfig: Deleted config '" + config.getName() + "'");
      } else {
         Logger.error("UserConfig: Failed to delete config!");
      }
   }

   public List<UserConfig> getConfigs() {
      return List.copyOf(this.configs);
   }

   public UserConfig getActiveConfig() {
      return this.activeConfig;
   }

   public void setActiveConfigName(String name) {
      this.loadConfigList();
      this.activeConfig = this.find(ConfigPath.sanitizeConfigName(name));
   }

   private UserConfig find(String name) {
      return this.configs.stream().filter(config -> config.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
   }

   private static long lastModified(Path file) {
      try {
         return Files.getLastModifiedTime(file).toMillis();
      } catch (IOException ignored) {
         return System.currentTimeMillis();
      }
   }
}
