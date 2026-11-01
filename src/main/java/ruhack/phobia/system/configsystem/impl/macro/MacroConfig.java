package ruhack.phobia.system.configsystem.impl.macro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import ruhack.phobia.system.configsystem.impl.consolelogger.Logger;
import ruhack.phobia.system.macrosystem.Macro;
import ruhack.phobia.system.macrosystem.MacroRepository;

public class MacroConfig {
   private static MacroConfig instance;
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private final Path configPath;

   private MacroConfig() {
      Path configDir = Paths.get("Phobia", "configs");

      try {
         Files.createDirectories(configDir);
      } catch (IOException var3) {
      }

      this.configPath = configDir.resolve("macros.file");
   }

   public static MacroConfig getInstance() {
      if (instance == null) {
         instance = new MacroConfig();
      }

      return instance;
   }

   public void save() {
      try {
         JsonArray array = new JsonArray();

         for (Macro macro : MacroRepository.getInstance().getMacroList()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", macro.name());
            obj.addProperty("message", macro.message());
            obj.addProperty("key", macro.key());
            array.add(obj);
         }

         Files.writeString(this.configPath, this.gson.toJson(array), StandardCharsets.UTF_8);
      } catch (IOException e) {
         Logger.error("MacroConfig: Save failed! " + e.getMessage());
      }
   }

   public void load() {
      try {
         if (!Files.exists(this.configPath)) {
            return;
         }

         String json = Files.readString(this.configPath, StandardCharsets.UTF_8);
         JsonArray array = JsonParser.parseString(json).getAsJsonArray();
         List<Macro> macros = new ArrayList<>();
         array.forEach(element -> {
            JsonObject obj = element.getAsJsonObject();
            String name = obj.get("name").getAsString();
            String message = obj.get("message").getAsString();
            int key = obj.get("key").getAsInt();
            macros.add(new Macro(name, message, key));
         });
         MacroRepository.getInstance().setMacros(macros);
         Logger.success("MacroConfig: macros.file loaded successfully!");
      } catch (Exception e) {
         Logger.error("MacroConfig: Load failed! " + e.getMessage());
      }
   }
}
