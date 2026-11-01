package ruhack.phobia.system.configsystem.impl.blockesp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import ruhack.phobia.system.configsystem.impl.consolelogger.Logger;

public class BlockESPConfig {
   private static BlockESPConfig instance;
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private final Path configPath;
   private final Set<String> blocks = new CopyOnWriteArraySet<>();

   private BlockESPConfig() {
      Path configDir = Paths.get("Phobia", "configs");

      try {
         Files.createDirectories(configDir);
      } catch (IOException var3) {
      }

      this.configPath = configDir.resolve("blockesp.file");
   }

   public static BlockESPConfig getInstance() {
      if (instance == null) {
         instance = new BlockESPConfig();
      }

      return instance;
   }

   public Set<String> getBlocks() {
      return this.blocks;
   }

   public void addBlock(String block) {
      this.blocks.add(block);
   }

   public void addBlockAndSave(String block) {
      this.addBlock(block);
      this.save();
   }

   public boolean removeBlock(String block) {
      return this.blocks.remove(block);
   }

   public boolean removeBlockAndSave(String block) {
      boolean removed = this.removeBlock(block);
      if (removed) {
         this.save();
      }

      return removed;
   }

   public boolean hasBlock(String block) {
      return this.blocks.contains(block);
   }

   public void clear() {
      this.blocks.clear();
   }

   public void clearAndSave() {
      this.clear();
      this.save();
   }

   public int size() {
      return this.blocks.size();
   }

   public List<String> getBlockList() {
      return new ArrayList<>(this.blocks);
   }

   public void save() {
      try {
         JsonArray array = new JsonArray();

         for (String block : this.blocks) {
            array.add(block);
         }

         Files.writeString(this.configPath, this.gson.toJson(array), StandardCharsets.UTF_8);
      } catch (IOException e) {
         Logger.error("BlockESPConfig: Save failed! " + e.getMessage());
      }
   }

   public void load() {
      try {
         if (!Files.exists(this.configPath)) {
            return;
         }

         String json = Files.readString(this.configPath, StandardCharsets.UTF_8);
         JsonArray array = JsonParser.parseString(json).getAsJsonArray();
         this.blocks.clear();
         array.forEach(element -> this.blocks.add(element.getAsString()));
         Logger.success("BlockESPConfig: blockesp.file loaded successfully!");
      } catch (Exception e) {
         Logger.error("BlockESPConfig: Load failed! " + e.getMessage());
      }
   }
}
