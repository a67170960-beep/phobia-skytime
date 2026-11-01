package ruhack.phobia.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AccessibilityOnboardingScreen.class)
public class AccessibilityOnboardingScreenMixin {
   @Inject(method = "init", at = @At("HEAD"), cancellable = true)
   private void skipOnboardingScreen(CallbackInfo ci) {
      MinecraftClient.getInstance().setScreen(new TitleScreen());
      ci.cancel();
   }
}
