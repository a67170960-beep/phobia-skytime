package ruhack.phobia.util.rotation;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic.AngleSmoothMode;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic.LinearSmoothMode;
import ruhack.phobia.util.rotation.angle.Angle;

public class RotationConfig {
   public static RotationConfig DEFAULT = new RotationConfig(new LinearSmoothMode(), true, true, false);
   public final boolean moveCorrection;
   public static boolean freeCorrection;
   public static boolean clickSpam;
   private final AngleSmoothMode angleSmooth;
   private final int resetThreshold = 1;

   public RotationConfig(boolean moveCorrection, boolean freeCorrection, boolean clickSpam) {
      this(new LinearSmoothMode(), moveCorrection, freeCorrection, clickSpam);
   }

   public RotationConfig(boolean moveCorrection) {
      this(new LinearSmoothMode(), moveCorrection, true, false);
   }

   public RotationConfig(AngleSmoothMode angleSmooth, boolean moveCorrection, boolean freeCorrection, boolean clickSpam) {
      this.angleSmooth = angleSmooth;
      this.moveCorrection = moveCorrection;
      RotationConfig.freeCorrection = freeCorrection;
      RotationConfig.clickSpam = clickSpam;
   }

   public RotationPlan createRotationPlan(Angle angle, Vec3d vec, Entity entity, int reset) {
      return new RotationPlan(angle, vec, entity, this.angleSmooth, reset, 1.0F, this.moveCorrection, freeCorrection);
   }

   public RotationPlan createRotationPlan(Angle angle, Vec3d vec, Entity entity, boolean moveCorrection, boolean freeCorrection) {
      return new RotationPlan(angle, vec, entity, this.angleSmooth, 1, 1.0F, moveCorrection, freeCorrection);
   }
}
