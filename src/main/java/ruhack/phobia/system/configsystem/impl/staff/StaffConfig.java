package ruhack.phobia.system.configsystem.impl.staff;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import ruhack.phobia.system.commandsystem.staffsystem.Staff;
import ruhack.phobia.system.commandsystem.staffsystem.StaffUtils;
import ruhack.phobia.system.configsystem.impl.consolelogger.Logger;

public class StaffConfig {
   private static StaffConfig instance;
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private final Path configPath;

   private StaffConfig() {
      Path configDir = Paths.get("Phobia", "configs");

      try {
         Files.createDirectories(configDir);
      } catch (IOException var3) {
      }

      this.configPath = configDir.resolve("staff.file");
   }

   public static StaffConfig getInstance() {
      if (instance == null) {
         instance = new StaffConfig();
      }

      return instance;
   }

   public void save() {
      try {
         JsonArray array = new JsonArray();

         for (Staff staff : StaffUtils.getStaffList()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", staff.name());
            entry.addProperty("addedAt", staff.addedAt());
            array.add(entry);
         }

         Files.writeString(this.configPath, this.gson.toJson(array), StandardCharsets.UTF_8);
      } catch (IOException e) {
         Logger.error("StaffConfig: Save failed! " + e.getMessage());
      }
   }

   public void load() {
      try {
         if (!Files.exists(this.configPath)) {
            return;
         }

         String json = Files.readString(this.configPath, StandardCharsets.UTF_8);
         JsonArray array = JsonParser.parseString(json).getAsJsonArray();
         List<Staff> entries = new ArrayList<>();
         long migrationTime = System.currentTimeMillis();

         for (JsonElement element : array) {
            if (element.isJsonObject()) {
               JsonObject object = element.getAsJsonObject();
               String name = object.get("name").getAsString();
               long addedAt = object.has("addedAt") ? object.get("addedAt").getAsLong() : migrationTime;
               entries.add(new Staff(name, addedAt));
            } else {
               entries.add(new Staff(element.getAsString(), migrationTime));
            }
         }

         StaffUtils.setStaff(entries);
         this.save();
         Logger.success("StaffConfig: staff.file loaded successfully!");
      } catch (Exception e) {
         Logger.error("StaffConfig: Load failed! " + e.getMessage());
      }
   }
}
