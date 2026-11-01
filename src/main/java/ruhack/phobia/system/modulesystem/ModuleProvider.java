package ruhack.phobia.system.modulesystem;

import java.util.List;
import lombok.Generated;

public class ModuleProvider {
   private final List<ModuleStructure> moduleStructures;

   public <T extends ModuleStructure> T get(String name) {
      return this.moduleStructures.stream().filter(module -> module.getName().equalsIgnoreCase(name)).map(module -> (T)module).findFirst().orElse(null);
   }

   public <T extends ModuleStructure> T get(Class<T> clazz) {
      return this.moduleStructures.stream().filter(module -> clazz.isAssignableFrom(module.getClass())).map(clazz::cast).findFirst().orElse(null);
   }

   @Generated
   public List<ModuleStructure> getModuleStructures() {
      return this.moduleStructures;
   }

   @Generated
   public ModuleProvider(List<ModuleStructure> moduleStructures) {
      this.moduleStructures = moduleStructures;
   }
}
