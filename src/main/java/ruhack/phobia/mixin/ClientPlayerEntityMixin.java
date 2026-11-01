package ruhack.phobia.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.MoveEvent;
import ruhack.phobia.system.eventsystem.impl.PushEvent;
import ruhack.phobia.system.eventsystem.impl.UsingItemEvent;
import ruhack.phobia.util.player.MovingUtil;
import ruhack.phobia.util.rotation.RotationController;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
   @Final
   @Shadow
   protected MinecraftClient client;
   private double prevX = 0.0;
   private double prevZ = 0.0;
   private float prevBodyYaw = 0.0F;

   public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
      super(world, profile);
   }

   @Shadow
   protected abstract void autoJump(float var1, float var2);

   @Inject(
      method = "move",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"),
      cancellable = true
   )
   public void onMoveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
      MoveEvent event = new MoveEvent(movement);
      EventManager.callEvent(event);
      double d = this.getX();
      double e = this.getZ();
      super.move(movementType, event.getMovement());
      this.autoJump((float)(this.getX() - d), (float)(this.getZ() - e));
      ci.cancel();
   }

   @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
   public void pushOutOfBlocks(double x, double z, CallbackInfo ci) {
      PushEvent event = new PushEvent(PushEvent.Type.BLOCK);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Redirect(method = "applyMovementSpeedFactors", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec2f;multiply(F)Lnet/minecraft/util/math/Vec2f;", ordinal = 1))
   private Vec2f cancelItemSlowdown(Vec2f vec2f, float multiplier) {
      UsingItemEvent event = new UsingItemEvent((byte)1);
      EventManager.callEvent(event);
      return event.isCancelled() && this.isUsingItem() && !this.hasVehicle() ? vec2f.multiply(1.0F) : vec2f.multiply(multiplier);
   }

   @ModifyExpressionValue(method = {"sendMovementPackets", "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
   private float hookSilentRotationYaw(float original) {
      if (IMinecraft.mc.player != null && RotationController.INSTANCE.getRotation() != null) {
         float currentYaw = RotationController.INSTANCE.getRotation().getYaw();
         float newBodyYaw = MovingUtil.calculateBodyYaw(
            currentYaw,
            this.prevBodyYaw,
            this.prevX,
            this.prevZ,
            IMinecraft.mc.player.getX(),
            IMinecraft.mc.player.getZ(),
            IMinecraft.mc.player.handSwingProgress
         );
         this.prevBodyYaw = newBodyYaw;
         this.prevX = IMinecraft.mc.player.getX();
         this.prevZ = IMinecraft.mc.player.getZ();
         IMinecraft.mc.player.setBodyYaw(newBodyYaw);
         return currentYaw;
      } else {
         return original;
      }
   }

   @ModifyExpressionValue(method = {"sendMovementPackets", "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
   private float hookSilentRotationPitch(float original) {
      return RotationController.INSTANCE.getRotation().getPitch();
   }
}
