package ruhack.phobia.system.modulesystem.impl.movement;

import lombok.Generated;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.istance.Instance;
import ruhack.phobia.util.math.TaskPriority;
import ruhack.phobia.util.rotation.RotationConfig;
import ruhack.phobia.util.rotation.RotationController;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.string.PlayerInteractionHelper;
import ruhack.phobia.util.timer.StopWatch;

public class Fly extends ModuleStructure {
   private final SelectSetting mode = new SelectSetting(
         "Режим", "Выберите режим полета", "Vanilla", "Vanilla", "Gliding", "Elytra 1.17", "Dragon Fly", "FunTime"
      )
      .selected("Vanilla");
   private final SliderSettings speedXZ = new SliderSettings("Скорость XZ", "Горизонтальная скорость", 1.5F).range(0.5F, 5.0F);
   private final SliderSettings speedY = new SliderSettings("Скорость Y", "Вертикальная скорость", 1.0F).range(0.1F, 3.0F);
   private StopWatch timer = new StopWatch();

   public static Fly getInstance() {
      return Instance.get(Fly.class);
   }

   public Fly() {
      super("Fly", "Позволяет игроку летать", ModuleCategory.MOVEMENT);
      this.settings(this.mode, this.speedXZ, this.speedY);
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (this.state && mc.player != null && mc.world != null) {
         if (this.mode.isSelected("Vanilla")) {
            this.handleVanillaMode();
         } else if (this.mode.isSelected("Dragon Fly")) {
            this.handleDragonFlyMode();
         } else if (this.mode.isSelected("FunTime")) {
            this.handleFunTimeMode();
         }

         if (this.mode.isSelected("Elytra 1.17")) {
            if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA)
               && mc.player.getVelocity().x == 0.0
               && mc.player.getVelocity().z == 0.0
               && mc.player.getVelocity().y >= 0.0) {
               if (mc.player.isOnGround()) {
                  mc.options.jumpKey.setPressed(true);
               } else if (!mc.player.isGliding()) {
                  PlayerInteractionHelper.startFallFlying();
               }
            }

            if (mc.player.isGliding() && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
               double currentY = mc.player.getVelocity().y;
               double maxY = this.speedY.getValue();
               double step = 0.06;
               long interval = 0L;
               RotationController.INSTANCE.rotateTo(new Angle(mc.player.getYaw(), -4.0F), RotationConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
               if (this.timer.every(interval)) {
                  double newY = Math.min(currentY + step, maxY);
                  mc.player.setVelocity(0.0, newY, 0.0);
               }

               mc.options.jumpKey.setPressed(false);
            }
         }

         if (this.mode.isSelected("Gliding")) {
            double motionY = this.getMotionY2();
            this.addMotion();
            Vec3d v = mc.player.getVelocity();
            mc.player.setVelocity(v.x, motionY, v.z);
         }
      }
   }

   private void handleFunTimeMode() {
      if (mc.player != null) {
         mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
         mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
         mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
         float yaw = mc.player.getYaw();
         double speed = this.speedXZ.getValue() / 3.0;
         double ySpeed = this.speedY.getValue() / 8.0;
         float f = mc.player.forwardSpeed;
         float s = mc.player.sidewaysSpeed;
         if (f != 0.0F || s != 0.0F) {
            float yawRad = yaw * (float) (Math.PI / 180.0);
            double moveX = -MathHelper.sin(yawRad) * speed * f + MathHelper.cos(yawRad) * speed * s;
            double moveZ = MathHelper.cos(yawRad) * speed * f + MathHelper.sin(yawRad) * speed * s;
            mc.player.setVelocity(moveX, mc.player.getVelocity().y, moveZ);
         }

         if (mc.options.jumpKey.isPressed()) {
            mc.player.setVelocity(mc.player.getVelocity().x, ySpeed, mc.player.getVelocity().z);
         } else if (mc.options.sneakKey.isPressed()) {
            mc.player.setVelocity(mc.player.getVelocity().x, -ySpeed, mc.player.getVelocity().z);
         }
      }
   }

   private void addMotion() {
      float yaw = RotationController.INSTANCE.getRotation().getYaw();
      float f = mc.player.forwardSpeed;
      float s = mc.player.sidewaysSpeed;
      double x = 0.0;
      double z = 0.0;
      if (f != 0.0F || s != 0.0F) {
         float yawRad = yaw * (float) (Math.PI / 180.0);
         x = -MathHelper.sin(yawRad) * this.speedXZ.getValue() / 16.0F * f + MathHelper.cos(yawRad) * this.speedXZ.getValue() / 16.0F * s;
         z = MathHelper.cos(yawRad) * this.speedXZ.getValue() / 16.0F * f + MathHelper.sin(yawRad) * this.speedXZ.getValue() / 16.0F * s;
      }

      mc.player.addVelocity(x, mc.player.getVelocity().y, z);
   }

   private void handleVanillaMode() {
      double motionY = this.getMotionY();
      this.setMotion(this.speedXZ.getValue());
      Vec3d v = mc.player.getVelocity();
      mc.player.setVelocity(v.x, motionY, v.z);
   }

   private void handleDragonFlyMode() {
      if (mc.player.getAbilities().flying) {
         this.setMotion(this.speedXZ.getValue());
         double motionY = 0.0;
         if (mc.options.jumpKey.isPressed()) {
            motionY = this.speedY.getValue();
         }

         if (mc.options.sneakKey.isPressed()) {
            motionY = -this.speedY.getValue();
         }

         Vec3d v = mc.player.getVelocity();
         mc.player.setVelocity(v.x, motionY, v.z);
      }
   }

   private double getMotionY2() {
      if (mc.options.sneakKey.isPressed()) {
         return -this.speedY.getValue() / 26.0F;
      } else {
         return mc.options.jumpKey.isPressed() ? this.speedY.getValue() / 26.0F : 0.0;
      }
   }

   private double getMotionY() {
      if (mc.options.sneakKey.isPressed()) {
         return -this.speedY.getValue();
      } else {
         return mc.options.jumpKey.isPressed() ? this.speedY.getValue() : 0.0;
      }
   }

   private void setMotion(float speed) {
      float yaw = mc.player.getYaw();
      float f = mc.player.forwardSpeed;
      float s = mc.player.sidewaysSpeed;
      float speedScale = speed / 3.0F;
      double x = 0.0;
      double z = 0.0;
      if (f != 0.0F || s != 0.0F) {
         float yawRad = yaw * (float) (Math.PI / 180.0);
         x = -MathHelper.sin(yawRad) * speedScale * f + MathHelper.cos(yawRad) * speedScale * s;
         z = MathHelper.cos(yawRad) * speedScale * f + MathHelper.sin(yawRad) * speedScale * s;
      }

      mc.player.setVelocity(x, mc.player.getVelocity().y, z);
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.timer.reset();
   }

   @Generated
   public SelectSetting getMode() {
      return this.mode;
   }

   @Generated
   public SliderSettings getSpeedXZ() {
      return this.speedXZ;
   }

   @Generated
   public SliderSettings getSpeedY() {
      return this.speedY;
   }

   @Generated
   public StopWatch getTimer() {
      return this.timer;
   }
}
