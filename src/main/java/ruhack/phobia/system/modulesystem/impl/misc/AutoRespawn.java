package ruhack.phobia.system.modulesystem.impl.misc;

import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.DeathScreenEvent;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;

public class AutoRespawn extends ModuleStructure {
   private final SelectSetting modeSetting = new SelectSetting("Режим", "Выберите, что будет использоваться", "Vanilla", "Vanilla");

   public AutoRespawn() {
      super("AutoRespawn", "Автоматически возраждается", ModuleCategory.MISC);
      this.settings(this.modeSetting);
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
   }

   @EventHandler
   public void onDeathScreen(DeathScreenEvent e) {
      if (this.modeSetting.isSelected("Vanilla")) {
         mc.player.requestRespawn();
         mc.setScreen(null);
      }
   }
}
