package ruhack.phobia.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.lang.reflect.Method;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.JumpEvent;
import ruhack.phobia.system.eventsystem.impl.PushEvent;
import ruhack.phobia.system.eventsystem.impl.SwingDurationEvent;
import ruhack.phobia.util.rotation.RotationController;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
   @Shadow
   public float bodyYaw;
   @Unique
   private static boolean baritoneChecked = false;
   @Unique
   private static boolean baritoneAvailable = false;
   @Unique
   private static Method getProviderMethod;
   @Unique
   private static Method getPrimaryBaritoneMethod;
   @Unique
   private static Method getPathingBehaviorMethod;
   @Unique
   private static Method isPathingMethod;

   @Shadow
   public abstract boolean hasStatusEffect(RegistryEntry<StatusEffect> var1);

   @Shadow
   @Nullable
   public abstract StatusEffectInstance getStatusEffect(RegistryEntry<StatusEffect> var1);

   @Shadow
   public abstract boolean isInSwimmingPose();

   @Shadow
   protected abstract double getEffectiveGravity();

   @Unique
   private boolean isBaritonePathing() {
      try {
         if (!baritoneChecked) {
            baritoneChecked = true;

            try {
               Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
               getProviderMethod = apiClass.getMethod("getProvider");
               Class<?> providerClass = Class.forName("baritone.api.IBaritoneProvider");
               getPrimaryBaritoneMethod = providerClass.getMethod("getPrimaryBaritone");
               Class<?> baritoneClass = Class.forName("baritone.api.IBaritone");
               getPathingBehaviorMethod = baritoneClass.getMethod("getPathingBehavior");
               Class<?> pathingClass = Class.forName("baritone.api.behavior.IPathingBehavior");
               isPathingMethod = pathingClass.getMethod("isPathing");
               baritoneAvailable = true;
            } catch (ClassNotFoundException | NoSuchMethodException e) {
               baritoneAvailable = false;
            }
         }

         if (!baritoneAvailable) {
            return false;
         }

         Object provider = getProviderMethod.invoke(null);
         if (provider == null) {
            return false;
         }

         Object baritone = getPrimaryBaritoneMethod.invoke(provider);
         if (baritone == null) {
            return false;
         }

         Object pathingBehavior = getPathingBehaviorMethod.invoke(baritone);
         if (pathingBehavior == null) {
            return false;
         }

         Object result = isPathingMethod.invoke(pathingBehavior);
         return Boolean.TRUE.equals(result);
      } catch (Exception e) {
         return false;
      }
   }

   @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
   public void isPushable(CallbackInfoReturnable<Boolean> infoReturnable) {
      PushEvent event = new PushEvent(PushEvent.Type.COLLISION);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         infoReturnable.setReturnValue(false);
      }
   }

   @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
   private void jump(CallbackInfo info) {
      if ((Object)this instanceof ClientPlayerEntity player) {
         if (this.isBaritonePathing()) {
            return;
         }

         JumpEvent event = new JumpEvent(player);
         EventManager.callEvent(event);
         if (event.isCancelled()) {
            info.cancel();
         }
      }
   }

   @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
   private void swingProgressHook(CallbackInfoReturnable<Integer> cir) {
      if ((Object)this == MinecraftClient.getInstance().player) {
         SwingDurationEvent event = new SwingDurationEvent();
         EventManager.callEvent(event);
         if (event.isCancelled()) {
            float animation = event.getAnimation();
            if (StatusEffectUtil.hasHaste(MinecraftClient.getInstance().player)) {
               animation *= 6 - (1 + StatusEffectUtil.getHasteAmplifier(MinecraftClient.getInstance().player));
            } else {
               animation *= this.hasStatusEffect(StatusEffects.MINING_FATIGUE) ? 6 + (1 + this.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) * 2 : 6;
            }

            cir.setReturnValue((int)animation);
         }
      }
   }

   @ModifyExpressionValue(method = "jump", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/util/math/Vec3d;"))
   private Vec3d hookFixRotation(Vec3d original) {
      if ((Object)this != MinecraftClient.getInstance().player) {
         return original;
      }

      float yaw = RotationController.INSTANCE.getMoveRotation().getYaw() * (float) (Math.PI / 180.0);
      return new Vec3d(-MathHelper.sin(yaw) * 0.2F, 0.0, MathHelper.cos(yaw) * 0.2F);
   }

   @ModifyExpressionValue(method = "calcGlidingVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getPitch()F"))
   private float hookModifyFallFlyingPitch(float original) {
      return (Object)this != MinecraftClient.getInstance().player ? original : RotationController.INSTANCE.getMoveRotation().getPitch();
   }

   @ModifyExpressionValue(method = "calcGlidingVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
   private Vec3d hookModifyFallFlyingRotationVector(Vec3d original) {
      return (Object)this != MinecraftClient.getInstance().player ? original : RotationController.INSTANCE.getMoveRotation().toVector();
   }

   @ModifyExpressionValue(method = "turnHead", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;wrapDegrees(F)F", ordinal = 1))
   private float wrapDegreesHook(float original) {
      return (Object)this == MinecraftClient.getInstance().player
         ? MathHelper.wrapDegrees(RotationController.INSTANCE.getRotation().getYaw() - this.bodyYaw)
         : original;
   }
}

