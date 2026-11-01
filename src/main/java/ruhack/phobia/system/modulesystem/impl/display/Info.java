package ruhack.phobia.system.modulesystem.impl.display;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import ruhack.phobia.system.dragsystem.PillHudElement;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.util.colorschem.ColorUtil;

public class Info extends PillHudElement {
   private static final String ICON_INFO = "\ue00b";
   private static final String ICON_COORDS = "A";
   private static final String ICON_BPS = "\ue011";
   private static final float INFO_VISUAL = 8.0F;
   private static final float COORD_GROUP_GAP = 3.0F;
   private static final int STACK_GAP = 4;
   private final Logotype logotype;
   private final String[] coords = new String[]{"0", "0", "0"};
   private int lastCoordX = Integer.MIN_VALUE;
   private int lastCoordY = Integer.MIN_VALUE;
   private int lastCoordZ = Integer.MIN_VALUE;

   public Info(Logotype logotype) {
      super("Info", 6, 31, false);
      this.logotype = logotype;
   }

   @Override
   public void drawDraggable(DrawContext context, int alpha) {
      float a = alpha / 255.0F;
      MsdfFont font = MsdfManager.BOLD;
      MsdfFont icons = MsdfManager.ICONS;
      this.updateCoords();
      String bps = String.valueOf(this.getBps());
      Hud hud = Hud.getInstance();
      boolean showBps = hud != null && hud.showBps.isValue();
      float infoSize = this.iconSize(icons, "\ue00b", 8.0F);
      float contentWidth = Msdf.width(icons, "\ue00b", infoSize) + this.dotBlockWidth() + this.coordsWidth(font, icons);
      if (showBps) {
         contentWidth += this.dotBlockWidth() + this.segmentWidth(font, icons, "\ue011", bps, "B/S");
      }

      this.width = (int)Math.ceil(contentWidth + 18.0F);
      this.height = 21;
      this.alignToLogotype();
      int accent = ColorUtil.multAlpha(ColorUtil.getClientColorAt((float)this.x / Math.max(1, Render2D.getFixedScaledWidth())), a);
      int textColor = ColorUtil.multAlpha(ColorUtil.rgba(236, 238, 245, 255), a);
      int unitColor = ColorUtil.multAlpha(ColorUtil.getClientColorAt((float)(this.x + this.width) / Math.max(1, Render2D.getFixedScaledWidth()), 200), a);
      int dotColor = ColorUtil.multAlpha(ColorUtil.rgba(255, 255, 255, 70), a);
      int bgColor = ColorUtil.multAlpha(ColorUtil.rgba(11, 12, 18, Math.round(Hud.getInstance().backgroundAlpha.getValue())), a);
      int outlineColor = ColorUtil.multAlpha(ColorUtil.rgba(255, 255, 255, 20), a);
      this.drawPill(context, bgColor, outlineColor);
      float cursor = this.x + 9.0F;
      Msdf.text(context, icons, "\ue00b", cursor, this.glyphY(icons, "\ue00b", infoSize), infoSize, accent, false);
      cursor += Msdf.width(icons, "\ue00b", infoSize);
      cursor = this.drawDot(context, cursor, dotColor);
      cursor = this.drawCoords(context, font, icons, cursor, accent, textColor, unitColor);
      if (showBps) {
         cursor = this.drawDot(context, cursor, dotColor);
         this.drawSegment(context, font, icons, cursor, "\ue011", bps, "B/S", accent, textColor, unitColor);
      }
   }

   private void alignToLogotype() {
      this.y = this.logotype.getVisualY() + this.logotype.getHeight() + 4;
      if (Render2D.hasUsableViewport()) {
         float viewportWidth = Render2D.getFixedScaledWidth() / Math.max(0.01F, Render2D.getContextScale());
         float logotypeCenter = this.logotype.getX() + this.logotype.getWidth() / 2.0F;
         if (logotypeCenter < viewportWidth / 3.0F) {
            this.x = this.logotype.getX();
         } else if (logotypeCenter > viewportWidth * 2.0F / 3.0F) {
            this.x = this.logotype.getX() + this.logotype.getWidth() - this.width;
         } else {
            this.x = Math.round(logotypeCenter - this.width / 2.0F);
         }

         int margin = 6;
         this.x = Math.max(margin, Math.min(this.x, (int)Math.floor(viewportWidth - this.width - margin)));
      }
   }

   private float drawCoords(DrawContext context, MsdfFont font, MsdfFont icons, float cursor, int iconColor, int textColor, int unitColor) {
      float size = this.iconSize(icons, "A", 6.5F);
      Msdf.text(context, icons, "A", cursor, this.glyphY(icons, "A", size), size, iconColor, false);
      cursor += Msdf.width(icons, "A", size) + 4.0F;
      float textY = this.textY(font, 7.5F);
      float unitY = this.unitY(font, textY);

      for (int i = 0; i < this.coords.length; i++) {
         String unit = switch (i) {
            case 0 -> "x";
            case 1 -> "y";
            default -> "z";
         };
         Msdf.text(context, font, this.coords[i], cursor, textY, 7.5F, textColor, false);
         cursor += Msdf.width(font, this.coords[i], 7.5F);
         Msdf.text(context, font, unit, cursor + 1.0F, unitY, 5.0F, unitColor, false);
         cursor += 1.0F + Msdf.width(font, unit, 5.0F);
         if (i < this.coords.length - 1) {
            cursor += 3.0F;
         }
      }

      return cursor;
   }

   private float coordsWidth(MsdfFont font, MsdfFont icons) {
      float size = this.iconSize(icons, "A", 6.5F);
      float w = Msdf.width(icons, "A", size) + 4.0F;

      for (int i = 0; i < this.coords.length; i++) {
         String unit = switch (i) {
            case 0 -> "x";
            case 1 -> "y";
            default -> "z";
         };
         w += Msdf.width(font, this.coords[i], 7.5F) + 1.0F + Msdf.width(font, unit, 5.0F);
         if (i < this.coords.length - 1) {
            w += 3.0F;
         }
      }

      return w;
   }

   private void updateCoords() {
      if (this.mc.player == null) {
         this.coords[0] = this.coords[1] = this.coords[2] = "0";
      } else {
         BlockPos pos = this.mc.player.getBlockPos();
         if (pos.getX() != this.lastCoordX) {
            this.lastCoordX = pos.getX();
            this.coords[0] = Integer.toString(this.lastCoordX);
         }

         if (pos.getY() != this.lastCoordY) {
            this.lastCoordY = pos.getY();
            this.coords[1] = Integer.toString(this.lastCoordY);
         }

         if (pos.getZ() != this.lastCoordZ) {
            this.lastCoordZ = pos.getZ();
            this.coords[2] = Integer.toString(this.lastCoordZ);
         }
      }
   }

   private int getBps() {
      if (this.mc.player == null) {
         return 0;
      }

      double dx = this.mc.player.getX() - this.mc.player.lastX;
      double dz = this.mc.player.getZ() - this.mc.player.lastZ;
      return (int)Math.round(Math.sqrt(dx * dx + dz * dz) * 20.0);
   }
}
