package ruhack.phobia.system.modulesystem.impl.movement;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.eventsystem.impl.UsingItemEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.util.istance.Instance;
import ruhack.phobia.util.player.swap.script.Script;
import ruhack.phobia.util.string.PlayerInteractionHelper;
import ruhack.phobia.util.timer.StopWatch;

public class NoSlowDown extends ModuleStructure {
   private final StopWatch notifWatch = new StopWatch();
   private final Script script = new Script();
   private boolean finish;
   public final SelectSetting itemMode = new SelectSetting(
      "Режим предмета", "Выберите режим обхода", "Grim Old", "Grim Old", "Grim New", "ReallyWorld", "SpookyTime", "FunTime"
   );
   private int ticks = 0;
   private int cycleCounter = 0;

   public static NoSlowDown getInstance() {
      return Instance.get(NoSlowDown.class);
   }

   public NoSlowDown() {
      super("NoSlowDown", "Ускоряет движение во время использования предметов", ModuleCategory.MOVEMENT);
      this.settings(this.itemMode);
   }

   private boolean isOnSnowOrCarpet() {
      if (mc.player != null && mc.world != null) {
         BlockPos playerPos = mc.player.getBlockPos();
         Block block = mc.world.getBlockState(playerPos).getBlock();
         return block == Blocks.SNOW
            || block == Blocks.WHITE_CARPET
            || block == Blocks.ORANGE_CARPET
            || block == Blocks.MAGENTA_CARPET
            || block == Blocks.LIGHT_BLUE_CARPET
            || block == Blocks.YELLOW_CARPET
            || block == Blocks.LIME_CARPET
            || block == Blocks.PINK_CARPET
            || block == Blocks.GRAY_CARPET
            || block == Blocks.LIGHT_GRAY_CARPET
            || block == Blocks.CYAN_CARPET
            || block == Blocks.PURPLE_CARPET
            || block == Blocks.BLUE_CARPET
            || block == Blocks.BROWN_CARPET
            || block == Blocks.GREEN_CARPET
            || block == Blocks.RED_CARPET
            || block == Blocks.BLACK_CARPET;
      } else {
         return false;
      }
   }

   @EventHandler
   public void onUpdate(TickEvent event) {
      if (mc.player != null) {
         if (!this.itemMode.isSelected("ReallyWorld") && !this.itemMode.isSelected("SpookyTime")) {
            if (mc.player.getActiveHand() != Hand.MAIN_HAND && mc.player.getActiveHand() != Hand.OFF_HAND) {
               this.ticks = 0;
            } else {
               this.ticks++;
            }
         } else if (!mc.player.isUsingRiptide()) {
            if (mc.player.isUsingItem()) {
               this.ticks++;
            } else {
               this.ticks = 0;
               this.cycleCounter = 0;
            }
         }
      }
   }

   @EventHandler
   public void onUsingItem(UsingItemEvent e) {
      if (mc.player != null) {
         Hand first = mc.player.getActiveHand();
         Hand second = first.equals(Hand.MAIN_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND;
         switch (e.getType()) {
            case 1:
               this.handleItemUse(e, first, second);
               break;
            case 2:
               while (!this.script.isFinished()) {
                  this.script.update();
               }
         }
      }
   }

   private void handleItemUse(UsingItemEvent e, Hand first, Hand second) {
      switch (this.itemMode.getValue()) {
         case "Grim Old":
            if (mc.player.getOffHandStack().getUseAction().equals(UseAction.NONE) || mc.player.getMainHandStack().getUseAction().equals(UseAction.NONE)) {
               PlayerInteractionHelper.interactItem(first);
               PlayerInteractionHelper.interactItem(second);
               e.cancel();
            }
            break;
         case "Grim New":
            if (mc.player.getItemUseTime() == 1 && mc.player.getActiveHand() == Hand.MAIN_HAND) {
               mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.DROP_ALL_ITEMS, BlockPos.ORIGIN, mc.player.getHorizontalFacing()));
            }

            if (mc.player.getItemUseTime() > 0) {
               e.cancel();
            }
            break;
         case "ReallyWorld":
            int[] thresholdsx;
            if (mc.player.isJumping()) {
               thresholdsx = new int[]{2, 2, 2};
            } else {
               thresholdsx = new int[]{2, 3, 3};
            }

            int thresholdx = thresholdsx[this.cycleCounter % thresholdsx.length];
            if (this.ticks >= thresholdx) {
               e.cancel();
               this.ticks = 0;
               this.cycleCounter++;
            }
            break;
         case "SpookyTime":
            int[] thresholds = new int[]{2, 2, 2};
            int threshold = thresholds[this.cycleCounter % 2];
            if (this.ticks >= threshold) {
               e.cancel();
               this.ticks = 0;
               this.cycleCounter++;
            }
            break;
         case "FunTime":
            if (this.ticks > 0.0F && mc.player.getItemUseTime() > 1.0F) {
               boolean mainHandCrossbow = mc.player.getMainHandStack().getItem() instanceof CrossbowItem;
               boolean offHandCrossbow = mc.player.getOffHandStack().getItem() instanceof CrossbowItem;
               if (mainHandCrossbow || offHandCrossbow) {
                  if (this.ticks > 0.0F && mc.player.getItemUseTime() > 1) {
                     e.cancel();
                     this.ticks = 0;
                  }
               } else if (mc.player.isOnGround() && this.isOnSnowOrCarpet()) {
                  mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, mc.player.getBlockPos().up(), Direction.DOWN));
                  mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z);
                  e.cancel();
                  this.ticks = 0;
               }
            }
      }
   }
}
