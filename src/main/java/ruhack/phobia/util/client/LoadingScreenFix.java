package ruhack.phobia.util.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;

public final class LoadingScreenFix {
   private LoadingScreenFix() {
   }

   public static boolean shouldForceClose(int ticks, MinecraftClient client) {
      if (ticks > 500) {
         return true;
      }

      if (client.world != null && client.player != null) {
         WorldRenderer renderer = client.worldRenderer;
         int chunks = renderer.getCompletedChunkCount();
         if (ticks > 160 && chunks >= 1) {
            return true;
         } else {
            return ticks > 40 && chunks >= 9 && renderer.isTerrainRenderComplete() ? true : ticks > 80 && chunks >= 4;
         }
      } else {
         return false;
      }
   }
}
