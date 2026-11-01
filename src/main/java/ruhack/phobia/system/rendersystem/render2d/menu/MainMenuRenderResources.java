package ruhack.phobia.system.rendersystem.render2d.menu;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.system.rendersystem.render2d.gif.Gif2D;
import ruhack.phobia.system.rendersystem.render2d.outline.Outline2D;
import ruhack.phobia.system.rendersystem.render2d.rectangle.Rectangle2D;

public final class MainMenuRenderResources {
   private MainMenuRenderResources() {
   }

   public static void reload() {
      Runnable task = MainMenuRenderResources::reloadOnRenderThread;
      if (RenderSystem.isOnRenderThread()) {
         task.run();
      } else {
         MinecraftClient.getInstance().execute(task);
      }
   }

   private static void reloadOnRenderThread() {
      MainMenuBackground.invalidate();
      MainMenuShader.shutdown();
      Gif2D.shutdown();
      Rectangle2D.shutdown();
      Outline2D.shutdown();
      Msdf2D.shutdown();
      MsdfManager.shutdown();
      Rectangle2D.init();
      Outline2D.init();
      MsdfManager.init();
      Msdf2D.init();
   }
}
