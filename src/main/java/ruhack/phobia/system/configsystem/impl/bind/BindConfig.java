package ruhack.phobia.system.configsystem.impl.bind;

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
import ruhack.phobia.system.configsystem.impl.consolelogger.Logger;

public class BindConfig {
   private static BindConfig instance;
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private final Path configPath;
   private int BindKey = 344;

   private BindConfig() {
      Path configDir = Paths.get("Phobia", "configs");

      try {
         Files.createDirectories(configDir);
      } catch (IOException var3) {
      }

      this.configPath = configDir.resolve("Bind.file");
      this.load();
   }

   public static BindConfig getInstance() {
      if (instance == null) {
         instance = new BindConfig();
      }

      return instance;
   }

   public void setKey(int key) {
      this.BindKey = key;
   }

   public void setKeyAndSave(int key) {
      this.setKey(key);
      this.save();
   }

   public void save() {
      try {
         JsonObject obj = new JsonObject();
         obj.addProperty("BindKey", this.BindKey);
         Files.writeString(this.configPath, this.gson.toJson(obj), StandardCharsets.UTF_8);
      } catch (IOException e) {
         Logger.error("BindConfig: Save failed! " + e.getMessage());
      }
   }

   public void load() {
      try {
         if (!Files.exists(this.configPath)) {
            return;
         }

         String json = Files.readString(this.configPath, StandardCharsets.UTF_8);
         JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
         if (obj.has("BindKey")) {
            this.BindKey = obj.get("BindKey").getAsInt();
         }

         Logger.success("BindConfig: Bind.file loaded successfully!");
      } catch (Exception e) {
         Logger.error("BindConfig: Load failed! " + e.getMessage());
      }
   }

   @Generated
   public int getBindKey() {
      return this.BindKey;
   }
}
