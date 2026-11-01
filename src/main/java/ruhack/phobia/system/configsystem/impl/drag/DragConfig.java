package ruhack.phobia.system.configsystem.impl.drag;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.configsystem.impl.consolelogger.Logger;
import ruhack.phobia.system.dragsystem.HudElement;
import ruhack.phobia.system.dragsystem.HudManager;

public class DragConfig {
   private static final int LAYOUT_VERSION = 2;
   private static DragConfig instance;
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private final Path configPath;

   private DragConfig() {
      Path configDir = Paths.get("Phobia", "configs");

      try {
         Files.createDirectories(configDir);
      } catch (IOException var3) {
      }

      this.configPath = configDir.resolve("draggables.file");
   }

   public static DragConfig getInstance() {
      if (instance == null) {
         instance = new DragConfig();
      }

      return instance;
   }

   public void save() {
      try {
         HudManager hudManager = this.getHudManager();
         if (hudManager == null || !hudManager.isInitialized()) {
            return;
         }

         JsonObject root = new JsonObject();
         root.addProperty("_layoutVersion", 2);

         for (HudElement element : hudManager.getElements()) {
            JsonObject elementJson = new JsonObject();
            elementJson.addProperty("x", element.getX());
            elementJson.addProperty("y", element.getY());
            elementJson.addProperty("width", element.getWidth());
            elementJson.addProperty("height", element.getHeight());
            root.add(element.getName(), elementJson);
         }

         Files.writeString(this.configPath, this.gson.toJson(root), StandardCharsets.UTF_8);
         Logger.success("DragConfig: draggables.json saved successfully!");
      } catch (IOException e) {
         Logger.error("DragConfig: Save failed! " + e.getMessage());
      }
   }

   public void load() {
      try {
         if (!Files.exists(this.configPath)) {
            Logger.info("DragConfig: No config file found, using defaults.");
            return;
         }

         HudManager hudManager = this.getHudManager();
         if (hudManager == null) {
            Logger.error("DragConfig: HudManager is null, cannot load.");
            return;
         }

         String json = Files.readString(this.configPath, StandardCharsets.UTF_8);
         if (json == null || json.trim().isEmpty()) {
            Logger.error("DragConfig: Config file is empty.");
            return;
         }

         JsonObject root = JsonParser.parseString(json).getAsJsonObject();
         int layoutVersion = root.has("_layoutVersion") ? root.get("_layoutVersion").getAsInt() : 1;

         for (HudElement element : hudManager.getElements()) {
            if (root.has(element.getName())) {
               JsonObject elementJson = root.getAsJsonObject(element.getName());
               if (element.getName().equals("ArrayList") && layoutVersion < 2) {
                  element.setX(6);
                  element.setY(56);
               } else {
                  if (elementJson.has("x")) {
                     element.setX(elementJson.get("x").getAsInt());
                  }

                  if (elementJson.has("y")) {
                     element.setY(elementJson.get("y").getAsInt());
                  }

                  if (elementJson.has("width")) {
                     element.setWidth(elementJson.get("width").getAsInt());
                  }

                  if (elementJson.has("height")) {
                     element.setHeight(elementJson.get("height").getAsInt());
                  }
               }
            }
         }

         Logger.success("DragConfig: draggables.file loaded successfully!");
      } catch (Exception e) {
         Logger.error("DragConfig: Load failed! " + e.getMessage());
      }
   }

   private HudManager getHudManager() {
      if (Initialization.getInstance() == null) {
         return null;
      } else {
         return Initialization.getInstance().getManager() == null ? null : Initialization.getInstance().getManager().getHudManager();
      }
   }
}
