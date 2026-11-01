package ruhack.phobia.system.modulesystem.impl.player;

import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.util.istance.Instance;

public class NoInteract extends ModuleStructure {
   public static NoInteract getInstance() {
      return Instance.get(NoInteract.class);
   }

   public NoInteract() {
      super("NoInteract", "Не дает взаимодействовать с блоками", ModuleCategory.PLAYER);
   }
}
