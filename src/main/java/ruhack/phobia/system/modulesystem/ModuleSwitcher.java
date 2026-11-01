package ruhack.phobia.system.modulesystem;

import java.util.List;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.KeyEvent;

public class ModuleSwitcher implements IMinecraft {
   private final List<ModuleStructure> moduleStructures;

   public ModuleSwitcher(List<ModuleStructure> moduleStructures, EventManager eventManager) {
      this.moduleStructures = moduleStructures;
      EventManager.register(this);
   }

   @EventHandler
   public void onKey(KeyEvent event) {
      for (ModuleStructure moduleStructure : this.moduleStructures) {
         if (event.key() == moduleStructure.getKey() && mc.currentScreen == null) {
            try {
               this.handleModuleState(moduleStructure, event.action());
            } catch (Exception var5) {
            }
         }
      }
   }

   private void handleModuleState(ModuleStructure moduleStructure, int action) {
      if (moduleStructure.getType() == 1 && action == 1) {
         moduleStructure.switchState();
      }
   }
}
