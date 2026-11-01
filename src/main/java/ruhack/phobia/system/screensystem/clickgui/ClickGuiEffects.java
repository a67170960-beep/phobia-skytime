package ruhack.phobia.system.screensystem.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import ruhack.phobia.system.rendersystem.render2d.Render2D;

public final class ClickGuiEffects {
   private ClickGuiEffects() {
   }

   public static void shadow(DrawContext context, float x, float y, float width, float height, float radius, ClickGuiEffects.ShadowLevel level, float alpha) {
      for (int i = level.steps; i >= 1; i--) {
         float progress = (float)i / level.steps;
         float spread = ClickGuiLayout.physicalToRender(level.spreadPx * progress);
         float offsetY = ClickGuiLayout.physicalToRender(level.offsetYPx * progress);
         float layerAlpha = alpha * (1.0F - progress * 0.62F) / level.steps;
         Render2D.rect(
            context,
            x - spread,
            y - spread + offsetY,
            width + spread * 2.0F,
            height + spread * 2.0F,
            radius + spread,
            ClickGuiTheme.alpha(level.color, layerAlpha),
            true
         );
      }
   }

   public static void accentGlow(DrawContext context, float x, float y, float width, float height, float radius, float alpha) {
      for (int i = 3; i >= 1; i--) {
         float spread = ClickGuiLayout.physicalToRender(2.0F + i * 2.0F);
         float glowX = x - spread;
         float glowW = width + spread * 2.0F;
         Render2D.rect(
            context,
            glowX,
            y - spread,
            glowW,
            height + spread * 2.0F,
            radius + spread,
            true,
            ClickGuiTheme.accentGradient(glowX, glowW, alpha * (0.09F / i) * 0.22F)
         );
      }
   }

   public static void glass(
      DrawContext context,
      float x,
      float y,
      float width,
      float height,
      float radius,
      int color,
      float blur,
      ClickGuiEffects.ShadowLevel shadowLevel,
      float alpha
   ) {
      glass(context, x, y, width, height, radius, color, blur, shadowLevel, alpha, true);
   }

   public static void glass(
      DrawContext context,
      float x,
      float y,
      float width,
      float height,
      float radius,
      int color,
      float blur,
      ClickGuiEffects.ShadowLevel shadowLevel,
      float alpha,
      boolean topHighlight
   ) {
      if (shadowLevel != null) {
         shadow(context, x, y, width, height, radius, shadowLevel, alpha);
      }

      Render2D.blur(context, x, y, width, height, radius, blur, true);
      Render2D.rect(context, x, y, width, height, radius, ClickGuiTheme.alpha(color, alpha), true);
      if (topHighlight) {
         topHighlight(context, x, y, width, radius, alpha * 0.72F);
      }
   }

   public static void logoGlow(DrawContext context, float x, float y, float size, Identifier texture, float alpha) {
      for (int i = 3; i >= 1; i--) {
         float spread = ClickGuiLayout.physicalToRender(i * 2.4F);
         Render2D.image(
            context,
            x - spread,
            y - spread,
            size + spread * 2.0F,
            texture,
            ClickGuiTheme.accentAt(x + size / 2.0F, Math.round(alpha * (0.28F / i) * 52.0F)),
            0.0F,
            true
         );
      }

      Render2D.image(context, x, y, size, texture, ClickGuiTheme.accentAt(x + size / 2.0F, Math.round(alpha * 255.0F)), 0.0F, true);
   }

   public static void topHighlight(DrawContext context, float x, float y, float width, float radius, float alpha) {
      float inset = ClickGuiLayout.physicalToRender(2.0F);
      Render2D.rect(
         context,
         x + inset,
         y + inset,
         width - inset * 2.0F,
         ClickGuiLayout.physicalToRender(1.0F),
         radius,
         radius,
         0.0F,
         0.0F,
         true,
         ClickGuiTheme.alpha(ClickGuiTheme.HIGHLIGHT, alpha)
      );
   }

   public enum ShadowLevel {
      WINDOW(12.0F, 5.0F, 8, ClickGuiTheme.SHADOW_WINDOW),
      FLOATING(8.0F, 4.0F, 6, ClickGuiTheme.SHADOW_FLOATING),
      CARD(3.0F, 2.0F, 3, ClickGuiTheme.SHADOW_CARD);

      private final float spreadPx;
      private final float offsetYPx;
      private final int steps;
      private final int color;

      ShadowLevel(float spreadPx, float offsetYPx, int steps, int color) {
         this.spreadPx = spreadPx;
         this.offsetYPx = offsetYPx;
         this.steps = steps;
         this.color = color;
      }
   }
}
