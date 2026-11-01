package ruhack.phobia.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruhack.phobia.system.modulesystem.impl.render.Ambience;
import ruhack.phobia.system.modulesystem.impl.render.NoRender;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {
   @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"))
   private Object injectBrightness(Object original) {
      if (original instanceof Double doubleValue) {
         Ambience ambience = Ambience.getInstance();
         if (ambience != null && ambience.isState()) {
            float brightnessValue = ambience.getBrightnessValue();
            return brightnessValue >= 0.0F ? Math.max(doubleValue, brightnessValue * 10.0F) : Math.max(doubleValue * (1.0 + brightnessValue), 0.0);
         } else {
            return original;
         }
      } else {
         return original;
      }
   }

   @Inject(method = "getDarkness", at = @At("HEAD"), cancellable = true)
   private void removeDarknessEffect(CallbackInfoReturnable<Float> cir) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Darkness")) {
         cir.setReturnValue(0.0F);
      }
   }
}
