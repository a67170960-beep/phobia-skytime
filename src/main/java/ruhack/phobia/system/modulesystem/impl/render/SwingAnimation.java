package ruhack.phobia.system.modulesystem.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.HandAnimationEvent;
import ruhack.phobia.system.eventsystem.impl.SwingDurationEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.impl.rage.KillAura;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;

public class SwingAnimation extends ModuleStructure {
   private final SelectSetting swingType = new SelectSetting(
      "Тип взмаха", "Выберите тип взмаха", "Chop", "Chop", "Swipe", "Down", "Smooth", "Smooth 2", "Power", "Feast", "Twist", "Default"
   );
   private final SliderSettings hitStrengthSetting = new SliderSettings("Сила взмаха", "Сила анимации взмаха", 1.0F).range(0.5F, 3.0F);
   private final SliderSettings swingSpeedSetting = new SliderSettings("Длительность взмаха", "Длительность анимации удара", 1.0F).range(0.5F, 4.0F);
   private final BooleanSetting onlySwing = new BooleanSetting("Только при взмахе", "Показывает анимацию только при взмахе").setValue(false);
   private final BooleanSetting onlyAura = new BooleanSetting("Только при включенной КиллАуре", "Показывает анимацию только при включенной киллауре")
      .setValue(false);
   private float spinAngle = 0.0F;
   private float spinBackTimer = 0.0F;
   private boolean wasSwinging = false;

   public SwingAnimation() {
      super("SwingAnimation", "Добавляет анимацию предметов в руке", ModuleCategory.RENDER);
      this.settings(this.swingType, this.hitStrengthSetting, this.swingSpeedSetting, this.onlySwing, this.onlyAura);
   }

   @EventHandler
   public void onSwingDuration(SwingDurationEvent e) {
      KillAura aura = KillAura.getInstance();
      if (!this.onlyAura.isValue() || aura.isState() && aura.getTarget() != null) {
         e.setAnimation(this.swingSpeedSetting.getValue());
         e.cancel();
      }
   }

   @EventHandler
   public void onHandAnimation(HandAnimationEvent e) {
      boolean isMainHand = e.getHand().equals(Hand.MAIN_HAND);
      if (isMainHand) {
         MatrixStack matrix = e.getMatrices();
         float swingProgress = e.getSwingProgress();
         int i = mc.player.getMainArm().equals(Arm.RIGHT) ? 1 : -1;
         float sin1 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
         float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
         float sinSmooth = (float)(Math.sin(swingProgress * Math.PI) * 0.5);
         float strength = this.hitStrengthSetting.getValue();
         KillAura aura = KillAura.getInstance();
         if (this.onlyAura.isValue() && (!aura.isState() || aura.getTarget() == null)) {
            return;
         }

         if (this.onlySwing.isValue() && mc.player.handSwingTicks == 0) {
            matrix.translate(i * 0.56F, -0.52F, -0.72F);
         } else {
            switch (this.swingType.getValue()) {
               case "Chop":
                  matrix.translate(0.56F * i, -0.44F, -0.72F);
                  matrix.translate(0.0F, -0.19800001F, 0.0F);
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * i));
                  matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * -20.0F * i * strength));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength));
                  matrix.translate(0.4F, 0.2F, 0.2F);
                  matrix.translate(-0.5F, 0.08F, 0.0F);
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
                  break;
               case "Twist":
                  matrix.translate(i * 0.56F, -0.36F, -0.72F);
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(80 * i));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -90.0F * strength));
                  matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((sin1 - sin2) * 60.0F * i * strength));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F));
                  matrix.translate(0.0F, -0.1F, 0.05F);
                  break;
               case "Swipe":
                  matrix.translate(0.56F * i, -0.32F, -0.72F);
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70 * i));
                  matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20 * i));
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * sin1 * -5.0F * strength));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * sin1 * -120.0F * strength));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F));
                  break;
               case "Default":
                  matrix.translate(i * 0.56F, -0.52F - sin2 * 0.5F * strength, -0.72F);
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * i));
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45 * i));
                  break;
               case "Down":
                  matrix.translate(i * 0.56F, -0.32F, -0.72F);
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76 * i));
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5.0F * strength));
                  matrix.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100.0F * strength));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155.0F * strength));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0F));
                  break;
               case "Smooth":
                  matrix.translate(i * 0.56F, -0.42F, -0.72F);
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + sin1 * -20.0F * strength)));
                  matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * sin2 * -20.0F * strength));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength));
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
                  matrix.translate(0.0, -0.1, 0.0);
                  break;
               case "Smooth 2":
                  matrix.translate(i * 0.56F, -0.42F, -0.72F);
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength));
                  matrix.translate(0.0, -0.1, 0.0);
                  break;
               case "Power":
                  matrix.translate(i * 0.56F, -0.32F, -0.72F);
                  matrix.translate(-sinSmooth * sinSmooth * sin1 * i * strength, 0.0F, 0.0F);
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(61 * i));
                  matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * strength));
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * sin1 * -5.0F * strength));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * sin1 * -30.0F * strength));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinSmooth * -60.0F * strength));
                  break;
               case "Feast":
                  matrix.translate(i * 0.56F, -0.32F, -0.72F);
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 75.0F * i * strength));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -45.0F * strength));
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                  matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
                  matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35 * i));
            }
         }

         e.cancel();
      }
   }
}
