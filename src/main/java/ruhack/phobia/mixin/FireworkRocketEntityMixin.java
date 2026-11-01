package ruhack.phobia.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.FireworkEvent;
import ruhack.phobia.util.rotation.RotationController;

@Mixin(FireworkRocketEntity.class)
public class FireworkRocketEntityMixin implements IMinecraft {
   @Shadow
   @Nullable
   private LivingEntity shooter;

   @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
   public Vec3d getRotationVectorHook(LivingEntity instance, Operation<Vec3d> original) {
      return this.shooter == mc.player ? RotationController.INSTANCE.getMoveRotation().toVector() : (Vec3d)original.call(new Object[]{instance});
   }

   @WrapOperation(
      method = "tick",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getVelocity()Lnet/minecraft/util/math/Vec3d;", ordinal = 0)
   )
   public Vec3d getVelocityHook(LivingEntity instance, Operation<Vec3d> original) {
      if (this.shooter == mc.player) {
         FireworkEvent event = new FireworkEvent((Vec3d)original.call(new Object[]{instance}));
         EventManager.callEvent(event);
         return event.getVector();
      } else {
         return (Vec3d)original.call(new Object[]{instance});
      }
   }
}
