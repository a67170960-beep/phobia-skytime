package ruhack.phobia.system.modulesystem.impl.rage;

import com.adl.nativeprotect.Native;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.friendsystem.FriendUtils;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.util.player.swap.InventoryUtils;

public class AutoExplosion extends ModuleStructure {
   private final BooleanSetting placeOnRightClick = new BooleanSetting("Ставить по ПКМ", "Автоматически ставить кристалл").setValue(true);
   private final MultiSelectSetting protect = new MultiSelectSetting("Не взрывать", "Защита окружения").value("Ресурсы", "Друзей").selected("Друзей");
   private BlockPos pending;
   private boolean useWasPressed;

   public AutoExplosion() {
      super("AutoExplosion", "Автоматически ставит и взрывает кристалл", ModuleCategory.RAGE);
      this.settings(this.placeOnRightClick, this.protect);
   }

   @EventHandler
   @Native
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         boolean pressed = mc.options.useKey.isPressed();
         if (this.placeOnRightClick.isValue()
            && pressed
            && !this.useWasPressed
            && mc.crosshairTarget instanceof BlockHitResult hit
            && hit.getType() == Type.BLOCK
            && this.canPlace(hit.getBlockPos())
            && !this.isProtected(hit.getBlockPos())) {
            int slot = InventoryUtils.findItemInHotbar(Items.END_CRYSTAL);
            if (slot != -1) {
               int old = InventoryUtils.currentSlot();
               InventoryUtils.selectSlotSilent(slot);
               mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
               InventoryUtils.selectSlotSilent(old);
               this.pending = hit.getBlockPos();
            }
         }

         this.useWasPressed = pressed;
         if (this.pending != null) {
            Box area = new Box(this.pending.up()).expand(1.0);

            for (Entity entity : mc.world.getEntities()) {
               if (entity instanceof EndCrystalEntity crystal && area.intersects(crystal.getBoundingBox())) {
                  if (!this.isProtected(this.pending)) {
                     mc.interactionManager.attackEntity(mc.player, crystal);
                     mc.player.swingHand(Hand.MAIN_HAND);
                  }

                  this.pending = null;
                  break;
               }
            }
         }
      }
   }

   private boolean canPlace(BlockPos pos) {
      return (mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) || mc.world.getBlockState(pos).isOf(Blocks.BEDROCK))
         && mc.world.getBlockState(pos.up()).isAir();
   }

   private boolean isProtected(BlockPos pos) {
      Box area = new Box(pos.up()).expand(6.0);
      if (this.protect.isSelected("Друзей")
         && mc.world.getPlayers().stream().anyMatch(player -> player != mc.player && FriendUtils.isFriend(player) && area.intersects(player.getBoundingBox()))) {
         return true;
      }

      if (this.protect.isSelected("Ресурсы")) {
         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemEntity item && area.intersects(item.getBoundingBox()) && valuable(item.getStack())) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean valuable(ItemStack stack) {
      return stack.isOf(Items.TOTEM_OF_UNDYING)
         || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)
         || stack.isOf(Items.NETHER_STAR)
         || stack.isOf(Items.ELYTRA)
         || stack.isOf(Items.NETHERITE_INGOT)
         || stack.isOf(Items.NETHERITE_SCRAP)
         || stack.isOf(Items.ANCIENT_DEBRIS)
         || stack.isOf(Items.PLAYER_HEAD);
   }
}
