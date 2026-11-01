package ruhack.phobia.system.rendersystem.render2d.font.msdf;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix4f;
import ruhack.phobia.system.rendersystem.render2d.Render2D;

public class Msdf {
   public static MsdfFont SANSATION;
   public static MsdfFont HAMBURG_BOLD;
   public static MsdfFont HAMBURG_REGULAR;
   public static MsdfFont BOLD;
   public static MsdfFont ICONS;
   private static boolean useFallback = false;

   public static void setFallback(boolean enabled) {
      useFallback = enabled;
   }

   public static boolean hasFonts() {
      MsdfManager.init();
      Msdf2D.init();
      return MsdfManager.getDefault() != null && MsdfManager.getDefault().isLoaded();
   }

   public static void text(DrawContext context, String text, float x, float y, float size, int color) {
      text(context, text, x, y, size, color, false);
   }

   public static void text(DrawContext context, String text, float x, float y, float size, int color, boolean overrideContext) {
      if (!useFallback && hasFonts()) {
         text(context, MsdfManager.getDefault(), text, x, y, size, color, overrideContext);
      } else {
         fallbackText(context, text, x, y, size, color);
      }
   }

   public static void text(DrawContext context, String fontName, String text, float x, float y, float size, int color, boolean overrideContext) {
      MsdfFont font = MsdfManager.get(fontName);
      if (!useFallback && font != null && font.isLoaded()) {
         text(context, font, text, x, y, size, color, overrideContext);
      } else {
         fallbackText(context, text, x, y, size, color);
      }
   }

   public static void text(DrawContext context, MsdfFont font, String text, float x, float y, float size, int color, boolean overrideContext) {
      text(context, font, text, x, y, size, color, 0.0F, overrideContext);
   }

   public static void text(DrawContext context, MsdfFont font, String text, float x, float y, float size, int color, float rotation, boolean overrideContext) {
      if (hasFonts()) {
         if (font == null) {
            font = MsdfManager.getDefault();
         }

         if (overrideContext) {
            MsdfFont finalFont = font;
            Render2D.addOverrideTask(() -> Msdf2D.drawString(Render2D.createProjection(), finalFont, text, x, y, size, color, rotation, 500.0F));
         } else if (font != null && font.isLoaded()) {
            Msdf2D.drawString(Render2D.createProjection(), font, text, x, y, size, color, rotation, 0.0F);
         } else {
            fallbackText(context, text, x, y, size, color);
         }
      }
   }

   public static void text(Matrix4f matrix, String text, float x, float y, float size, int color) {
      Msdf2D.drawString(matrix, MsdfManager.getDefault(), text, x, y, size, color, 0.0F, 0.0F);
   }

   public static void text(Matrix4f matrix, String fontName, String text, float x, float y, float size, int color) {
      Msdf2D.drawString(matrix, MsdfManager.get(fontName), text, x, y, size, color, 0.0F, 0.0F);
   }

   public static void text(Matrix4f matrix, MsdfFont font, String text, float x, float y, float size, int color) {
      Msdf2D.drawString(matrix, font, text, x, y, size, color, 0.0F, 0.0F);
   }

   private static void fallbackText(DrawContext context, String text, float x, float y, float size, int color) {
      MinecraftClient client = MinecraftClient.getInstance();
      context.drawText(client.textRenderer, text, (int)x, (int)y, color, false);
   }

   public static float width(String text, float size) {
      return width(MsdfManager.getDefault(), text, size);
   }

   public static float width(String fontName, String text, float size) {
      return width(MsdfManager.get(fontName), text, size);
   }

   public static float width(MsdfFont font, String text, float size) {
      return font == null ? 0.0F : font.getStringWidth(text, size);
   }

   public static float height(float size) {
      return height(MsdfManager.getDefault(), size);
   }

   public static float height(String fontName, float size) {
      return height(MsdfManager.get(fontName), size);
   }

   public static float height(MsdfFont font, float size) {
      return font == null ? size : font.getLineHeight() * size;
   }

   public static void textCentered(DrawContext context, String text, float x, float y, float size, int color) {
      text(context, text, x - width(text, size) / 2.0F, y, size, color, false);
   }

   public static void textCentered(DrawContext context, String fontName, String text, float x, float y, float size, int color) {
      text(context, fontName, text, x - width(fontName, text, size) / 2.0F, y, size, color, false);
   }

   public static void textRight(DrawContext context, String text, float x, float y, float size, int color) {
      text(context, text, x - width(text, size), y, size, color, false);
   }

   public static void textRight(DrawContext context, String fontName, String text, float x, float y, float size, int color) {
      text(context, fontName, text, x - width(fontName, text, size), y, size, color, false);
   }

   public static void textWithShadow(DrawContext context, String text, float x, float y, float size, int color) {
      text(context, text, x + 1.0F, y + 1.0F, size, 1140850688, false);
      text(context, text, x, y, size, color, false);
   }

   public static void textWithShadow(DrawContext context, String fontName, String text, float x, float y, float size, int color) {
      text(context, fontName, text, x + 1.0F, y + 1.0F, size, 1140850688, false);
      text(context, fontName, text, x, y, size, color, false);
   }

   public static void glowText(
      DrawContext context, String text, float x, float y, float size, int textColor, int glowColor, float glowIntensity, boolean overrideContext
   ) {
      glowText(context, MsdfManager.getDefault(), text, x, y, size, textColor, glowColor, glowIntensity, overrideContext);
   }

   public static void glowText(
      DrawContext context, MsdfFont font, String text, float x, float y, float size, int textColor, int glowColor, float glowIntensity, boolean overrideContext
   ) {
      if (hasFonts()) {
         if (font == null) {
            font = MsdfManager.getDefault();
         }

         if (overrideContext) {
            MsdfFont finalFont = font;
            Render2D.addOverrideTask(
               () -> MsdfGlow2D.drawString(Render2D.createProjection(), finalFont, text, x, y, size, textColor, glowColor, glowIntensity, 500.0F)
            );
         } else if (font != null && font.isLoaded()) {
            MsdfGlow2D.drawString(Render2D.createProjection(), font, text, x, y, size, textColor, glowColor, glowIntensity, 0.0F);
         }
      }
   }

   public static void glowText(
      DrawContext context,
      String fontName,
      String text,
      float x,
      float y,
      float size,
      int textColor,
      int glowColor,
      float glowIntensity,
      boolean overrideContext
   ) {
      MsdfFont font = MsdfManager.get(fontName);
      glowText(context, font, text, x, y, size, textColor, glowColor, glowIntensity, overrideContext);
   }

   public static void textRainbow(DrawContext context, String text, float x, float y, float size, float speed) {
      textRainbow(context, MsdfManager.getDefault(), text, x, y, size, speed, false);
   }

   public static void textRainbow(DrawContext context, MsdfFont font, String text, float x, float y, float size, float speed, boolean overrideContext) {
      if (overrideContext) {
         Render2D.addOverrideTask(() -> textRainbow(context, font, text, x, y, size, speed, false));
      } else if (font != null && !text.isEmpty()) {
         int[] colors = new int[text.length()];
         long time = System.currentTimeMillis();

         for (int i = 0; i < text.length(); i++) {
            float hue = ((float)time * speed / 1000.0F + i * 0.1F) % 1.0F;
            colors[i] = hsbToRgb(hue, 1.0F, 1.0F);
         }

         Msdf2D.drawStringColored(Render2D.createProjection(), font, text, x, y, size, colors, 500.0F);
      }
   }

   public static void textGradient(DrawContext context, String text, float x, float y, float size, int startColor, int endColor) {
      textGradient(context, MsdfManager.getDefault(), text, x, y, size, startColor, endColor, false);
   }

   public static void textGradient(
      DrawContext context, MsdfFont font, String text, float x, float y, float size, int startColor, int endColor, boolean overrideContext
   ) {
      if (overrideContext) {
         Render2D.addOverrideTask(() -> textGradient(context, font, text, x, y, size, startColor, endColor, false));
      } else if (font != null && !text.isEmpty()) {
         int[] colors = new int[text.length()];

         for (int i = 0; i < text.length(); i++) {
            float t = text.length() > 1 ? (float)i / (text.length() - 1) : 0.0F;
            colors[i] = lerpColor(startColor, endColor, t);
         }

         Msdf2D.drawStringColored(Render2D.createProjection(), font, text, x, y, size, colors, 500.0F);
      }
   }

   public static void textColored(DrawContext context, String text, float x, float y, float size, int... colors) {
      textColored(context, MsdfManager.getDefault(), text, x, y, size, false, colors);
   }

   public static void textColored(DrawContext context, MsdfFont font, String text, float x, float y, float size, boolean overrideContext, int... colors) {
      if (overrideContext) {
         Render2D.addOverrideTask(() -> textColored(context, font, text, x, y, size, false, colors));
      } else if (font != null && !text.isEmpty() && colors.length != 0) {
         float z = Render2D.isOverrideActive() ? 500.0F : 0.0F;
         Msdf2D.drawStringColored(Render2D.createProjection(), font, text, x, y, size, colors, z);
      }
   }

   private static int hsbToRgb(float hue, float saturation, float brightness) {
      int r = 0;
      int g = 0;
      int b = 0;
      if (saturation == 0.0F) {
         r = g = b = (int)(brightness * 255.0F + 0.5F);
      } else {
         float h = (hue - (float)Math.floor(hue)) * 6.0F;
         float f = h - (float)Math.floor(h);
         float p = brightness * (1.0F - saturation);
         float q = brightness * (1.0F - saturation * f);
         float t = brightness * (1.0F - saturation * (1.0F - f));
         switch ((int)h) {
            case 0:
               r = (int)(brightness * 255.0F + 0.5F);
               g = (int)(t * 255.0F + 0.5F);
               b = (int)(p * 255.0F + 0.5F);
               break;
            case 1:
               r = (int)(q * 255.0F + 0.5F);
               g = (int)(brightness * 255.0F + 0.5F);
               b = (int)(p * 255.0F + 0.5F);
               break;
            case 2:
               r = (int)(p * 255.0F + 0.5F);
               g = (int)(brightness * 255.0F + 0.5F);
               b = (int)(t * 255.0F + 0.5F);
               break;
            case 3:
               r = (int)(p * 255.0F + 0.5F);
               g = (int)(q * 255.0F + 0.5F);
               b = (int)(brightness * 255.0F + 0.5F);
               break;
            case 4:
               r = (int)(t * 255.0F + 0.5F);
               g = (int)(p * 255.0F + 0.5F);
               b = (int)(brightness * 255.0F + 0.5F);
               break;
            case 5:
               r = (int)(brightness * 255.0F + 0.5F);
               g = (int)(p * 255.0F + 0.5F);
               b = (int)(q * 255.0F + 0.5F);
         }
      }

      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   private static int lerpColor(int c1, int c2, float t) {
      int a1 = c1 >> 24 & 0xFF;
      int r1 = c1 >> 16 & 0xFF;
      int g1 = c1 >> 8 & 0xFF;
      int b1 = c1 & 0xFF;
      int a2 = c2 >> 24 & 0xFF;
      int r2 = c2 >> 16 & 0xFF;
      int g2 = c2 >> 8 & 0xFF;
      int b2 = c2 & 0xFF;
      int a = (int)(a1 + (a2 - a1) * t);
      int r = (int)(r1 + (r2 - r1) * t);
      int g = (int)(g1 + (g2 - g1) * t);
      int b = (int)(b1 + (b2 - b1) * t);
      return a << 24 | r << 16 | g << 8 | b;
   }
}
