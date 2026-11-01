package ruhack.phobia.util.istance;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.Generated;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.modulesystem.ModuleStructure;

public final class Instance {
   private static final ConcurrentMap<Class<? extends ModuleStructure>, ModuleStructure> instanceModules = new ConcurrentHashMap<>();

   public static <T extends ModuleStructure> T get(Class<T> clazz) {
      return clazz.cast(
         instanceModules.computeIfAbsent(
            clazz, instance -> Initialization.getInstance().getManager().getModuleProvider().get((Class<? extends ModuleStructure>)instance)
         )
      );
   }

   public static <T extends ModuleStructure> T get(String module) {
      return Initialization.getInstance().getManager().getModuleProvider().get(module);
   }

   @Generated
   private Instance() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
