package ruhack.phobia.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.world.ClientChunkLoadProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruhack.phobia.util.client.LoadingScreenFix;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin {
   @Shadow
   private ClientChunkLoadProgress chunkLoadProgress;
   @Unique
   private int phobia$loadingTicks;

   @Inject(method = "tick", at = @At("TAIL"))
   private void phobia$unstickLoading(CallbackInfo ci) {
      this.phobia$loadingTicks++;
      if (this.chunkLoadProgress == null || !this.chunkLoadProgress.isDone()) {
         if (LoadingScreenFix.shouldForceClose(this.phobia$loadingTicks, MinecraftClient.getInstance())) {
            ((LevelLoadingScreen)(Object)this).close();
         }
      }
   }
}
