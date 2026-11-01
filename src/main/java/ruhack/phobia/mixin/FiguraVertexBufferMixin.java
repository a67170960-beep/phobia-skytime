package ruhack.phobia.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ruhack.phobia.system.modulesystem.impl.render.ShaderESP;
import ruhack.phobia.util.render.ClientPipelines;

@Mixin(targets = "org/figuramc/figura/model/rendering/ImmediateFiguraRenderer$VertexBuffer", remap = false)
public class FiguraVertexBufferMixin {
   @Redirect(
      method = "consume",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider;getBuffer(Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;", remap = true),
      remap = false
   )
   private VertexConsumer phobia$directCustomModelOutline(VertexConsumerProvider provider, RenderLayer layer) {
      if (ClientPipelines.isCustomModelOutline(layer)) {
         OutlineVertexConsumerProvider outlineProvider = MinecraftClient.getInstance().getBufferBuilders().getOutlineVertexConsumers();
         ShaderESP shaderESP = ShaderESP.getInstance();
         if (shaderESP != null) {
            outlineProvider.setColor(shaderESP.getOutlineColor());
         }

         return outlineProvider.getBuffer(layer);
      } else {
         return provider.getBuffer(layer);
      }
   }
}
