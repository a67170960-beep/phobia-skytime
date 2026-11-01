package ruhack.phobia.system.modulesystem.impl.movement;

import com.adl.nativeprotect.Native;
import lombok.Generated;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.player.MovingUtil;
import ruhack.phobia.util.timer.StopWatch;

public class Jesus extends ModuleStructure {
   private final SelectSetting mode = new SelectSetting("Режим", "Выберите режим передвижения по воде", "Matrix", "Matrix", "MetaHVH", "FunTime New");
   private final SliderSettings funtimeSpeed = new SliderSettings("Скорость FT", "Скорость передвижения по воде", 0.08F)
      .range(0.01F, 0.2F)
      .visible(() -> this.mode.isSelected("FunTime New"));
   private final StopWatch timer = new StopWatch();
   private boolean isMoving;
   private int tickCounter = 0;
   private final float melonBallSpeed = 0.44F;

   public Jesus() {
      super("Jesus", "Позволяет игроку ходить по воде", ModuleCategory.MOVEMENT);
      this.settings(this.mode, this.funtimeSpeed);
   }

   @Override
   public void deactivate() {
      this.tickCounter = 0;
   }

   @EventHandler
   public void tick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (this.mode.isSelected("Matrix")) {
            this.handleMatrixMode();
         } else if (this.mode.isSelected("MetaHVH")) {
            this.handleMetaHVHMode();
         } else if (this.mode.isSelected("FunTime New")) {
            this.handleFunTimeNewMode();
         }
      }
   }

   @Native
   private void handleMatrixMode() {
      if (mc.player.isTouchingWater() || mc.player.isInLava()) {
         StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
         StatusEffectInstance slowEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
         ItemStack offHandItem = mc.player.getOffHandStack();
         String itemName = offHandItem.getName().getString();
         float appliedSpeed = 0.0F;
         if (itemName.contains("Ломтик Дыни") && speedEffect != null && speedEffect.getAmplifier() == 2) {
            appliedSpeed = 0.49254498F;
         } else if (speedEffect != null) {
            if (speedEffect.getAmplifier() == 2) {
               appliedSpeed = 0.506F;
            } else if (speedEffect.getAmplifier() == 1) {
               appliedSpeed = 0.44F;
            }
         } else {
            appliedSpeed = 0.2992F;
         }

         if (slowEffect != null) {
            appliedSpeed *= 0.85F;
         }

         MovingUtil.setVelocity(appliedSpeed);
         this.isMoving = mc.options.forwardKey.isPressed()
            || mc.options.backKey.isPressed()
            || mc.options.leftKey.isPressed()
            || mc.options.rightKey.isPressed();
         if (!this.isMoving) {
            mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
         }

         double yMotion = mc.options.jumpKey.isPressed() ? 0.019 : 0.003;
         mc.player.setVelocity(mc.player.getVelocity().x, yMotion, mc.player.getVelocity().z);
      }
   }

   @Native
   private void handleMetaHVHMode() {
      if (mc.player.isTouchingWater() || mc.player.isInLava()) {
         StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
         StatusEffectInstance slowEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
         float appliedSpeed = 0.47F;
         if (speedEffect != null) {
            if (speedEffect.getAmplifier() == 2) {
               appliedSpeed = 0.564F;
            } else if (speedEffect.getAmplifier() == 1) {
               appliedSpeed = 0.49349996F;
            }
         } else {
            appliedSpeed = 0.329F;
         }

         if (slowEffect != null) {
            appliedSpeed *= 0.8F;
         }

         MovingUtil.setVelocity(appliedSpeed);
         this.isMoving = mc.options.forwardKey.isPressed()
            || mc.options.backKey.isPressed()
            || mc.options.leftKey.isPressed()
            || mc.options.rightKey.isPressed();
         if (!this.isMoving) {
            mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
         }

         double yMotion = mc.options.jumpKey.isPressed() ? 0.025 : 0.005;
         mc.player.setVelocity(mc.player.getVelocity().x, yMotion, mc.player.getVelocity().z);
      }
   }

   @Native
   private void handleFunTimeNewMode() {
      if (!mc.player.isInFluid() && !mc.player.isTouchingWater()) {
         this.tickCounter = 0;
      } else {
         this.tickCounter++;
         if (this.tickCounter > 2) {
            this.tickCounter = 0;
         }

         if (MovingUtil.hasPlayerMovement()) {
            double speed = this.funtimeSpeed.getValue();
            double yaw = Math.toRadians(mc.player.getYaw());
            double motionX = -Math.sin(yaw) * speed;
            double motionZ = Math.cos(yaw) * speed;
            double motionY;
            if (this.tickCounter == 0) {
               motionY = 0.05;
            } else if (this.tickCounter == 2) {
               motionY = -0.05;
            } else {
               motionY = 0.0;
            }

            mc.player.setVelocity(motionX, motionY, motionZ);
         } else {
            mc.player.setVelocity(0.0, 0.0, 0.0);
         }
      }
   }

   @Generated
   public SelectSetting getMode() {
      return this.mode;
   }

   @Generated
   public SliderSettings getFuntimeSpeed() {
      return this.funtimeSpeed;
   }

   @Generated
   public StopWatch getTimer() {
      return this.timer;
   }

   @Generated
   public boolean isMoving() {
      return this.isMoving;
   }

   @Generated
   public int getTickCounter() {
      return this.tickCounter;
   }

   @Generated
   public float getMelonBallSpeed() {
      return 0.44F;
   }
}
