package ruhack.phobia.system.configsystem.impl.friend;

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
import ruhack.phobia.system.configsystem.impl.consolelogger.Logger;
import ruhack.phobia.system.friendsystem.Friend;
import ruhack.phobia.system.friendsystem.FriendUtils;

public class FriendConfig {
   private static FriendConfig instance;
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private final Path configPath;

   private FriendConfig() {
      Path configDir = Paths.get("Phobia", "configs");

      try {
         Files.createDirectories(configDir);
      } catch (IOException var3) {
      }

      this.configPath = configDir.resolve("friends.file");
   }

   public static FriendConfig getInstance() {
      if (instance == null) {
         instance = new FriendConfig();
      }

      return instance;
   }

   public void save() {
      try {
         JsonArray array = new JsonArray();

         for (Friend friend : FriendUtils.getFriends()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", friend.getName());
            entry.addProperty("addedAt", friend.getAddedAt());
            array.add(entry);
         }

         Files.writeString(this.configPath, this.gson.toJson(array), StandardCharsets.UTF_8);
      } catch (IOException e) {
         Logger.error("FriendConfig: Save failed! " + e.getMessage());
      }
   }

   public void load() {
      try {
         if (!Files.exists(this.configPath)) {
            return;
         }

         String json = Files.readString(this.configPath, StandardCharsets.UTF_8);
         JsonArray array = JsonParser.parseString(json).getAsJsonArray();
         List<Friend> entries = new ArrayList<>();
         long migrationTime = System.currentTimeMillis();

         for (JsonElement element : array) {
            if (element.isJsonObject()) {
               JsonObject object = element.getAsJsonObject();
               String name = object.get("name").getAsString();
               long addedAt = object.has("addedAt") ? object.get("addedAt").getAsLong() : migrationTime;
               entries.add(new Friend(name, addedAt));
            } else {
               entries.add(new Friend(element.getAsString(), migrationTime));
            }
         }

         FriendUtils.setFriends(entries);
         this.save();
         Logger.success("FriendConfig: friends.file loaded successfully!");
      } catch (Exception e) {
         Logger.error("FriendConfig: Load failed! " + e.getMessage());
      }
   }
}
