package ruhack.phobia.mixin;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruhack.phobia.system.modulesystem.impl.misc.PVPSafe;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin {
   @Shadow
   private ButtonWidget exitButton;

   @Inject(method = {"initWidgets", "tick"}, at = @At("TAIL"))
   private void updateDisconnectButton(CallbackInfo ci) {
      if (this.exitButton != null) {
         this.exitButton.active = !PVPSafe.shouldBlockExit();
      }
   }
}
