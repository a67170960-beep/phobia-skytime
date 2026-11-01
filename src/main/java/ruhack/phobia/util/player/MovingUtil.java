package ruhack.phobia.util.player;

import java.util.Objects;
import lombok.Generated;
import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.system.modulesystem.impl.rage.KillAura;
import ruhack.phobia.util.rotation.RotationController;

public final class MovingUtil implements IMinecraft {
   public static boolean hasPlayerMovement() {
      Input input = mc.player.input;
      if (input.hasForwardMovement()) {
         return true;
      }

      Vec2f vec = input.getMovementInput();
      return vec.x != 0.0F || vec.y != 0.0F;
   }

   public static void setVelocity(double velocity) {
      double[] direction = calculateDirection(velocity);
      Objects.requireNonNull(mc.player).setVelocity(direction[0], mc.player.getVelocity().getY(), direction[1]);
   }

   public static final boolean moveKeyPressed(int keyNumber) {
      boolean w = mc.options.forwardKey.isPressed();
      boolean a = mc.options.leftKey.isPressed();
      boolean s = mc.options.backKey.isPressed();
      boolean d = mc.options.rightKey.isPressed();
      return keyNumber == 0 ? w : (keyNumber == 1 ? a : (keyNumber == 2 ? s : keyNumber == 3 && d));
   }

   public static final boolean w() {
      return moveKeyPressed(0);
   }

   public static final boolean a() {
      return moveKeyPressed(1);
   }

   public static final boolean s() {
      return moveKeyPressed(2);
   }

   public static final boolean d() {
      return moveKeyPressed(3);
   }

   public static final float moveYaw(float entityYaw) {
      return entityYaw
         + (
            !a() || !d() || w() && s() || !w() && !s()
               ? (
                  !w() || !s() || a() && d() || !a() && !d()
                     ? (
                        (!a() || !d() || w() && s()) && (!w() || !s() || a() && d())
                           ? (
                              !a() && !d() && !s()
                                 ? 0
                                 : (w() && !s() ? 45 : (!s() || w() ? (!w() && !s() || w() && s() ? 90 : 0) : (!a() && !d() ? 180 : 135))) * (a() ? -1 : 1)
                           )
                           : 0
                     )
                     : (a() ? -90 : (d() ? 90 : 0))
               )
               : (w() ? 0 : (s() ? 180 : 0))
         );
   }

   public static double[] forward(double d) {
      Vec2f movement = mc.player.input.getMovementInput();
      float f = movement.y;
      float f2 = movement.x;
      float f3 = RotationController.INSTANCE.getRotation().getYaw();
      if (f != 0.0F) {
         if (f2 > 0.0F) {
            f3 += f > 0.0F ? -45 : 45;
         } else if (f2 < 0.0F) {
            f3 += f > 0.0F ? 45 : -45;
         }

         f2 = 0.0F;
         if (f > 0.0F) {
            f = 1.0F;
         } else if (f < 0.0F) {
            f = -1.0F;
         }
      }

      double d2 = Math.sin(Math.toRadians(f3 + 90.0F));
      double d3 = Math.cos(Math.toRadians(f3 + 90.0F));
      double d4 = f * d * d3 + f2 * d * d2;
      double d5 = f * d * d2 - f2 * d * d3;
      return new double[]{d4, d5};
   }

   public static double[] calculateDirection(double distance) {
      Vec2f movement = mc.player.input.getMovementInput();
      float forward = movement.y;
      float sideways = movement.x;
      return calculateDirection(forward, sideways, distance);
   }

   public static double[] calculateDirection(float forward, float sideways, double distance) {
      float yaw = mc.player.getYaw();
      if (forward != 0.0F) {
         if (sideways > 0.0F) {
            yaw += forward > 0.0F ? -45.0F : 45.0F;
         } else if (sideways < 0.0F) {
            yaw += forward > 0.0F ? 45.0F : -45.0F;
         }

         sideways = 0.0F;
         forward = forward > 0.0F ? 1.0F : -1.0F;
      }

      double sinYaw = Math.sin(Math.toRadians(yaw + 90.0F));
      double cosYaw = Math.cos(Math.toRadians(yaw + 90.0F));
      double xMovement = forward * distance * cosYaw + sideways * distance * sinYaw;
      double zMovement = forward * distance * sinYaw - sideways * distance * cosYaw;
      return new double[]{xMovement, zMovement};
   }

   public static double getDistanceToGround() {
      for (double y = mc.player.getY(); y > 0.0; y -= 0.1) {
         if (!mc.world.getBlockState(mc.player.getBlockPos().down((int)(mc.player.getY() - y + 1.0))).isAir()) {
            return mc.player.getY() - y;
         }
      }

      return 256.0;
   }

   public static float calculateBodyYaw(float yaw, float prevBodyYaw, double prevX, double prevZ, double currentX, double currentZ, float handSwingProgress) {
      double motionX = currentX - prevX;
      double motionZ = currentZ - prevZ;
      float motionSquared = (float)(motionX * motionX + motionZ * motionZ);
      float bodyYaw = prevBodyYaw;
      float swing = mc.player.handSwingProgress;
      if (motionSquared > 0.0025000002F) {
         float movementYaw = (float)MathHelper.atan2(motionZ, motionX) * (180.0F / (float)Math.PI) - 90.0F;
         float yawDiff = MathHelper.abs(MathHelper.wrapDegrees(yaw) - movementYaw);
         if (95.0F < yawDiff && yawDiff < 265.0F) {
            bodyYaw = movementYaw - 180.0F;
         } else {
            bodyYaw = movementYaw;
         }
      }

      if (mc.player != null && mc.player.handSwingProgress - 0.3F > 0.0F) {
         bodyYaw = yaw;
      }

      float deltaYaw = MathHelper.wrapDegrees(bodyYaw - prevBodyYaw);
      bodyYaw = prevBodyYaw + deltaYaw * 0.3F;
      float yawOffsetDiff = MathHelper.wrapDegrees(yaw - bodyYaw);
      float maxHeadRotation = 52.0F;
      if (Math.abs(yawOffsetDiff) > maxHeadRotation) {
         bodyYaw += yawOffsetDiff - MathHelper.sign(yawOffsetDiff) * maxHeadRotation;
      }

      return KillAura.getInstance().isState() && KillAura.getInstance().getTarget() != null ? yaw : bodyYaw;
   }

   public static double getDegreesRelativeToView(Vec3d positionRelativeToPlayer, float yaw) {
      float optimalYaw = (float)Math.atan2(-positionRelativeToPlayer.x, positionRelativeToPlayer.z);
      double currentYaw = Math.toRadians(MathHelper.wrapDegrees(yaw));
      return Math.toDegrees(MathHelper.wrapDegrees(optimalYaw - currentYaw));
   }

   public static PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs, float deadAngle) {
      boolean forwards = input.forward();
      boolean backwards = input.backward();
      boolean left = input.left();
      boolean right = input.right();
      if (dgs >= -90.0F + deadAngle && dgs <= 90.0F - deadAngle) {
         forwards = true;
      } else if (dgs < -90.0F - deadAngle || dgs > 90.0F + deadAngle) {
         backwards = true;
      }

      if (dgs >= 0.0F + deadAngle && dgs <= 180.0F - deadAngle) {
         right = true;
      } else if (dgs >= -180.0F + deadAngle && dgs <= 0.0F - deadAngle) {
         left = true;
      }

      return new PlayerInput(forwards, backwards, left, right, input.jump(), input.sneak(), input.sprint());
   }

   public static PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs) {
      return getDirectionalInputForDegrees(input, dgs, 20.0F);
   }

   @Generated
   private MovingUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
