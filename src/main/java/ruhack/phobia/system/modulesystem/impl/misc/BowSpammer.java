package ruhack.phobia.system.modulesystem.impl.misc;

import lombok.Generated;
import net.minecraft.item.BowItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;

public class BowSpammer extends ModuleStructure {
   private final SliderSettings delay = new SliderSettings("Задержка", "Задержка между выстрелами", 2.5F).range(2.2F, 5.0F);

   public BowSpammer() {
      super("BowSpammer", "Bow Spammer", ModuleCategory.MISC);
      this.settings(this.delay);
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         if (mc.getNetworkHandler() != null) {
            if (this.canShoot()) {
               this.sendShootPackets();
               mc.player.stopUsingItem();
            }
         }
      }
   }

   private boolean canShoot() {
      if (!(mc.player.getMainHandStack().getItem() instanceof BowItem)) {
         return false;
      } else {
         return !mc.player.isUsingItem() ? false : mc.player.getItemUseTime() >= this.delay.getValue();
      }
   }

   private void sendShootPackets() {
      mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
      mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
   }

   @Generated
   public SliderSettings getDelay() {
      return this.delay;
   }
}
