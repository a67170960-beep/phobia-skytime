package ruhack.phobia.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil.Type;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.FovEvent;
import ruhack.phobia.system.eventsystem.impl.HotBarScrollEvent;
import ruhack.phobia.system.eventsystem.impl.KeyEvent;
import ruhack.phobia.system.eventsystem.impl.MouseRotationEvent;

@Mixin(Mouse.class)
public abstract class MouseMixin {
   @Final
   @Shadow
   private MinecraftClient client;
   @Shadow
   private boolean cursorLocked;
   @Shadow
   private double x;
   @Shadow
   private double y;
   @Shadow
   private double cursorDeltaX;
   @Shadow
   private double cursorDeltaY;
   @Shadow
   private boolean hasResolutionChanged;

   @Inject(method = "onMouseButton", at = @At("HEAD"))
   public void onMouseButtonHook(long window, MouseInput input, int action, CallbackInfo ci) {
      if (input.button() != -1 && window == this.client.getWindow().getHandle()) {
         EventManager.callEvent(new KeyEvent(this.client.currentScreen, Type.MOUSE, input.button(), action));
      }
   }

   @Inject(
      method = "onMouseScroll",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"),
      cancellable = true
   )
   public void onMouseScrollHook(long window, double horizontal, double vertical, CallbackInfo ci) {
      HotBarScrollEvent event = new HotBarScrollEvent(horizontal, vertical);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "updateMouse", at = @At("HEAD"))
   private void onUpdateMouse(double timeDelta, CallbackInfo ci) {
      FovEvent event = new FovEvent();
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         double slowdown = (double)event.getFov() / ((Integer)(Object)this.client.options.getFov().getValue()).intValue();
         this.cursorDeltaX *= slowdown;
         this.cursorDeltaY *= slowdown;
      }
   }

   @WrapWithCondition(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"), require = 1, allow = 1)
   private boolean modifyMouseRotationInput(ClientPlayerEntity instance, double cursorDeltaX, double cursorDeltaY) {
      MouseRotationEvent event = new MouseRotationEvent((float)cursorDeltaX, (float)cursorDeltaY);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         return false;
      }

      instance.changeLookDirection(event.getCursorDeltaX(), event.getCursorDeltaY());
      return false;
   }
}
