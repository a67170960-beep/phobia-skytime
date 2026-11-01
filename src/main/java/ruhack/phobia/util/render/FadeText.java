package ruhack.phobia.util.render;

import net.minecraft.client.gui.DrawContext;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.render.scissor.Scissor;

public class FadeText {
   public static void render(
      DrawContext context, MsdfFont font, String text, float x, float y, float size, float maxWidth, float fadeWidth, int color, boolean overrideContext
   ) {
      float textWidth = font.getStringWidth(text, size);
      if (textWidth <= maxWidth) {
         Msdf.text(context, font, text, x, y, size, color, overrideContext);
      } else {
         float visibleWidth = maxWidth - fadeWidth;
         Scissor.push(x, y - 2.0F, visibleWidth, size + 4.0F, overrideContext);
         Msdf.text(context, font, text, x, y, size, color, overrideContext);
         Scissor.pop(overrideContext);
         int alpha = color >> 24 & 0xFF;
         float fadeStart = x + visibleWidth;
         Scissor.push(fadeStart, y - 2.0F, fadeWidth, size + 4.0F, overrideContext);
         float step = fadeWidth / 6.0F;

         for (int i = 0; i < 6; i++) {
            float progress = i / 5.0F;
            int fadedAlpha = (int)(alpha * (1.0F - progress));
            int fadedColor = ColorUtil.rgba(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, fadedAlpha);
            float stepX = fadeStart + i * step;
            Scissor.push(stepX, y - 2.0F, step + 0.5F, size + 4.0F, overrideContext);
            Msdf.text(context, font, text, x, y, size, fadedColor, overrideContext);
            Scissor.pop(overrideContext);
         }

         Scissor.pop(overrideContext);
      }
   }

   public static void renderGradient(
      DrawContext context,
      MsdfFont font,
      String text,
      float x,
      float y,
      float size,
      float maxWidth,
      float fadeWidth,
      int color,
      float alphaMultiplier,
      boolean overrideContext
   ) {
      if (font != null) {
         float textWidth = font.getStringWidth(text, size);
         if (textWidth <= maxWidth) {
            Msdf.text(context, font, text, x, y, size, color, overrideContext);
         } else {
            int steps = 8;
            float stepWidth = fadeWidth / steps;
            float solidWidth = maxWidth - fadeWidth;
            int baseAlpha = color >> 24 & 0xFF;
            Scissor.push(x, y - 2.0F, solidWidth, size + 6.0F, overrideContext);
            Msdf.text(context, font, text, x, y, size, color, overrideContext);
            Scissor.pop(overrideContext);

            for (int i = 0; i < steps; i++) {
               float progress = (float)i / (steps - 1);
               int fadedAlpha = (int)(baseAlpha * (1.0F - progress * progress));
               int fadedColor = ColorUtil.rgba(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, fadedAlpha);
               float stepX = x + solidWidth + i * stepWidth;
               Scissor.push(stepX, y - 2.0F, stepWidth + 0.5F, size + 6.0F, overrideContext);
               Msdf.text(context, font, text, x, y, size, fadedColor, overrideContext);
               Scissor.pop(overrideContext);
            }
         }
      }
   }
}
