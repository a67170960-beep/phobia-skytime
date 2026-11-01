package ruhack.phobia.system.rendersystem.render2d.menu;

import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public final class MainMenuBackground {
   private static volatile Boolean customPanorama;

   private MainMenuBackground() {
   }

   public static void invalidate() {
      customPanorama = null;
   }

   public static boolean hasCustomPanorama(MinecraftClient client) {
      if (client == null) {
         return false;
      }

      Boolean cached = customPanorama;
      if (cached != null) {
         return cached;
      }

      boolean result = detect(client);
      customPanorama = result;
      return result;
   }

   private static boolean detect(MinecraftClient client) {
      ResourceManager manager = client.getResourceManager();

      for (int i = 0; i < 6; i++) {
         Identifier face = Identifier.ofVanilla("textures/gui/title/background/panorama_" + i + ".png");
         Optional<Resource> resource = manager.getResource(face);
         if (resource.isPresent() && !isBuiltinPack(resource.get())) {
            return true;
         }
      }

      return false;
   }

   private static boolean isBuiltinPack(Resource resource) {
      String packId = resource.getPack().getId();
      return "vanilla".equals(packId) || "fabric".equals(packId) || "programmer_art".equals(packId);
   }
}
