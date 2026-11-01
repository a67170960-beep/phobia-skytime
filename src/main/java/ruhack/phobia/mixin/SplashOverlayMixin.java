package ruhack.phobia.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.system.rendersystem.render2d.menu.MainMenuShader;
import ruhack.phobia.system.screensystem.MainMenuScreen;
import ruhack.phobia.util.colorschem.ColorUtil;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
   private static final Identifier PHOBIA_LOGO = Identifier.of("phobia", "phobia.png");
   @Shadow
   @Final
   private MinecraftClient client;
   @Shadow
   @Final
   private ResourceReload reload;
   @Shadow
   private float progress;
   @Shadow
   private long reloadCompleteTime;
   @Shadow
   private long reloadStartTime;

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void phobia$renderResourceReload(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      ci.cancel();
      long now = Util.getMeasuringTimeMs();
      if (this.reloadStartTime == -1L) {
         this.reloadStartTime = now;
      }

      float actualProgress = this.reload.getProgress();
      this.progress = MathHelper.clamp(this.progress * 0.95F + actualProgress * 0.05F, 0.0F, 1.0F);

      try {
         float shaderMouseX = context.getScaledWindowWidth() <= 0 ? 0.5F : (float)mouseX / context.getScaledWindowWidth();
         float shaderMouseY = context.getScaledWindowHeight() <= 0 ? 0.5F : 1.0F - (float)mouseY / context.getScaledWindowHeight();
         MainMenuShader.render(shaderMouseX, shaderMouseY);
      } catch (Throwable ignored) {
         context.fillGradient(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), -15263716, -16250613);
         if (MsdfManager.BOLD != null) {
            MainMenuShader.shutdown();
         }
      }

      try {
         this.phobia$renderProgressBar(context);
      } catch (Throwable ignored) {
         this.phobia$renderFallbackProgressBar(context);
      }

      if (this.reloadCompleteTime > -1L && now - this.reloadCompleteTime >= 2000L) {
         MainMenuScreen.protectFromFancyMenu(this.client.currentScreen);
         this.client.setOverlay(null);
      }
   }

   private void phobia$renderProgressBar(DrawContext context) {
      float screenWidth = Render2D.getFixedScaledWidth();
      float screenHeight = Render2D.getFixedScaledHeight();
      float width = Math.max(135.0F, Math.min(180.0F, screenWidth * 0.36F));
      float height = 3.5F;
      float x = (screenWidth - width) * 0.5F;
      float y = screenHeight * 0.58F;
      float logoSize = 78.0F;
      float logoX = x + width * 0.5F - logoSize * 0.5F;
      float logoY = y - logoSize - 17.0F;
      Render2D.image(context, logoX, logoY, logoSize, PHOBIA_LOGO, ColorUtil.rgba(245, 245, 248, 255), 0.0F, false);
      float filledWidth = width * MathHelper.clamp(this.progress, 0.0F, 1.0F);
      if (filledWidth > 0.35F) {
         Render2D.rect(context, x - 4.0F, y - 4.0F, filledWidth + 8.0F, height + 8.0F, 5.75F, ColorUtil.rgba(255, 255, 255, 7), false);
         Render2D.rect(context, x - 2.5F, y - 2.5F, filledWidth + 5.0F, height + 5.0F, 4.25F, ColorUtil.rgba(255, 255, 255, 12), false);
         Render2D.rect(context, x - 1.25F, y - 1.25F, filledWidth + 2.5F, height + 2.5F, 3.0F, ColorUtil.rgba(255, 255, 255, 20), false);
      }

      Render2D.rect(context, x, y, width, height, 1.75F, ColorUtil.rgba(255, 255, 255, 32), false);
      if (filledWidth > 0.35F) {
         Render2D.rect(context, x, y, filledWidth, height, Math.min(1.75F, filledWidth * 0.5F), ColorUtil.rgba(245, 245, 248, 255), false);
      }
   }

   private void phobia$renderFallbackProgressBar(DrawContext context) {
      int screenWidth = context.getScaledWindowWidth();
      int screenHeight = context.getScaledWindowHeight();
      int width = Math.max(135, Math.min(180, Math.round(screenWidth * 0.36F)));
      int x = (screenWidth - width) / 2;
      int y = Math.round(screenHeight * 0.58F);
      int filled = Math.round(width * MathHelper.clamp(this.progress, 0.0F, 1.0F));
      context.fill(x, y, x + width, y + 3, 721420287);
      if (filled > 0) {
         context.fill(x, y, x + filled, y + 3, -657928);
      }
   }
}
