package ruhack.phobia.system.dragsystem;

import net.minecraft.client.gui.DrawContext;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfGlyph;

public abstract class PillHudElement extends AbstractHudElement {
   protected static final float HEIGHT = 21.0F;
   protected static final float PADDING = 9.0F;
   protected static final float GAP = 4.0F;
   protected static final float DOT_GAP = 6.0F;
   protected static final float DOT_SIZE = 2.0F;
   protected static final float ICON_VISUAL = 6.5F;
   protected static final float TEXT_SIZE = 7.5F;
   protected static final float UNIT_SIZE = 5.0F;
   protected static final float BLUR_STRENGTH = 8.0F;
   protected static final float ROUNDING = 8.0F;

   public PillHudElement(String name, int x, int y, boolean draggable) {
      super(name, x, y, 120, 21, draggable);
   }

   @Override
   public float getRoundingRadius() {
      return 8.0F;
   }

   protected void drawPill(DrawContext context, int bgColor, int outlineColor) {
      Render2D.blur(context, this.x, this.y, this.width, 21.0F, 8.0F, 8.0F, false);
      Render2D.rect(context, this.x, this.y, this.width, 21.0F, 8.0F, bgColor, false);
      Render2D.outline(context, this.x, this.y, this.width, 21.0F, 8.0F, 0.6F, outlineColor, false);
   }

   protected float drawSegment(
      DrawContext context, MsdfFont font, MsdfFont icons, float cursor, String icon, String text, String unit, int iconColor, int textColor, int unitColor
   ) {
      float size = this.iconSize(icons, icon, 6.5F);
      Msdf.text(context, icons, icon, cursor, this.glyphY(icons, icon, size), size, iconColor, false);
      cursor += Msdf.width(icons, icon, size) + 4.0F;
      float textY = this.textY(font, 7.5F);
      Msdf.text(context, font, text, cursor, textY, 7.5F, textColor, false);
      cursor += Msdf.width(font, text, 7.5F);
      if (unit != null) {
         Msdf.text(context, font, unit, cursor + 1.0F, this.unitY(font, textY), 5.0F, unitColor, false);
         cursor += 1.0F + Msdf.width(font, unit, 5.0F);
      }

      return cursor;
   }

   protected float segmentWidth(MsdfFont font, MsdfFont icons, String icon, String text, String unit) {
      float size = this.iconSize(icons, icon, 6.5F);
      float w = Msdf.width(icons, icon, size) + 4.0F + Msdf.width(font, text, 7.5F);
      if (unit != null) {
         w += 1.0F + Msdf.width(font, unit, 5.0F);
      }

      return w;
   }

   protected float drawDot(DrawContext context, float cursor, int color) {
      return this.drawDot(context, cursor, this.y + 10.5F, color);
   }

   protected float drawDot(DrawContext context, float cursor, float centerY, int color) {
      float dotX = cursor + 6.0F;
      float dotY = centerY - 1.0F;
      Render2D.rect(context, dotX, dotY, 2.0F, 2.0F, 1.0F, color, false);
      return dotX + 2.0F + 6.0F;
   }

   protected float dotBlockWidth() {
      return 14.0F;
   }

   protected float iconSize(MsdfFont icons, String icon, float visual) {
      if (icons == null) {
         return visual;
      }

      MsdfGlyph glyph = icons.getGlyph(icon.charAt(0));
      return glyph != null && !(glyph.height <= 0.0F) ? visual * icons.getEmSize() / glyph.height : visual;
   }

   protected float glyphY(MsdfFont icons, String icon, float size) {
      return this.glyphY(icons, icon, size, this.y + 10.5F);
   }

   protected float glyphY(MsdfFont icons, String icon, float size, float centerY) {
      if (icons == null) {
         return centerY - size / 2.0F;
      } else {
         MsdfGlyph glyph = icons.getGlyph(icon.charAt(0));
         if (glyph != null && !(glyph.height <= 0.0F)) {
            float scale = size / icons.getEmSize();
            return centerY - (icons.getAscender() - glyph.bearingY + glyph.height / 2.0F) * scale;
         } else {
            return centerY - size / 2.0F;
         }
      }
   }

   protected float textY(MsdfFont font, float size) {
      return this.textY(font, size, this.y + 10.5F);
   }

   protected float textY(MsdfFont font, float size, float centerY) {
      if (font == null) {
         return centerY - size / 2.0F;
      } else {
         MsdfGlyph ref = font.getGlyph(48);
         if (ref != null && !(ref.height <= 0.0F)) {
            float scale = size / font.getEmSize();
            return centerY - (font.getAscender() - ref.bearingY + ref.height / 2.0F) * scale;
         } else {
            return centerY - font.getLineHeight() * size / 2.0F;
         }
      }
   }

   protected float unitY(MsdfFont font, float textDrawY) {
      if (font == null) {
         return textDrawY + 7.5F - 5.0F;
      }

      float ascender = font.getAscender() / font.getEmSize();
      return textDrawY + ascender * 2.5F;
   }
}
