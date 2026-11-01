package ruhack.phobia.system.modulesystem.impl.player;

import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;

public class NoDelay extends ModuleStructure {
   public MultiSelectSetting ignoreSetting = new MultiSelectSetting("Тип", "").value("Прыжок", "Правый клик", "Задержка ломания").selected("Прыжок");

   public NoDelay() {
      super("NoDelay", "Убирает задержку на разные действия", ModuleCategory.PLAYER);
      this.settings(this.ignoreSetting);
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null) {
         if (this.ignoreSetting.isSelected("Задержка ломания")) {
            mc.interactionManager.blockBreakingCooldown = 0;
         }

         if (this.ignoreSetting.isSelected("Прыжок")) {
            mc.player.jumpingCooldown = 0;
         }

         if (this.ignoreSetting.isSelected("Правый клик")) {
            mc.itemUseCooldown = 0;
         }
      }
   }
}
