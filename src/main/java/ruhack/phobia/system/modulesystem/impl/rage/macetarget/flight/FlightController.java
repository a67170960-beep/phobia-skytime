package ruhack.phobia.system.modulesystem.impl.rage.macetarget.flight;

import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.system.modulesystem.impl.rage.macetarget.prediction.TargetPredictor;
import ruhack.phobia.system.modulesystem.impl.rage.macetarget.state.MaceState;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;

public class FlightController {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private final TargetPredictor predictor;
   private boolean predictionEnabled = false;
   private float height = 30.0F;

   public FlightController(TargetPredictor predictor) {
      this.predictor = predictor;
   }

   public void setPredictionEnabled(boolean enabled) {
      this.predictionEnabled = enabled;
   }

   public void setHeight(float height) {
      this.height = height;
   }

   public Vec3d getTargetPosition(LivingEntity target, MaceState.Stage stage) {
      if (target == null) {
         return Vec3d.ZERO;
      } else {
         return this.predictionEnabled && this.predictor.isMoving() ? this.predictor.getPredictedPosition(target, stage) : target.getEyePos();
      }
   }

   public Angle calculateAngle(LivingEntity target, MaceState.Stage stage) {
      if (target != null && mc.player != null) {
         Vec3d targetPos = this.getTargetPosition(target, stage);
         switch (stage) {
            case FLYING_UP:
               Vec3d flyTarget = targetPos.add(0.0, this.height, 0.0);
               return AngleUtil.fromVec3d(flyTarget.subtract(mc.player.getEyePos()));
            case TARGETTING:
            case ATTACKING:
               return AngleUtil.fromVec3d(targetPos.subtract(mc.player.getEyePos()));
            default:
               return AngleUtil.cameraAngle();
         }
      } else {
         return AngleUtil.cameraAngle();
      }
   }

   @Generated
   public TargetPredictor getPredictor() {
      return this.predictor;
   }

   @Generated
   public boolean isPredictionEnabled() {
      return this.predictionEnabled;
   }

   @Generated
   public float getHeight() {
      return this.height;
   }
}
