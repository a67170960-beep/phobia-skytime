package ruhack.phobia.system.screensystem.clickgui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;

public final class DescriptionPanelComponent {
   public void render(
      DrawContext context,
      ModuleCardBounds bounds,
      float screenWidth,
      float screenHeight,
      float avoidX,
      float avoidY,
      float avoidWidth,
      float avoidHeight,
      float parentAlpha
   ) {
      if (bounds != null && MsdfManager.BOLD != null) {
         ModuleStructure visible = bounds.module();
         String description = ModulePresentation.description(visible);
         float width = ClickGuiLayout.physicalToRender(190.0F);
         float bodySize = ClickGuiLayout.physicalToRender(10.0F);
         float padding = ClickGuiLayout.physicalToRender(10.0F);
         List<String> lines = wrap(description, width - padding * 2.0F, bodySize, MsdfManager.BOLD);
         float margin = ClickGuiLayout.physicalToRender(8.0F);
         float height = Math.min(
            ClickGuiLayout.physicalToRender(220.0F), Math.min(ClickGuiLayout.physicalToRender(48.0F + lines.size() * 13.0F), screenHeight - margin * 2.0F)
         );
         float offset = ClickGuiLayout.physicalToRender(7.0F);
         float x = bounds.x() + bounds.width() + offset;
         float y = bounds.y();
         boolean rightUnavailable = x + width > screenWidth - margin || intersects(x, y, width, height, avoidX, avoidY, avoidWidth, avoidHeight);
         if (rightUnavailable) {
            x = bounds.x() - width - offset;
         }

         if (y + height > screenHeight - margin) {
            y = bounds.y() + bounds.height() - height;
         }

         x = clamp(x, margin, screenWidth - width - margin);
         y = clamp(y, margin, screenHeight - height - margin);
         float a = parentAlpha;
         float radius = ClickGuiLayout.physicalToRender(10.0F);
         ClickGuiEffects.glass(context, x, y, width, height, radius, ClickGuiTheme.FLOATING, 10.0F, ClickGuiEffects.ShadowLevel.FLOATING, a);
         Render2D.outline(
            context, x, y, width, height, radius, ClickGuiLayout.physicalToRender(1.0F), ClickGuiTheme.alpha(ClickGuiTheme.BORDER, a * 0.62F), true
         );
         Msdf.text(
            context,
            MsdfManager.BOLD,
            visible.getName(),
            x + padding,
            y + ClickGuiLayout.physicalToRender(11.0F),
            ClickGuiLayout.physicalToRender(13.0F),
            ClickGuiTheme.alpha(ClickGuiTheme.TEXT, a),
            true
         );
         Msdf.text(
            context,
            MsdfManager.BOLD,
            visible.getCategory().getReadableName(),
            x + padding,
            y + ClickGuiLayout.physicalToRender(29.0F),
            ClickGuiLayout.physicalToRender(9.0F),
            ClickGuiTheme.accentAt(x, Math.round(a * 255.0F)),
            true
         );
         float lineY = y + ClickGuiLayout.physicalToRender(45.0F);

         for (String line : lines) {
            if (lineY + ClickGuiLayout.physicalToRender(11.0F) > y + height) {
               break;
            }

            Msdf.text(context, MsdfManager.BOLD, line, x + padding, lineY, bodySize, ClickGuiTheme.alpha(ClickGuiTheme.TEXT_MUTED, a), true);
            lineY += ClickGuiLayout.physicalToRender(13.0F);
         }
      }
   }

   private static boolean intersects(float x, float y, float width, float height, float otherX, float otherY, float otherWidth, float otherHeight) {
      return otherWidth > 0.0F && otherHeight > 0.0F && x < otherX + otherWidth && x + width > otherX && y < otherY + otherHeight && y + height > otherY;
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private static List<String> wrap(String text, float width, float size, MsdfFont font) {
      List<String> lines = new ArrayList<>();

      for (String paragraph : text.split("\\R")) {
         StringBuilder line = new StringBuilder();

         for (String word : paragraph.trim().split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && Msdf.width(font, candidate, size) > width) {
               lines.add(line.toString());
               line = new StringBuilder(word);
            } else {
               line = new StringBuilder(candidate);
            }
         }

         if (!line.isEmpty()) {
            lines.add(line.toString());
         }
      }

      return lines;
   }
}
