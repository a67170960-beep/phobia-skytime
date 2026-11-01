package ruhack.phobia.system.modulesystem.impl.movement;

import lombok.Generated;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.util.player.MovingUtil;

public class NoFallDamage extends ModuleStructure {
   private final SelectSetting mode = new SelectSetting("Режим", "Выберите тип", "Flag", "Flag");

   public NoFallDamage() {
      super("NoFallDamage", "Убирает получение урона с высоты", ModuleCategory.MOVEMENT);
      this.settings(this.mode);
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (mc.player != null && mc.world != null) {
         if (mc.player.fallDistance > 0.0 && MovingUtil.getDistanceToGround() > 4.0) {
            mc.player.setVelocity(0.0, 0.0, 0.0);
         }
      }
   }

   @Generated
   public SelectSetting getMode() {
      return this.mode;
   }
}
