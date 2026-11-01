package ruhack.phobia.system.modulesystem;

import lombok.Generated;

public class ModuleBuilder {
   private final ModuleRepository repository;

   public ModuleBuilder add(ModuleStructure module) {
      this.repository.registerModule(module, false);
      return this;
   }

   public ModuleBuilder hidden(ModuleStructure module) {
      this.repository.registerModule(module, true);
      return this;
   }

   @Generated
   public ModuleBuilder(ModuleRepository repository) {
      this.repository = repository;
   }
}
