package ruhack.phobia.system.modulesystem.impl.display;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import ruhack.phobia.system.dragsystem.PillHudElement;
import ruhack.phobia.system.modulesystem.impl.player.AutoWarden;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.util.colorschem.ColorUtil;

public class AutoWardenHud extends PillHudElement {
   private static final float PAD = 8.0F;
   private static final float RADIUS = 6.0F;
   private static final float TITLE_SIZE = 8.0F;
   private static final float LINE_SIZE = 6.5F;
   private static final float LINE_GAP = 3.0F;

   public AutoWardenHud() {
      super("AutoWarden", 150, 94, true);
   }

   @Override
   public float getRoundingRadius() {
      return 6.0F;
   }

   @Override
   public boolean visible() {
      if (this.mc.player != null && !this.mc.options.hudHidden) {
         AutoWarden module = AutoWarden.getInstance();
         return module != null && module.isState() && module.showHudPanel();
      } else {
         return false;
      }
   }

   @Override
   public void drawDraggable(DrawContext context, int alpha) {
      AutoWarden module = AutoWarden.getInstance();
      if (module != null) {
         MsdfFont font = MsdfManager.BOLD;
         String title = "Auto Warden";
         List<String> lines = module.hudLines();
         float a = alpha / 255.0F;
         float contentWidth = Msdf.width(font, title, 8.0F);
         float contentHeight = 12.0F;

         for (String line : lines) {
            contentWidth = Math.max(contentWidth, Msdf.width(font, line, 6.5F));
            contentHeight += 9.5F;
         }

         this.width = (int)Math.ceil(contentWidth + 16.0F);
         this.height = (int)Math.ceil(contentHeight + 16.0F);
         int bgColor = ColorUtil.multAlpha(ColorUtil.rgba(11, 12, 18, Math.round(Hud.getInstance().backgroundAlpha.getValue())), a);
         int outlineColor = ColorUtil.multAlpha(ColorUtil.rgba(255, 255, 255, 20), a);
         int titleColor = ColorUtil.multAlpha(module.isBlockedPhase() ? ColorUtil.rgba(255, 77, 103, 255) : ColorUtil.rgba(255, 90, 120, 255), a);
         int textColor = ColorUtil.multAlpha(ColorUtil.rgba(231, 231, 231, 255), a);
         Render2D.blur(context, this.x, this.y, this.width, this.height, 6.0F, 8.0F, false);
         Render2D.rect(context, this.x, this.y, this.width, this.height, 6.0F, bgColor, false);
         Render2D.outline(context, this.x, this.y, this.width, this.height, 6.0F, 0.6F, outlineColor, false);
         float cursorY = this.y + 8.0F;
         Msdf.text(context, font, title, this.x + 8.0F, cursorY, 8.0F, titleColor, false);
         cursorY += 12.0F;

         for (String line : lines) {
            Msdf.text(context, font, line, this.x + 8.0F, cursorY, 6.5F, textColor, false);
            cursorY += 9.5F;
         }
      }
   }
}
