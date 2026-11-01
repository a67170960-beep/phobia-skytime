package ruhack.phobia.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;
import net.minecraft.util.AssetInfo.TextureAssetInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public class CustomCapeMixin {
   @Unique
   private static final Identifier CAPE_ID = Identifier.of("minecraft", "capes/cape");
   @Unique
   private static final TextureAssetInfo CAPE_ASSET = new TextureAssetInfo(CAPE_ID);

   @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
   private void replaceCape(CallbackInfoReturnable<SkinTextures> cir) {
      AbstractClientPlayerEntity player = (AbstractClientPlayerEntity)(Object)this;
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && player.getUuid().equals(client.player.getUuid())) {
         SkinTextures old = (SkinTextures)cir.getReturnValue();
         cir.setReturnValue(new SkinTextures(old.body(), CAPE_ASSET, CAPE_ASSET, old.model(), old.secure()));
      }
   }
}
