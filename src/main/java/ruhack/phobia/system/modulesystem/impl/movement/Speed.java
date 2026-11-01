package ruhack.phobia.system.modulesystem.impl.movement;

import com.adl.nativeprotect.Native;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.PlayerTravelEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.eventsystem.impl.UsingItemEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.math.TaskPriority;
import ruhack.phobia.util.player.MovingUtil;
import ruhack.phobia.util.player.swap.InventoryUtils;
import ruhack.phobia.util.rotation.RotationConfig;
import ruhack.phobia.util.rotation.RotationController;
import ruhack.phobia.util.rotation.angle.Angle;

public class Speed extends ModuleStructure {
   private final SelectSetting mode = new SelectSetting(
      "Режим", "Выберите режим скорости", "Vanilla", "Vanilla", "Grim", "FunTime", "Funtime Elytra", "HolyWorld"
   );
   private final SliderSettings speed = new SliderSettings("Скорость", "Настройка скорости передвижения", 1.5F)
      .range(1.0F, 5.0F)
      .visible(() -> this.mode.isSelected("Vanilla"));
   private final BooleanSetting diving = new BooleanSetting("Пикирование", "Наклоняет серверную ротацию вниз во время работы режима")
      .visible(() -> this.mode.isSelected("Funtime Elytra"));

   public Speed() {
      super("Speed", "Ускоряет игрока", ModuleCategory.MOVEMENT);
      this.settings(this.mode, this.speed, this.diving);
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (this.mode.isSelected("Vanilla")) {
         MovingUtil.setVelocity(this.speed.getValue() / 3.0F);
      }

      if (this.mode.isSelected("Funtime Elytra")) {
         this.handleFuntimeElytraMode();
      }
   }

   @EventHandler
   public void onMotion(PlayerTravelEvent e) {
      if (this.mode.isSelected("FunTime")) {
         this.handleFunTimeMode();
      }

      if (this.mode.isSelected("Grim") && e.isPre() && MovingUtil.hasPlayerMovement()) {
         this.handleGrimMode();
      }

      if (this.mode.isSelected("HolyWorld") && e.isPre() && MovingUtil.hasPlayerMovement()) {
         this.handleHolyWorldMode();
      }

      if (this.mode.isSelected("Funtime Elytra") && !e.isPre() && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) {
         mc.player.stopGliding();
      }
   }

   @EventHandler
   public void onUsingItem(UsingItemEvent event) {
      if (this.mode.isSelected("w  ") && event.getType() == -1 && InventoryUtils.findHotbarItem(Items.ELYTRA) != -1) {
         event.cancel();
      }
   }

   @Native
   private void handleFunTimeMode() {
      if (!mc.player.isSwimming()
         && !mc.player.isGliding()
         && !mc.player.isSneaking()
         && mc.player.getBoundingBox().maxY - mc.player.getBoundingBox().minY < 1.5) {
         float motion = mc.player.hasStatusEffect(StatusEffects.SPEED) ? 0.32F : 0.28F;
         MovingUtil.setVelocity(motion);
      }
   }

   @Native
   private void handleFuntimeElytraMode() {
      int elytraSlot = InventoryUtils.findHotbarItem(Items.ELYTRA);
      if (elytraSlot != -1) {
         if (this.diving.isValue()) {
            float yaw = mc.gameRenderer.getCamera().getYaw();
            if (mc.options.getPerspective().isFrontView()) {
               yaw -= 180.0F;
            }

            RotationController.INSTANCE.rotateTo(new Angle(yaw, 55.0F), RotationConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
         }

         mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
         mc.player.startGliding();
      }
   }

   @Native
   private void handleGrimMode() {
      int collisions = 0;

      for (Entity ent : mc.world.getEntities()) {
         if (ent != mc.player
            && !(ent instanceof ArmorStandEntity)
            && (ent instanceof LivingEntity || ent instanceof BoatEntity)
            && mc.player.getBoundingBox().expand(0.5).intersects(ent.getBoundingBox())) {
            collisions++;
         }
      }

      double[] motion = MovingUtil.forward(0.07 * collisions);
      mc.player.addVelocity(motion[0], 0.0, motion[1]);
   }

   @Native
   private void handleHolyWorldMode() {
      int collisions = 0;

      for (Entity ent : mc.world.getEntities()) {
         if (ent != mc.player
            && !(ent instanceof ArmorStandEntity)
            && (ent instanceof LivingEntity || ent instanceof BoatEntity)
            && mc.player.getBoundingBox().expand(0.35F).intersects(ent.getBoundingBox())) {
            collisions++;
         }
      }

      double[] motion = MovingUtil.forward(0.0205 * collisions);
      mc.player.addVelocity(motion[0], 0.0, motion[1]);
   }

   private boolean hasSprintingTarget() {
      return false;
   }

   @Override
   public void deactivate() {
   }
}
