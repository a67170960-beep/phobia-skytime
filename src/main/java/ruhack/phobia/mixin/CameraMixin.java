package ruhack.phobia.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.CameraPositionEvent;
import ruhack.phobia.system.modulesystem.impl.legit.LegitAura;
import ruhack.phobia.util.rotation.RotationController;
import ruhack.phobia.util.rotation.angle.Angle;

@Mixin(Camera.class)
public abstract class CameraMixin {
   @Shadow
   private Vec3d pos;
   @Shadow
   @Final
   private Mutable blockPos;
   @Shadow
   private float yaw;
   @Shadow
   private float pitch;

   @Shadow
   public abstract void setRotation(float var1, float var2);

   @Shadow
   protected abstract void moveBy(float var1, float var2, float var3);

   @Shadow
   protected abstract float clipToSpace(float var1);

   @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V", shift = Shift.AFTER))
   private void injectQuickPerspectiveSwap(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
      RotationController rotation = RotationController.INSTANCE;
      if (LegitAura.getInstance().isState()) {
         Angle prev = rotation.getPreviousAngle();
         Angle curr = rotation.getCurrentAngle();
         if (prev != null && curr != null) {
            float factor = tickProgress;
            float yaw = MathHelper.lerp(factor, prev.getYaw(), curr.getYaw());
            float pitch = MathHelper.lerp(factor, prev.getPitch(), curr.getPitch());
            this.setRotation(yaw, pitch);
         }
      }
   }

   @Inject(method = "setPos(Lnet/minecraft/util/math/Vec3d;)V", at = @At("HEAD"), cancellable = true)
   private void posHook(Vec3d pos, CallbackInfo ci) {
      CameraPositionEvent event = new CameraPositionEvent(pos);
      EventManager.callEvent(event);
      this.pos = pos = event.getPos();
      this.blockPos.set(pos.x, pos.y, pos.z);
      ci.cancel();
   }
}
