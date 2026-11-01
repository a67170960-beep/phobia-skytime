package ruhack.phobia.mixin;

import net.minecraft.client.gui.screen.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.DeathScreenEvent;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin {
   @Shadow
   private int ticksSinceDeath;

   @Inject(method = "tick", at = @At("TAIL"))
   private void phobia$dispatchDeathScreenEvent(CallbackInfo ci) {
      EventManager.callEvent(new DeathScreenEvent(this.ticksSinceDeath));
   }
}
