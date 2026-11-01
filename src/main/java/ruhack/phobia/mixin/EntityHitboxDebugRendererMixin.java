package ruhack.phobia.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.debug.EntityHitboxDebugRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ruhack.phobia.util.rotation.RotationController;
import ruhack.phobia.util.rotation.angle.Angle;

@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {
   @Redirect(method = "drawHitbox", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getRotationVec(F)Lnet/minecraft/util/math/Vec3d;"))
   private Vec3d redirectLookVector(Entity entity, float tickProgress) {
      if (entity == MinecraftClient.getInstance().player) {
         RotationController r = RotationController.INSTANCE;
         Angle angle = r.getRotation();
         if (angle != null && RotationController.INSTANCE.getRotation().toVector() != MinecraftClient.getInstance().player.getRotationVector()) {
            float yaw = angle.getYaw();
            float pitch = angle.getPitch();
            float yawRad = (float)Math.toRadians(-yaw);
            float pitchRad = (float)Math.toRadians(-pitch);
            float x = MathHelper.sin(yawRad) * MathHelper.cos(pitchRad);
            float y = MathHelper.sin(pitchRad);
            float z = MathHelper.cos(yawRad) * MathHelper.cos(pitchRad);
            return new Vec3d(x, y, z);
         }
      }

      return entity.getRotationVec(tickProgress);
   }
}
