package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import ruhack.phobia.system.eventsystem.api.events.Event;
import ruhack.phobia.system.modulesystem.ModuleStructure;

public class ModuleToggleEvent implements Event {
   private final ModuleStructure module;
   private final boolean enabled;

   @Generated
   public ModuleStructure getModule() {
      return this.module;
   }

   @Generated
   public boolean isEnabled() {
      return this.enabled;
   }

   @Generated
   public ModuleToggleEvent(ModuleStructure module, boolean enabled) {
      this.module = module;
      this.enabled = enabled;
   }
}
