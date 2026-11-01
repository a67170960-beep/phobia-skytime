package ruhack.phobia.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.PlayerTravelEvent;
import ruhack.phobia.system.eventsystem.impl.PushEvent;
import ruhack.phobia.system.eventsystem.impl.SwimmingEvent;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements IMinecraft {
   @Inject(method = "isPushedByFluids", at = @At("HEAD"), cancellable = true)
   public void isPushedByFluids(CallbackInfoReturnable<Boolean> cir) {
      PushEvent event = new PushEvent(PushEvent.Type.WATER);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
   private void onTravelPre(Vec3d movementInput, CallbackInfo ci) {
      if (mc.player != null) {
         PlayerTravelEvent event = new PlayerTravelEvent(movementInput, true);
         EventManager.callEvent(event);
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @ModifyExpressionValue(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
   public Vec3d travelHook(Vec3d vec3d) {
      SwimmingEvent event = new SwimmingEvent(vec3d);
      EventManager.callEvent(event);
      return event.getVector();
   }

   @Inject(method = "travel", at = @At("RETURN"))
   private void onTravelPost(Vec3d movementInput, CallbackInfo ci) {
      if (mc.player != null) {
         PlayerTravelEvent event = new PlayerTravelEvent(movementInput, false);
         EventManager.callEvent(event);
      }
   }
}
