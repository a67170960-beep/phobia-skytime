package ruhack.phobia.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.BoundingBoxControlEvent;
import ruhack.phobia.system.eventsystem.impl.PlayerVelocityStrafeEvent;
import ruhack.phobia.util.rotation.RotationController;

@Mixin(Entity.class)
public abstract class EntityMixin implements IMinecraft {
   @Shadow
   private Box boundingBox;
   @Shadow
   public float yaw;
   @Unique
   private boolean client$local;

   @Inject(method = "<init>", at = @At("TAIL"))
   private void onInit(EntityType<?> type, World world, CallbackInfo ci) {
      this.client$local = (Object)this instanceof ClientPlayerEntity;
   }

   @Inject(method = "getBoundingBox", at = @At("HEAD"), cancellable = true)
   public final void getBoundingBox(CallbackInfoReturnable<Box> cir) {
      BoundingBoxControlEvent event = new BoundingBoxControlEvent(this.boundingBox, (Entity)(Object)this);
      EventManager.callEvent(event);
      cir.setReturnValue(event.getBox());
   }

   @Redirect(
      method = "updateVelocity",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;")
   )
   public Vec3d hookVelocity(Vec3d movementInput, float speed, float yaw) {
      if ((Object)this == mc.player) {
         PlayerVelocityStrafeEvent event = new PlayerVelocityStrafeEvent(movementInput, speed, yaw, Entity.movementInputToVelocity(movementInput, speed, yaw));
         EventManager.callEvent(event);
         return event.getVelocity();
      } else {
         return Entity.movementInputToVelocity(movementInput, speed, yaw);
      }
   }

   @ModifyVariable(method = "getRotationVector", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private float modifyPitch(float pitch) {
      return (Object)this instanceof ClientPlayerEntity && RotationController.INSTANCE.getCurrentAngle() != null
         ? RotationController.INSTANCE.getCurrentAngle().getPitch()
         : pitch;
   }

   @ModifyVariable(method = "getRotationVector", at = @At("HEAD"), ordinal = 1, argsOnly = true)
   private float modifyYaw(float yaw) {
      return (Object)this instanceof ClientPlayerEntity && RotationController.INSTANCE.getCurrentAngle() != null
         ? RotationController.INSTANCE.getCurrentAngle().getYaw()
         : yaw;
   }
}
