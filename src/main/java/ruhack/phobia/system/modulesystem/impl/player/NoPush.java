package ruhack.phobia.system.modulesystem.impl.player;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.PlayerCollisionEvent;
import ruhack.phobia.system.eventsystem.impl.PushEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;

public class NoPush extends ModuleStructure {
   MultiSelectSetting ignoreSetting = new MultiSelectSetting("Игнорировать", "").value("Вода", "Блоки", "Коллизию сущностей", "Рыхлый снег", "Ягоды");

   public NoPush() {
      super("AntiPush", "Убирает колизию от разных блоков/сущностей", ModuleCategory.PLAYER);
      this.settings(this.ignoreSetting);
   }

   @EventHandler
   public void onPush(PushEvent e) {
      switch (e.getType()) {
         case COLLISION:
            e.setCancelled(this.ignoreSetting.isSelected("Коллизию сущностей"));
            break;
         case WATER:
            e.setCancelled(this.ignoreSetting.isSelected("Вода"));
            break;
         case BLOCK:
            e.setCancelled(this.ignoreSetting.isSelected("Блоки"));
      }
   }

   @EventHandler
   public void onPlayerCollision(PlayerCollisionEvent e) {
      Block block = e.getBlock();
      if (block.equals(Blocks.POWDER_SNOW)) {
         e.setCancelled(this.ignoreSetting.isSelected("Рыхлый снег"));
      } else if (block.equals(Blocks.SWEET_BERRY_BUSH)) {
         e.setCancelled(this.ignoreSetting.isSelected("Ягоды"));
      }
   }
}
