package ruhack.phobia.system.modulesystem.impl.rage;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.target.TargetSelector;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.istance.Instance;
import ruhack.phobia.util.player.swap.InventoryUtils;

public class WebTrap extends ModuleStructure {
   public final SliderSettings range = new SliderSettings("Дистанция", "Максимальная дистанция до цели", 5.0F).range(2.0F, 6.0F).step(0.5F);
   public final SelectSetting swapMode = new SelectSetting("Свап", "Способ переключения на паутину", "Silent", "Silent", "Legit");
   public final BooleanSetting headWeb = new BooleanSetting("И на голову", "Ставить вторую паутину на уровень головы").setValue(false);
   public final BooleanSetting selfWeb = new BooleanSetting("Под себя если нет цели", "Если цели нет — ставить паутину под себя").setValue(false);
   public final SliderSettings leadTicks = new SliderSettings("Упреждение", "На сколько тиков предсказывать движение цели", 2.0F).range(0.0F, 5.0F).step(1.0F);
   private final TargetSelector targetSelector = new TargetSelector();
   private static final int STAGE_SWAP = 0;
   private static final int STAGE_PLACE = 1;
   private static final int STAGE_RESTORE = 2;
   private int stage;
   private boolean swapped;
   private boolean silentSwap;
   private LivingEntity target;

   public static WebTrap getInstance() {
      return Instance.get(WebTrap.class);
   }

   public WebTrap() {
      super("WebTrap", "Ставит паутину в цель по бинду", ModuleCategory.RAGE);
      this.settings(this.range, this.swapMode, this.headWeb, this.selfWeb, this.leadTicks);
   }

   @Override
   public void activate() {
      this.stage = 0;
      this.swapped = false;
      this.target = null;
   }

   @Override
   public void deactivate() {
      if (this.swapped) {
         this.restoreSlot();
      }

      this.target = null;
      this.targetSelector.releaseTarget();
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         switch (this.stage) {
            case 0:
               this.target = this.findTarget();
               if (this.target == null && !this.selfWeb.isValue()) {
                  this.setState(false);
                  return;
               }

               int slot = InventoryUtils.findHotbarItem(Items.COBWEB);
               if (slot == -1) {
                  this.setState(false);
                  return;
               }

               if (mc.player.getInventory().getSelectedSlot() != slot) {
                  InventoryUtils.saveSlot();
                  this.silentSwap = this.swapMode.isSelected("Silent");
                  if (this.silentSwap) {
                     InventoryUtils.selectSlotSilent(slot);
                  } else {
                     InventoryUtils.selectSlot(slot);
                  }

                  this.swapped = true;
               }

               this.stage = 1;
               break;
            case 1:
               BlockPos feet = this.target != null ? this.webPos(this.target) : mc.player.getBlockPos();
               this.placeWeb(feet);
               if (this.headWeb.isValue()) {
                  this.placeWeb(feet.up());
               }

               this.stage = 2;
               break;
            case 2:
               if (this.swapped) {
                  this.restoreSlot();
                  this.swapped = false;
               }

               this.setState(false);
         }
      } else {
         this.setState(false);
      }
   }

   private LivingEntity findTarget() {
      TargetSelector.EntityFilter filter = new TargetSelector.EntityFilter(List.of("Игроки"));
      this.targetSelector.searchTargets(mc.world.getEntities(), this.range.getValue(), 360.0F, true);
      this.targetSelector.validateTarget(filter::isValid);
      return this.targetSelector.getCurrentTarget();
   }

   private BlockPos webPos(LivingEntity target) {
      float lead = this.leadTicks.getValue();
      double dx = (target.getX() - target.lastX) * lead;
      double dz = (target.getZ() - target.lastZ) * lead;
      Vec3d base = target.getEntityPos().add(dx, 0.0, dz);
      BlockPos pos = BlockPos.ofFloored(base.x, base.y + 0.05, base.z);

      for (int i = 0; i < 2 && !mc.world.getBlockState(pos).isReplaceable(); i++) {
         pos = pos.up();
      }

      for (int drop = 0; drop < 4 && mc.world.getBlockState(pos).isReplaceable() && mc.world.getBlockState(pos.down()).isReplaceable(); drop++) {
         pos = pos.down();
      }

      return pos;
   }

   private boolean placeWeb(BlockPos pos) {
      if (!mc.world.getBlockState(pos).isReplaceable()) {
         return false;
      }

      for (Direction dir : Direction.values()) {
         BlockPos support = pos.offset(dir);
         BlockState state = mc.world.getBlockState(support);
         if (!state.isAir() && !state.isReplaceable()) {
            Vec3d hitVec = Vec3d.ofCenter(support).add(Vec3d.of(dir.getOpposite().getVector()).multiply(0.5));
            BlockHitResult hit = new BlockHitResult(hitVec, dir.getOpposite(), support, false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            mc.player.swingHand(Hand.MAIN_HAND);
            return true;
         }
      }

      return false;
   }

   private void restoreSlot() {
      if (this.silentSwap) {
         InventoryUtils.restoreSlotSilent();
      } else {
         InventoryUtils.restoreSlot();
      }
   }
}
