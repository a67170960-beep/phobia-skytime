package ruhack.phobia.system.configsystem.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.configsystem.impl.autobuyconfig.AutoBuyConfig;
import ruhack.phobia.system.configsystem.impl.consolelogger.Logger;
import ruhack.phobia.system.modulesystem.ModuleRepository;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.setting.Setting;
import ruhack.phobia.system.modulesystem.setting.implement.BindSetting;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.ColorSetting;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.system.modulesystem.setting.implement.TextSetting;

public class ConfigSerializer {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

   public String serialize() {
      JsonObject root = new JsonObject();
      JsonObject modulesJson = new JsonObject();
      ModuleRepository repository = this.getModuleRepository();
      if (repository != null) {
         for (ModuleStructure module : repository.modules()) {
            JsonObject moduleJson = this.serializeModule(module);
            modulesJson.add(module.getName(), moduleJson);
         }
      }

      root.add("modules", modulesJson);
      root.add("autobuy", this.serializeAutoBuy());
      root.addProperty("version", "2.0");
      root.addProperty("timestamp", System.currentTimeMillis());
      root.addProperty("client", "Phobia Beta");
      return GSON.toJson(root);
   }

   private JsonObject serializeAutoBuy() {
      JsonObject autoBuyJson = new JsonObject();
      AutoBuyConfig autoBuyConfig = AutoBuyConfig.getInstance();
      autoBuyJson.addProperty("globalEnabled", autoBuyConfig.isGlobalEnabled());
      JsonObject itemsJson = new JsonObject();
      Map<String, AutoBuyConfig.ItemConfig> allItems = autoBuyConfig.getAllItemConfigs();

      for (Entry<String, AutoBuyConfig.ItemConfig> entry : allItems.entrySet()) {
         JsonObject itemJson = new JsonObject();
         AutoBuyConfig.ItemConfig itemConfig = entry.getValue();
         itemJson.addProperty("enabled", itemConfig.isEnabled());
         itemJson.addProperty("buyBelow", itemConfig.getBuyBelow());
         itemJson.addProperty("minQuantity", itemConfig.getMinQuantity());
         itemsJson.add(entry.getKey(), itemJson);
      }

      autoBuyJson.add("items", itemsJson);
      return autoBuyJson;
   }

   private JsonObject serializeModule(ModuleStructure module) {
      JsonObject moduleJson = new JsonObject();
      moduleJson.addProperty("enabled", module.isState());
      moduleJson.addProperty("key", module.getKey());
      moduleJson.addProperty("type", module.getType());
      moduleJson.addProperty("favorite", module.isFavorite());
      JsonObject settingsJson = new JsonObject();

      for (Setting setting : module.settings()) {
         JsonElement element = this.serializeSetting(setting);
         if (element != null) {
            settingsJson.add(setting.getName(), element);
         }
      }

      moduleJson.add("settings", settingsJson);
      return moduleJson;
   }

   private JsonElement serializeSetting(Setting setting) {
      if (setting instanceof BooleanSetting boolSetting) {
         return new JsonPrimitive(boolSetting.isValue());
      } else if (setting instanceof SliderSettings sliderSetting) {
         return new JsonPrimitive(sliderSetting.getValue());
      } else if (setting instanceof BindSetting bindSetting) {
         JsonObject bindJson = new JsonObject();
         bindJson.addProperty("key", bindSetting.getKey());
         bindJson.addProperty("type", bindSetting.getType());
         bindJson.addProperty("mouse", bindSetting.isMouse());
         return bindJson;
      } else if (setting instanceof TextSetting textSetting) {
         return new JsonPrimitive(textSetting.getValue() != null ? textSetting.getValue() : "");
      } else if (setting instanceof SelectSetting selectSetting) {
         return new JsonPrimitive(selectSetting.getValue());
      } else if (setting instanceof ColorSetting colorSetting) {
         JsonObject colorJson = new JsonObject();
         colorJson.addProperty("hue", colorSetting.getHue());
         colorJson.addProperty("saturation", colorSetting.getSaturation());
         colorJson.addProperty("brightness", colorSetting.getBrightness());
         colorJson.addProperty("alpha", colorSetting.getAlpha());
         return colorJson;
      } else if (!(setting instanceof MultiSelectSetting multiSetting)) {
         return null;
      } else {
         JsonArray array = new JsonArray();

         for (String value : multiSetting.getSelected()) {
            array.add(value);
         }

         return array;
      }
   }

   public void deserialize(String json) {
      try {
         JsonObject root = JsonParser.parseString(json).getAsJsonObject();
         if (root.has("modules")) {
            JsonObject modulesJson = root.getAsJsonObject("modules");
            ModuleRepository repository = this.getModuleRepository();
            if (repository != null) {
               for (ModuleStructure module : repository.modules()) {
                  if (modulesJson.has(module.getName())) {
                     this.deserializeModule(module, modulesJson.getAsJsonObject(module.getName()));
                  }
               }
            }
         }

         if (root.has("autobuy")) {
            this.deserializeAutoBuy(root.getAsJsonObject("autobuy"));
         }
      } catch (JsonSyntaxException e) {
         Logger.error("AutoConfiguration: File syntax error!");
      }
   }

   private void deserializeAutoBuy(JsonObject autoBuyJson) {
      AutoBuyConfig autoBuyConfig = AutoBuyConfig.getInstance();
      if (autoBuyJson.has("globalEnabled")) {
         autoBuyConfig.setGlobalEnabled(autoBuyJson.get("globalEnabled").getAsBoolean());
      }

      if (autoBuyJson.has("items")) {
         JsonObject itemsJson = autoBuyJson.getAsJsonObject("items");

         for (Entry<String, JsonElement> entry : itemsJson.entrySet()) {
            String itemName = entry.getKey();
            JsonObject itemJson = entry.getValue().getAsJsonObject();
            boolean enabled = itemJson.has("enabled") && itemJson.get("enabled").getAsBoolean();
            int buyBelow = itemJson.has("buyBelow") ? itemJson.get("buyBelow").getAsInt() : 1000;
            int minQuantity = itemJson.has("minQuantity") ? itemJson.get("minQuantity").getAsInt() : 1;
            AutoBuyConfig.ItemConfig itemConfig = new AutoBuyConfig.ItemConfig(enabled, buyBelow, minQuantity);
            autoBuyConfig.setItemConfig(itemName, itemConfig);
         }
      }
   }

   private void deserializeModule(ModuleStructure module, JsonObject moduleJson) {
      if (moduleJson.has("key")) {
         module.setKey(moduleJson.get("key").getAsInt());
      }

      if (moduleJson.has("type")) {
         module.setType(moduleJson.get("type").getAsInt());
      }

      if (moduleJson.has("favorite")) {
         module.setFavorite(moduleJson.get("favorite").getAsBoolean());
      }

      if (moduleJson.has("settings")) {
         JsonObject settingsJson = moduleJson.getAsJsonObject("settings");

         for (Setting setting : module.settings()) {
            if (settingsJson.has(setting.getName())) {
               this.deserializeSetting(setting, settingsJson.get(setting.getName()));
            }
         }
      }

      if (moduleJson.has("enabled")) {
         boolean enabled = moduleJson.get("enabled").getAsBoolean();
         if (enabled != module.isState()) {
            module.setState(enabled);
         }
      }
   }

   private void deserializeSetting(Setting setting, JsonElement element) {
      try {
         if (setting instanceof BooleanSetting boolSetting) {
            boolSetting.setValue(element.getAsBoolean());
         } else if (setting instanceof SliderSettings sliderSetting) {
            sliderSetting.setValue((float)element.getAsDouble());
         } else if (setting instanceof BindSetting bindSetting) {
            if (element.isJsonObject()) {
               JsonObject bindJson = element.getAsJsonObject();
               int key = bindJson.has("key") ? bindJson.get("key").getAsInt() : bindSetting.getKey();
               boolean mouse = bindJson.has("mouse") ? bindJson.get("mouse").getAsBoolean() : isLegacyMouseButton(key);
               bindSetting.setKeyBind(key, mouse);
               if (bindJson.has("type")) {
                  bindSetting.setType(bindJson.get("type").getAsInt());
               }
            } else {
               int key = element.getAsInt();
               bindSetting.setKeyBind(key, isLegacyMouseButton(key));
            }
         } else if (setting instanceof TextSetting textSetting) {
            textSetting.setValue(element.getAsString());
         } else if (setting instanceof SelectSetting selectSetting) {
            selectSetting.setValue(element.getAsString());
         } else if (setting instanceof ColorSetting colorSetting) {
            if (element.isJsonObject()) {
               JsonObject colorJson = element.getAsJsonObject();
               if (colorJson.has("hue")) {
                  colorSetting.setHue(colorJson.get("hue").getAsFloat());
               }

               if (colorJson.has("saturation")) {
                  colorSetting.setSaturation(colorJson.get("saturation").getAsFloat());
               }

               if (colorJson.has("brightness")) {
                  colorSetting.setBrightness(colorJson.get("brightness").getAsFloat());
               }

               if (colorJson.has("alpha")) {
                  colorSetting.setAlpha(colorJson.get("alpha").getAsFloat());
               }
            } else {
               colorSetting.setColor(element.getAsInt());
            }
         } else if (setting instanceof MultiSelectSetting multiSetting && element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            List<String> selected = new ArrayList<>();

            for (JsonElement e : array) {
               selected.add(e.getAsString());
            }

            multiSetting.setSelected(selected);
         }
      } catch (Exception var14) {
      }
   }

   private static boolean isLegacyMouseButton(int key) {
      return key >= 0 && key <= 7;
   }

   private ModuleRepository getModuleRepository() {
      Initialization instance = Initialization.getInstance();
      return instance != null && instance.getManager() != null ? instance.getManager().getModuleRepository() : null;
   }
}
