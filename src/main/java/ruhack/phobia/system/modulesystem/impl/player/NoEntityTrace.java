package ruhack.phobia.system.modulesystem.impl.player;

import net.minecraft.item.ItemStack;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.util.istance.Instance;

public class NoEntityTrace extends ModuleStructure {
   private final BooleanSetting noSword = new BooleanSetting("Выключать с мечом", "Не дает бить сквозь сущность если в руке меч").setValue(true);

   public NoEntityTrace() {
      super("NoEntityTrace", "No Entity Trace", ModuleCategory.PLAYER);
      this.settings(this.noSword);
   }

   public static NoEntityTrace getInstance() {
      return Instance.get(NoEntityTrace.class);
   }

   public boolean shouldIgnoreEntityTrace() {
      if (!this.isState() || mc.player == null) {
         return false;
      }

      if (!this.noSword.isValue()) {
         return true;
      }

      ItemStack stack = mc.player.getMainHandStack();
      String key = stack.getItem().getTranslationKey().toLowerCase();
      return !key.contains("sword");
   }
}
