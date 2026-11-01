package ruhack.phobia.system.modulesystem.impl.rage;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
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
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic.LinearSmoothMode;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.istance.Instance;
import ruhack.phobia.util.math.TaskPriority;
import ruhack.phobia.util.player.swap.InventoryUtils;
import ruhack.phobia.util.rotation.RotationConfig;
import ruhack.phobia.util.rotation.RotationController;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;
import ruhack.phobia.util.string.chat.ChatMessage;

public class AutoCart extends ModuleStructure {
   private static final double BLOCK_REACH = 4.2;
   private static final double ENTITY_REACH = 2.9;
   private static final int SEARCH_RADIUS = 5;
   public final BooleanSetting ignite = new BooleanSetting("Поджигать", "Взрывать вагонетку после установки").setValue(true);
   public final SelectSetting igniteMode = new SelectSetting("Поджиг", "Способ детонации", "Лук", "Лук", "Огниво").visible(() -> this.ignite.isValue());
   public final SliderSettings drawTicks = new SliderSettings("Натяжка", "Сколько тиков натягивать лук перед выстрелом", 20.0F)
      .range(5.0F, 25.0F)
      .step(1.0F)
      .visible(() -> this.ignite.isValue() && this.igniteMode.isSelected("Лук"));
   public final SelectSetting swapMode = new SelectSetting("Свап", "Способ переключения предметов", "Silent", "Silent", "Legit");
   public final BooleanSetting debugMessages = new BooleanSetting("Сообщения", "Писать в чат почему модуль не сработал").setValue(true);
   private static final int STAGE_FIND = 0;
   private static final int STAGE_SWAP_CART = 1;
   private static final int STAGE_PLACE_CART = 2;
   private static final int STAGE_SWAP_IGNITER = 3;
   private static final int STAGE_FLINT_IGNITE = 4;
   private static final int STAGE_BOW_DRAW = 5;
   private static final int STAGE_RESTORE = 6;
   private int stage;
   private int waitTicks;
   private int drawCounter;
   private boolean swapped;
   private boolean silentSwap;
   private BlockPos railPos;

   public static AutoCart getInstance() {
      return Instance.get(AutoCart.class);
   }

   public AutoCart() {
      super("AutoCart", "Ставит и взрывает ТНТ-вагонетку на ближайшей рельсе", ModuleCategory.RAGE);
      this.settings(this.ignite, this.igniteMode, this.drawTicks, this.swapMode, this.debugMessages);
   }

   @Override
   public void activate() {
      this.stage = 0;
      this.waitTicks = 0;
      this.drawCounter = 0;
      this.swapped = false;
      this.railPos = null;
   }

   @Override
   public void deactivate() {
      if (this.stage == 5) {
         mc.options.useKey.setPressed(false);
      }

      if (this.swapped) {
         this.restoreSlot();
         this.swapped = false;
      }

      this.railPos = null;
   }

   private boolean isBowMode() {
      return this.igniteMode.isSelected("Лук");
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         Entity cart;
         switch (this.stage) {
            case 0:
               this.railPos = this.findNearestRail();
               if (this.railPos == null) {
                  this.abort("рельсы в радиусе 4.2 не найдено");
                  return;
               }

               if (InventoryUtils.findHotbarItem(Items.TNT_MINECART) == -1 && this.findCart() == null) {
                  this.abort("нет ТНТ-вагонетки в хотбаре");
                  return;
               }

               if (this.ignite.isValue()) {
                  if (this.isBowMode()) {
                     if (InventoryUtils.findHotbarItem(Items.BOW) == -1) {
                        this.abort("нет лука в хотбаре");
                        return;
                     }

                     if (!mc.player.getAbilities().creativeMode && !this.hasArrows()) {
                        this.abort("нет стрел");
                        return;
                     }
                  } else if (InventoryUtils.findHotbarItem(Items.FLINT_AND_STEEL) == -1) {
                     this.abort("нет огнива в хотбаре");
                     return;
                  }
               }

               this.silentSwap = this.swapMode.isSelected("Silent") && (!this.ignite.isValue() || !this.isBowMode());
               this.waitTicks = 0;
               this.stage = this.findCart() != null ? (this.ignite.isValue() ? 3 : 6) : 1;
               break;
            case 1:
               if (this.swapTo(InventoryUtils.findHotbarItem(Items.TNT_MINECART))) {
                  this.stage = 2;
               }
               break;
            case 2:
               Vec3d hitVec = Vec3d.ofCenter(this.railPos);
               mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hitVec, Direction.UP, this.railPos, false));
               mc.player.swingHand(Hand.MAIN_HAND);
               this.waitTicks = 0;
               this.stage = this.ignite.isValue() ? 3 : 6;
               break;
            case 3:
               cart = this.findCart();
               if (cart == null) {
                  if (++this.waitTicks > 8) {
                     this.abort("вагонетка не появилась");
                  }

                  return;
               }

               if (this.isBowMode()) {
                  if (this.swapTo(InventoryUtils.findHotbarItem(Items.BOW))) {
                     this.drawCounter = 0;
                     this.stage = 5;
                  }
               } else {
                  if (mc.player.getEyePos().distanceTo(cart.getEntityPos()) > 2.9) {
                     this.abort("вагонетка дальше 2.9 — возьми лук или подойди");
                     return;
                  }

                  if (this.swapTo(InventoryUtils.findHotbarItem(Items.FLINT_AND_STEEL))) {
                     this.stage = 4;
                  }
               }
               break;
            case 4:
               cart = this.findCart();
               if (cart != null) {
                  mc.interactionManager.interactEntity(mc.player, cart, Hand.MAIN_HAND);
                  mc.player.swingHand(Hand.MAIN_HAND);
               }

               this.stage = 6;
               break;
            case 5:
               cart = this.findCart();
               if (cart == null) {
                  mc.options.useKey.setPressed(false);
                  this.abort("вагонетка пропала до выстрела");
                  return;
               }

               this.aimAt(cart);
               mc.options.useKey.setPressed(true);
               if (++this.drawCounter >= (int)this.drawTicks.getValue()) {
                  mc.options.useKey.setPressed(false);
                  this.stage = 6;
               }
               break;
            case 6:
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

   private void aimAt(Entity cart) {
      Vec3d point = cart.getEntityPos().add(0.0, cart.getHeight() * 0.5, 0.0);
      Angle angle = AngleUtil.fromVec3d(point.subtract(mc.player.getEyePos()));
      RotationConfig config = new RotationConfig(new LinearSmoothMode(), true, false, false);
      RotationController.INSTANCE.rotateTo(angle, 1, config, TaskPriority.HIGH_IMPORTANCE_1, this);
   }

   private boolean hasArrows() {
      return InventoryUtils.findItemAnywhere(Items.ARROW) != -1
         || InventoryUtils.findItemAnywhere(Items.TIPPED_ARROW) != -1
         || InventoryUtils.findItemAnywhere(Items.SPECTRAL_ARROW) != -1;
   }

   private void abort(String reason) {
      if (this.debugMessages.isValue()) {
         ChatMessage.brandmessage("AutoCart: " + reason);
      }

      if (this.swapped) {
         this.restoreSlot();
         this.swapped = false;
      }

      this.setState(false);
   }

   private BlockPos findNearestRail() {
      BlockPos center = mc.player.getBlockPos();
      Vec3d eyes = mc.player.getEyePos();
      BlockPos best = null;
      double bestDist = Double.MAX_VALUE;

      for (BlockPos pos : BlockPos.iterate(center.add(-5, -5, -5), center.add(5, 5, 5))) {
         if (this.isRail(mc.world.getBlockState(pos))) {
            double dist = eyes.distanceTo(Vec3d.ofCenter(pos));
            if (dist <= 4.2 && dist < bestDist) {
               best = pos.toImmutable();
               bestDist = dist;
            }
         }
      }

      return best;
   }

   private boolean isRail(BlockState state) {
      return state.getBlock() instanceof AbstractRailBlock;
   }

   private Entity findCart() {
      if (this.railPos == null) {
         return null;
      }

      Vec3d center = Vec3d.ofCenter(this.railPos);

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof TntMinecartEntity && entity.isAlive() && entity.getEntityPos().distanceTo(center) < 2.0) {
            return entity;
         }
      }

      return null;
   }

   private boolean swapTo(int slot) {
      if (slot == -1) {
         this.abort("предмет пропал из хотбара");
         return false;
      }

      if (mc.player.getInventory().getSelectedSlot() == slot) {
         return true;
      }

      if (!this.swapped) {
         InventoryUtils.saveSlot();
         this.swapped = true;
      }

      if (this.silentSwap) {
         InventoryUtils.selectSlotSilent(slot);
      } else {
         InventoryUtils.selectSlot(slot);
      }

      return true;
   }

   private void restoreSlot() {
      if (this.silentSwap) {
         InventoryUtils.restoreSlotSilent();
      } else {
         InventoryUtils.restoreSlot();
      }
   }
}
