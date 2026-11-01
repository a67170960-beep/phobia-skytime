package ruhack.phobia.system.configsystem.impl.autobuyconfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import lombok.Generated;

public class AutoBuyConfig {
   private static AutoBuyConfig instance;
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private final Path configPath;
   private AutoBuyConfig.ConfigData data = new AutoBuyConfig.ConfigData();

   private AutoBuyConfig() {
      Path configDir = Paths.get("Phobia", "configs", "autobuy");

      try {
         Files.createDirectories(configDir);
      } catch (IOException var3) {
      }

      this.configPath = configDir.resolve("autobuy.file");
      this.load();
   }

   public static AutoBuyConfig getInstance() {
      if (instance == null) {
         instance = new AutoBuyConfig();
      }

      return instance;
   }

   public void load() {
      try {
         if (Files.exists(this.configPath)) {
            String json = Files.readString(this.configPath);
            AutoBuyConfig.ConfigData loaded = (AutoBuyConfig.ConfigData)this.gson.fromJson(json, AutoBuyConfig.ConfigData.class);
            if (loaded != null) {
               this.data = loaded;
               if (this.data.getItems() == null) {
                  this.data.setItems(new HashMap<>());
               }
            }
         }
      } catch (IOException var3) {
      }
   }

   public void save() {
      try {
         String json = this.gson.toJson(this.data);
         Files.writeString(this.configPath, json);
      } catch (IOException var2) {
      }
   }

   public void reset() {
      this.data = new AutoBuyConfig.ConfigData();

      try {
         if (Files.exists(this.configPath)) {
            Files.delete(this.configPath);
         }
      } catch (IOException var2) {
      }

      this.save();
   }

   public boolean isGlobalEnabled() {
      return this.data.isGlobalEnabled();
   }

   public void setGlobalEnabled(boolean enabled) {
      this.data.setGlobalEnabled(enabled);
   }

   public void setGlobalEnabledAndSave(boolean enabled) {
      this.data.setGlobalEnabled(enabled);
      this.save();
   }

   public AutoBuyConfig.ItemConfig getItemConfig(String itemName) {
      return this.data.getItems().computeIfAbsent(itemName, k -> new AutoBuyConfig.ItemConfig());
   }

   public AutoBuyConfig.ItemConfig getItemConfigOrNull(String itemName) {
      return this.data.getItems().get(itemName);
   }

   public void setItemConfig(String itemName, AutoBuyConfig.ItemConfig config) {
      this.data.getItems().put(itemName, config);
   }

   public void setItemConfigAndSave(String itemName, AutoBuyConfig.ItemConfig config) {
      this.data.getItems().put(itemName, config);
      this.save();
   }

   public void setItemEnabled(String itemName, boolean enabled) {
      AutoBuyConfig.ItemConfig config = this.getItemConfig(itemName);
      config.setEnabled(enabled);
   }

   public void setItemEnabledAndSave(String itemName, boolean enabled) {
      AutoBuyConfig.ItemConfig config = this.getItemConfig(itemName);
      config.setEnabled(enabled);
      this.save();
   }

   public void setItemBuyBelow(String itemName, int buyBelow) {
      AutoBuyConfig.ItemConfig config = this.getItemConfig(itemName);
      config.setBuyBelow(buyBelow);
   }

   public void setItemBuyBelowAndSave(String itemName, int buyBelow) {
      AutoBuyConfig.ItemConfig config = this.getItemConfig(itemName);
      config.setBuyBelow(buyBelow);
      this.save();
   }

   public void setItemMinQuantity(String itemName, int minQuantity) {
      AutoBuyConfig.ItemConfig config = this.getItemConfig(itemName);
      config.setMinQuantity(minQuantity);
   }

   public void setItemMinQuantityAndSave(String itemName, int minQuantity) {
      AutoBuyConfig.ItemConfig config = this.getItemConfig(itemName);
      config.setMinQuantity(minQuantity);
      this.save();
   }

   public boolean isItemEnabled(String itemName) {
      AutoBuyConfig.ItemConfig config = this.getItemConfigOrNull(itemName);
      return config != null && config.isEnabled();
   }

   public int getItemBuyBelow(String itemName) {
      return this.getItemConfig(itemName).getBuyBelow();
   }

   public int getItemMinQuantity(String itemName) {
      return this.getItemConfig(itemName).getMinQuantity();
   }

   public boolean hasItemConfig(String itemName) {
      return this.data.getItems().containsKey(itemName);
   }

   public void loadItemSettings(String itemName, int defaultPrice) {
      if (!this.hasItemConfig(itemName)) {
         AutoBuyConfig.ItemConfig config = new AutoBuyConfig.ItemConfig(false, defaultPrice, 1);
         this.data.getItems().put(itemName, config);
      }
   }

   public Map<String, AutoBuyConfig.ItemConfig> getAllItemConfigs() {
      return new HashMap<>(this.data.getItems());
   }

   @Generated
   public AutoBuyConfig.ConfigData getData() {
      return this.data;
   }

   public static class ConfigData {
      private boolean globalEnabled = false;
      private Map<String, AutoBuyConfig.ItemConfig> items = new HashMap<>();

      @Generated
      public boolean isGlobalEnabled() {
         return this.globalEnabled;
      }

      @Generated
      public Map<String, AutoBuyConfig.ItemConfig> getItems() {
         return this.items;
      }

      @Generated
      public void setGlobalEnabled(boolean globalEnabled) {
         this.globalEnabled = globalEnabled;
      }

      @Generated
      public void setItems(Map<String, AutoBuyConfig.ItemConfig> items) {
         this.items = items;
      }
   }

   public static class ItemConfig {
      private boolean enabled = false;
      private int buyBelow = 1000;
      private int minQuantity = 1;

      public ItemConfig() {
      }

      public ItemConfig(boolean enabled, int buyBelow, int minQuantity) {
         this.enabled = enabled;
         this.buyBelow = buyBelow;
         this.minQuantity = minQuantity;
      }

      @Generated
      public boolean isEnabled() {
         return this.enabled;
      }

      @Generated
      public int getBuyBelow() {
         return this.buyBelow;
      }

      @Generated
      public int getMinQuantity() {
         return this.minQuantity;
      }

      @Generated
      public void setEnabled(boolean enabled) {
         this.enabled = enabled;
      }

      @Generated
      public void setBuyBelow(int buyBelow) {
         this.buyBelow = buyBelow;
      }

      @Generated
      public void setMinQuantity(int minQuantity) {
         this.minQuantity = minQuantity;
      }
   }
}
