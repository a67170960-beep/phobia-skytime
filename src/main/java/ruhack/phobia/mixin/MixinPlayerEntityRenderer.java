package ruhack.phobia.mixin;

import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruhack.phobia.system.modulesystem.impl.render.ShaderESP;
import ruhack.phobia.system.modulesystem.impl.render.Tags;
import ruhack.phobia.system.modulesystem.impl.render.chinahat.ChinaHatFeatureRenderer;
import ruhack.phobia.system.modulesystem.impl.render.wings.WingsFeatureRenderer;

@Mixin(PlayerEntityRenderer.class)
public class MixinPlayerEntityRenderer {
   @Inject(method = "updateRenderState", at = @At("TAIL"))
   private void phobia$shaderEspPlayers(PlayerLikeEntity player, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
      ShaderESP shaderESP = ShaderESP.getInstance();
      if (shaderESP != null && shaderESP.isState() && shaderESP.shouldOutline(player)) {
         state.outlineColor = shaderESP.usesCustomModelOutline(player) ? 0 : shaderESP.getOutlineColor();
      }
   }

   @Inject(method = "<init>", at = @At("TAIL"))
   private void onInit(Context ctx, boolean slim, CallbackInfo ci) {
      PlayerEntityRenderer renderer = (PlayerEntityRenderer)(Object)this;
      renderer.addFeature(new ChinaHatFeatureRenderer(renderer));
      renderer.addFeature(new WingsFeatureRenderer(renderer));
   }

   @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
   private void phobia$forcePlayerTag(PlayerLikeEntity player, double squaredDistance, CallbackInfoReturnable<Boolean> cir) {
      Tags tags = Tags.getInstance();
      if (tags != null && tags.isState() && tags.shouldTag(player)) {
         cir.setReturnValue(false);
      }
   }
}
