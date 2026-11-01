package ruhack.phobia.system.macrosystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import ruhack.phobia.system.configsystem.impl.macro.MacroConfig;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.KeyEvent;

public class MacroRepository {
   private static MacroRepository instance;
   private final List<Macro> macroList = new ArrayList<>();
   private final MinecraftClient mc = MinecraftClient.getInstance();

   public MacroRepository() {
      instance = this;
   }

   public static MacroRepository getInstance() {
      if (instance == null) {
         instance = new MacroRepository();
      }

      return instance;
   }

   public void init() {
      EventManager.register(this);
      MacroConfig.getInstance().load();
   }

   public void addMacro(String name, String message, int key) {
      this.macroList.add(new Macro(name, message, key));
   }

   public void addMacroAndSave(String name, String message, int key) {
      this.addMacro(name, message, key);
      MacroConfig.getInstance().save();
   }

   public boolean hasMacro(String name) {
      return this.macroList.stream().anyMatch(macro -> macro.name().equalsIgnoreCase(name));
   }

   public Optional<Macro> getMacro(String name) {
      return this.macroList.stream().filter(macro -> macro.name().equalsIgnoreCase(name)).findFirst();
   }

   public void deleteMacro(String name) {
      this.macroList.removeIf(macro -> macro.name().equalsIgnoreCase(name));
   }

   public void deleteMacroAndSave(String name) {
      this.deleteMacro(name);
      MacroConfig.getInstance().save();
   }

   public void clearList() {
      this.macroList.clear();
   }

   public void clearListAndSave() {
      this.clearList();
      MacroConfig.getInstance().save();
   }

   public int size() {
      return this.macroList.size();
   }

   public List<String> getMacroNames() {
      return this.macroList.stream().map(Macro::name).collect(Collectors.toList());
   }

   public void setMacros(List<Macro> macros) {
      this.macroList.clear();
      this.macroList.addAll(macros);
   }

   @EventHandler
   public void onKey(KeyEvent event) {
      if (this.mc.player != null && this.mc.currentScreen == null) {
         if (event.action() == 1) {
            this.macroList.stream().filter(macro -> macro.key() == event.key()).findFirst().ifPresent(macro -> {
               String message = macro.message();
               if (message.startsWith("/")) {
                  this.mc.player.networkHandler.sendChatCommand(message.substring(1));
               } else {
                  this.mc.player.networkHandler.sendChatMessage(message);
               }
            });
         }
      }
   }

   @Generated
   public List<Macro> getMacroList() {
      return this.macroList;
   }

   @Generated
   public MinecraftClient getMc() {
      return this.mc;
   }
}
