package ruhack.phobia.system.configsystem.impl.prefix;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.Generated;
import ruhack.phobia.system.commandsystem.CommandManager;
import ruhack.phobia.system.configsystem.impl.consolelogger.Logger;

public class PrefixConfig {
   private static PrefixConfig instance;
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private final Path configPath;
   private String prefix = ".";

   private PrefixConfig() {
      Path configDir = Paths.get("Phobia", "configs");

      try {
         Files.createDirectories(configDir);
      } catch (IOException var3) {
      }

      this.configPath = configDir.resolve("prefix.file");
   }

   public static PrefixConfig getInstance() {
      if (instance == null) {
         instance = new PrefixConfig();
      }

      return instance;
   }

   public void setPrefix(String prefix) {
      this.prefix = prefix;
      if (CommandManager.getInstance() != null) {
         CommandManager.getInstance().setPrefix(prefix);
      }
   }

   public void setPrefixAndSave(String prefix) {
      this.setPrefix(prefix);
      this.save();
   }

   public void save() {
      try {
         JsonObject obj = new JsonObject();
         obj.addProperty("prefix", this.prefix);
         Files.writeString(this.configPath, this.gson.toJson(obj), StandardCharsets.UTF_8);
      } catch (IOException e) {
         Logger.error("PrefixConfig: Save failed! " + e.getMessage());
      }
   }

   public void load() {
      try {
         if (!Files.exists(this.configPath)) {
            return;
         }

         String json = Files.readString(this.configPath, StandardCharsets.UTF_8);
         JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
         if (obj.has("prefix")) {
            String loadedPrefix = obj.get("prefix").getAsString();
            if (!loadedPrefix.isEmpty()) {
               this.prefix = loadedPrefix;
            }
         }

         Logger.success("PrefixConfig: prefix.file loaded successfully!");
      } catch (Exception e) {
         Logger.error("PrefixConfig: Load failed! " + e.getMessage());
      }
   }

   @Generated
   public String getPrefix() {
      return this.prefix;
   }
}
