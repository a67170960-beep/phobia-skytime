package ruhack.phobia.util.render.scissor;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.util.colorschem.ColorUtil;

public class TextScissor {
   private static final Map<String, Float> scrollOffsets = new HashMap<>();
   private static final Map<String, Long> lastUpdateTimes = new HashMap<>();

   public static void render(
      DrawContext context,
      MsdfFont font,
      String text,
      float x,
      float y,
      float fontSize,
      float maxWidth,
      float fadeWidth,
      int textColor,
      int bgColor,
      boolean scaled,
      String key,
      float mouseX,
      float mouseY,
      float areaHeight
   ) {
      if (font != null && text != null && !text.isEmpty()) {
         float textWidth = Msdf.width(font, text, fontSize);
         if (textWidth <= maxWidth) {
            Msdf.text(context, font, text, x, y, fontSize, textColor, scaled);
         } else {
            long now = System.currentTimeMillis();
            long lastTime = lastUpdateTimes.getOrDefault(key, now);
            float dt = Math.min((float)(now - lastTime) / 1000.0F, 0.05F);
            lastUpdateTimes.put(key, now);
            float currentOffset = scrollOffsets.getOrDefault(key, 0.0F);
            boolean hovered = mouseX >= x && mouseX <= x + maxWidth && mouseY >= y - 2.0F && mouseY <= y + areaHeight + 2.0F;
            float overflow = textWidth - maxWidth;
            float targetOffset = 0.0F;
            if (hovered) {
               targetOffset = -(overflow + 4.0F);
            }

            float hoverSpeed = Math.max(0.8F, 3.0F - overflow / 60.0F);
            float returnSpeed = Math.max(1.5F, 5.0F - overflow / 80.0F);
            float speed = hovered ? hoverSpeed : returnSpeed;
            currentOffset += (targetOffset - currentOffset) * Math.min(dt * speed, 1.0F);
            if (Math.abs(currentOffset - targetOffset) < 0.05F) {
               currentOffset = targetOffset;
            }

            scrollOffsets.put(key, currentOffset);
            float renderX = x + currentOffset;
            Scissor.push(x, y - 2.0F, maxWidth, fontSize + 6.0F, scaled);
            Msdf.text(context, font, text, renderX, y, fontSize, textColor, scaled);
            Scissor.pop(scaled);
            int bgR = bgColor >> 16 & 0xFF;
            int bgG = bgColor >> 8 & 0xFF;
            int bgB = bgColor & 0xFF;
            int bgA = bgColor >> 24 & 0xFF;
            int transparent = ColorUtil.rgba(bgR, bgG, bgB, 0);
            int opaque = ColorUtil.rgba(bgR, bgG, bgB, bgA);
            float fadeX = x + maxWidth - fadeWidth;
            float fadeY = y - 2.0F;
            float fadeH = fontSize + 6.0F;
            Render2D.rect(context, fadeX, fadeY, fadeWidth, fadeH, 0.0F, 0.0F, 0.0F, 0.0F, scaled, transparent, opaque, opaque, transparent);
         }
      }
   }

   public static void render(
      DrawContext context,
      MsdfFont font,
      String text,
      float x,
      float y,
      float fontSize,
      float maxWidth,
      int textColor,
      int bgColor,
      boolean scaled,
      String key,
      float mouseX,
      float mouseY,
      float areaHeight
   ) {
      render(context, font, text, x, y, fontSize, maxWidth, 14.0F, textColor, bgColor, scaled, key, mouseX, mouseY, areaHeight);
   }

   public static void render(
      DrawContext context,
      MsdfFont font,
      String text,
      float x,
      float y,
      float fontSize,
      float maxWidth,
      float fadeWidth,
      int textColor,
      int bgColor,
      boolean scaled
   ) {
      if (font != null && text != null && !text.isEmpty()) {
         float textWidth = Msdf.width(font, text, fontSize);
         if (textWidth <= maxWidth) {
            Msdf.text(context, font, text, x, y, fontSize, textColor, scaled);
         } else {
            Scissor.push(x, y - 2.0F, maxWidth, fontSize + 6.0F, scaled);
            Msdf.text(context, font, text, x, y, fontSize, textColor, scaled);
            Scissor.pop(scaled);
            int bgR = bgColor >> 16 & 0xFF;
            int bgG = bgColor >> 8 & 0xFF;
            int bgB = bgColor & 0xFF;
            int bgA = bgColor >> 24 & 0xFF;
            int transparent = ColorUtil.rgba(bgR, bgG, bgB, 0);
            int opaque = ColorUtil.rgba(bgR, bgG, bgB, bgA);
            float fadeX = x + maxWidth - fadeWidth;
            float fadeY = y - 2.0F;
            float fadeH = fontSize + 6.0F;
            Render2D.rect(context, fadeX, fadeY, fadeWidth, fadeH, 0.0F, 0.0F, 0.0F, 0.0F, scaled, transparent, opaque, opaque, transparent);
         }
      }
   }

   public static void cleanup(String key) {
      scrollOffsets.remove(key);
      lastUpdateTimes.remove(key);
   }
}
