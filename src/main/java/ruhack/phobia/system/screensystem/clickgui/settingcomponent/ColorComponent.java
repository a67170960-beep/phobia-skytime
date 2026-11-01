package ruhack.phobia.system.screensystem.clickgui.settingcomponent;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import ruhack.phobia.system.modulesystem.setting.implement.ColorSetting;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.system.screensystem.clickgui.ClickGuiEffects;
import ruhack.phobia.system.screensystem.clickgui.ClickGuiLayout;
import ruhack.phobia.system.screensystem.clickgui.ClickGuiTheme;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.interfaces.AbstractSettingComponent;
import ruhack.phobia.util.render.scissor.Scissor;

public final class ColorComponent extends AbstractSettingComponent {
   private static final float HEADER_HEIGHT_PX = 24.0F;
   private static final float PICKER_HEIGHT_PX = 104.0F;
   private static final float HUE_WIDTH_PX = 12.0F;
   private static final float ALPHA_HEIGHT_PX = 10.0F;
   private static final float INNER_GAP_PX = 8.0F;
   private static final float EXPANDED_HEIGHT_PX = 138.0F;
   private final ColorSetting colorSetting;
   private boolean expanded;
   private boolean draggingPicker;
   private boolean draggingHue;
   private boolean draggingAlpha;
   private boolean changedWhileDragging;
   private float expandAnimation;
   private float animatedHue = -1.0F;
   private float animatedSaturation = -1.0F;
   private float animatedBrightness = -1.0F;
   private float animatedAlpha = -1.0F;
   private long lastFrame = System.nanoTime();
   private float boxX;
   private float boxY;
   private float boxWidth;
   private float boxHeight;
   private float pickerX;
   private float pickerY;
   private float pickerWidth;
   private float pickerHeight;
   private float hueX;
   private float hueWidth;
   private float alphaX;
   private float alphaY;
   private float alphaWidth;
   private float alphaHeight;

   public ColorComponent(ColorSetting setting) {
      super(setting);
      this.colorSetting = setting;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   public void render(DrawContext context, float x, float y, float width, int mouseX, int mouseY, float delta) {
      this.x = x;
      this.y = y;
      this.width = width;
      float dt = Math.min((float)(System.nanoTime() - this.lastFrame) / 1.0E9F, 0.1F);
      this.lastFrame = System.nanoTime();
      this.expandAnimation = approach(this.expandAnimation, this.expanded ? 1.0F : 0.0F, dt * 12.0F);
      this.initializeAnimatedColor();
      float colorSpeed = !this.draggingPicker && !this.draggingHue && !this.draggingAlpha ? 10.0F : 18.0F;
      this.animatedHue = approach(this.animatedHue, this.colorSetting.getHue(), dt * colorSpeed);
      this.animatedSaturation = approach(this.animatedSaturation, this.colorSetting.getSaturation(), dt * colorSpeed);
      this.animatedBrightness = approach(this.animatedBrightness, this.colorSetting.getBrightness(), dt * colorSpeed);
      this.animatedAlpha = approach(this.animatedAlpha, this.colorSetting.getAlpha(), dt * colorSpeed);
      this.boxX = x;
      this.boxY = y;
      this.boxWidth = width;
      float headerHeight = ClickGuiLayout.physicalToRender(24.0F);
      float expandedHeight = ClickGuiLayout.physicalToRender(138.0F) * this.expandAnimation;
      this.boxHeight = headerHeight + expandedHeight;
      this.height = this.boxY - y + this.boxHeight;
      float radius = ClickGuiLayout.physicalToRender(7.0F);
      ClickGuiEffects.shadow(
         context, this.boxX, this.boxY, this.boxWidth, this.boxHeight, radius, ClickGuiEffects.ShadowLevel.CARD, this.alphaMultiplier * 0.55F
      );
      Render2D.rect(
         context, this.boxX, this.boxY, this.boxWidth, this.boxHeight, radius, ClickGuiTheme.alpha(ClickGuiTheme.GLASS_INSET, this.alphaMultiplier), true
      );
      Render2D.outline(
         context,
         this.boxX,
         this.boxY,
         this.boxWidth,
         this.boxHeight,
         radius,
         ClickGuiLayout.physicalToRender(1.0F),
         ClickGuiTheme.alpha(ClickGuiTheme.BORDER, this.alphaMultiplier * 0.68F),
         true
      );
      this.renderHeader(context, headerHeight);
      if (this.expandAnimation > 0.01F) {
         Scissor.push(this.boxX, this.boxY + headerHeight, this.boxWidth, expandedHeight, true);
         this.renderExpanded(context, headerHeight, this.alphaMultiplier * this.expandAnimation);
         Scissor.pop(true);
      }

      if (this.draggingPicker) {
         this.updatePicker(mouseX, mouseY);
      } else if (this.draggingHue) {
         this.updateHue(mouseY);
      } else if (this.draggingAlpha) {
         this.updateAlpha(mouseX);
      }
   }

   private void renderHeader(DrawContext context, float headerHeight) {
      float padding = ClickGuiLayout.physicalToRender(8.0F);
      Msdf.text(
         context,
         MsdfManager.BOLD,
         this.colorSetting.getName(),
         this.boxX,
         this.boxY + ClickGuiLayout.physicalToRender(7.0F),
         ClickGuiLayout.physicalToRender(10.0F),
         ClickGuiTheme.alpha(ClickGuiTheme.TEXT, this.alphaMultiplier),
         true
      );
      float swatchSize = ClickGuiLayout.physicalToRender(14.0F);
      float swatchX = this.boxX + this.boxWidth - ClickGuiLayout.physicalToRender(82.0F);
      float swatchY = this.boxY + (headerHeight - swatchSize) / 2.0F;
      int color = this.colorSetting.getColorWithAlpha();
      this.drawChecker(context, swatchX, swatchY, swatchSize, swatchSize, ClickGuiLayout.physicalToRender(4.0F), this.alphaMultiplier);
      Render2D.rect(
         context, swatchX, swatchY, swatchSize, swatchSize, ClickGuiLayout.physicalToRender(5.0F), ColorUtil.multiplyAlpha(color, this.alphaMultiplier), true
      );
      Render2D.outline(
         context,
         swatchX,
         swatchY,
         swatchSize,
         swatchSize,
         ClickGuiLayout.physicalToRender(5.0F),
         ClickGuiLayout.physicalToRender(1.0F),
         ClickGuiTheme.alpha(ClickGuiTheme.HIGHLIGHT, this.alphaMultiplier),
         true
      );
      String hex = this.colorSetting.getHexString();
      Msdf.text(
         context,
         MsdfManager.BOLD,
         hex,
         swatchX + swatchSize + ClickGuiLayout.physicalToRender(6.0F),
         this.boxY + ClickGuiLayout.physicalToRender(7.0F),
         ClickGuiLayout.physicalToRender(9.0F),
         ClickGuiTheme.alpha(ClickGuiTheme.TEXT, this.alphaMultiplier),
         true
      );
      float arrowSize = ClickGuiLayout.physicalToRender(8.0F);
      float arrowX = this.boxX + this.boxWidth - padding - arrowSize;
      Msdf.text(
         context,
         MsdfManager.ICONS,
         "p",
         arrowX,
         this.boxY + ClickGuiLayout.physicalToRender(8.0F),
         arrowSize,
         this.expanded
            ? ClickGuiTheme.accentAt(this.x + this.width, Math.round(this.alphaMultiplier * 255.0F))
            : ClickGuiTheme.alpha(ClickGuiTheme.TEXT_MUTED, this.alphaMultiplier),
         -45.0F + 90.0F * this.expandAnimation,
         true
      );
   }

   private void renderExpanded(DrawContext context, float headerHeight, float alpha) {
      float padding = ClickGuiLayout.physicalToRender(8.0F);
      float gap = ClickGuiLayout.physicalToRender(8.0F);
      this.pickerX = this.boxX + padding;
      this.pickerY = this.boxY + headerHeight + padding;
      this.pickerHeight = ClickGuiLayout.physicalToRender(104.0F);
      this.hueWidth = ClickGuiLayout.physicalToRender(12.0F);
      this.hueX = this.boxX + this.boxWidth - padding - this.hueWidth;
      this.pickerWidth = this.hueX - gap - this.pickerX;
      float pickerRadius = ClickGuiLayout.physicalToRender(6.0F);
      Render2D.colorPicker(context, this.pickerX, this.pickerY, this.pickerWidth, this.pickerHeight, this.colorSetting.getHue(), pickerRadius, true, alpha);
      Render2D.outline(
         context,
         this.pickerX,
         this.pickerY,
         this.pickerWidth,
         this.pickerHeight,
         pickerRadius,
         ClickGuiLayout.physicalToRender(1.0F),
         ClickGuiTheme.alpha(ClickGuiTheme.BORDER, alpha * 0.9F),
         true
      );
      this.drawPickerIndicator(context, alpha);
      Render2D.hueBar(context, this.hueX, this.pickerY, this.hueWidth, this.pickerHeight, this.hueWidth / 2.0F, true, alpha);
      this.drawHueIndicator(context, alpha);
      this.alphaX = this.pickerX;
      this.alphaY = this.pickerY + this.pickerHeight + gap;
      this.alphaWidth = this.boxWidth - padding * 2.0F;
      this.alphaHeight = ClickGuiLayout.physicalToRender(10.0F);
      this.drawChecker(context, this.alphaX, this.alphaY, this.alphaWidth, this.alphaHeight, ClickGuiLayout.physicalToRender(5.0F), alpha);
      int rgb = this.colorSetting.getColorNoAlpha();
      int transparent = ColorUtil.withAlpha(rgb, 0);
      int opaque = ColorUtil.withAlpha(rgb, (int)(255.0F * alpha));
      Render2D.rect(
         context, this.alphaX, this.alphaY, this.alphaWidth, this.alphaHeight, this.alphaHeight / 2.0F, true, transparent, opaque, opaque, transparent
      );
      this.drawAlphaIndicator(context, alpha);
   }

   private void drawPickerIndicator(DrawContext context, float alpha) {
      float radius = ClickGuiLayout.physicalToRender(4.0F);
      float centerX = this.pickerX + radius + this.animatedSaturation * (this.pickerWidth - radius * 2.0F);
      float centerY = this.pickerY + radius + (1.0F - this.animatedBrightness) * (this.pickerHeight - radius * 2.0F);
      Render2D.rect(context, centerX - radius, centerY - radius, radius * 2.0F, radius * 2.0F, radius, ClickGuiTheme.alpha(-1, alpha), true);
      float inner = ClickGuiLayout.physicalToRender(2.0F);
      Render2D.rect(
         context,
         centerX - inner,
         centerY - inner,
         inner * 2.0F,
         inner * 2.0F,
         inner,
         ColorUtil.multiplyAlpha(this.colorSetting.getColorNoAlpha(), alpha),
         true
      );
      Render2D.outline(
         context,
         centerX - radius,
         centerY - radius,
         radius * 2.0F,
         radius * 2.0F,
         radius,
         ClickGuiLayout.physicalToRender(1.0F),
         ClickGuiTheme.alpha(-1342177280, alpha),
         true
      );
   }

   private void drawHueIndicator(DrawContext context, float alpha) {
      float indicatorHeight = ClickGuiLayout.physicalToRender(4.0F);
      float y = this.pickerY + indicatorHeight / 2.0F + this.animatedHue * (this.pickerHeight - indicatorHeight);
      Render2D.rect(
         context,
         this.hueX - ClickGuiLayout.physicalToRender(2.0F),
         y - indicatorHeight / 2.0F,
         this.hueWidth + ClickGuiLayout.physicalToRender(4.0F),
         indicatorHeight,
         indicatorHeight / 2.0F,
         ClickGuiTheme.alpha(-1, alpha),
         true
      );
      Render2D.outline(
         context,
         this.hueX - ClickGuiLayout.physicalToRender(2.0F),
         y - indicatorHeight / 2.0F,
         this.hueWidth + ClickGuiLayout.physicalToRender(4.0F),
         indicatorHeight,
         indicatorHeight / 2.0F,
         ClickGuiLayout.physicalToRender(1.0F),
         ClickGuiTheme.alpha(-1879048192, alpha),
         true
      );
   }

   private void drawAlphaIndicator(DrawContext context, float alpha) {
      float radius = ClickGuiLayout.physicalToRender(5.0F);
      float centerX = this.alphaX + radius + this.animatedAlpha * (this.alphaWidth - radius * 2.0F);
      float centerY = this.alphaY + this.alphaHeight / 2.0F;
      Render2D.rect(context, centerX - radius, centerY - radius, radius * 2.0F, radius * 2.0F, radius, ClickGuiTheme.alpha(-1, alpha), true);
      Render2D.outline(
         context,
         centerX - radius,
         centerY - radius,
         radius * 2.0F,
         radius * 2.0F,
         radius,
         ClickGuiLayout.physicalToRender(1.0F),
         ClickGuiTheme.alpha(-1879048192, alpha),
         true
      );
   }

   private void drawChecker(DrawContext context, float x, float y, float width, float height, float tile, float alpha) {
      Render2D.rect(context, x, y, width, height, Math.min(tile, height / 2.0F), ClickGuiTheme.alpha(-4737091, alpha), true);
      float horizontalInset = Math.min(width / 2.0F, height / 2.0F);
      float innerX = x + horizontalInset;
      float innerWidth = Math.max(0.0F, width - horizontalInset * 2.0F);
      int columns = (int)Math.ceil(innerWidth / tile);
      int rows = Math.max(1, (int)Math.ceil(height / tile));

      for (int row = 0; row < rows; row++) {
         for (int column = 0; column < columns; column++) {
            if ((row + column) % 2 == 0) {
               float tileX = innerX + column * tile;
               float tileY = y + row * tile;
               Render2D.rect(
                  context,
                  tileX,
                  tileY,
                  Math.min(tile, innerX + innerWidth - tileX),
                  Math.min(tile, y + height - tileY),
                  0.0F,
                  ClickGuiTheme.alpha(-8947840, alpha),
                  true
               );
            }
         }
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return false;
      }

      float headerHeight = ClickGuiLayout.physicalToRender(24.0F);
      if (isInside(mouseX, mouseY, this.boxX, this.boxY, this.boxWidth, headerHeight)) {
         this.expanded = !this.expanded;
         return true;
      }

      if (this.expanded && this.expandAnimation > 0.65F) {
         if (isInside(mouseX, mouseY, this.pickerX, this.pickerY, this.pickerWidth, this.pickerHeight)) {
            this.draggingPicker = true;
            this.changedWhileDragging = true;
            this.updatePicker(mouseX, mouseY);
            return true;
         }

         if (isInside(mouseX, mouseY, this.hueX, this.pickerY, this.hueWidth, this.pickerHeight)) {
            this.draggingHue = true;
            this.changedWhileDragging = true;
            this.updateHue(mouseY);
            return true;
         }

         if (isInside(mouseX, mouseY, this.alphaX, this.alphaY, this.alphaWidth, this.alphaHeight)) {
            this.draggingAlpha = true;
            this.changedWhileDragging = true;
            this.updateAlpha(mouseX);
            return true;
         }

         if (!isInside(mouseX, mouseY, this.boxX, this.boxY, this.boxWidth, this.boxHeight)) {
            this.expanded = false;
         }
      }

      return false;
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (this.changedWhileDragging && (this.draggingPicker || this.draggingHue || this.draggingAlpha)) {
         this.colorSetting.addToPreviousColors();
      }

      this.changedWhileDragging = false;
      this.draggingPicker = false;
      this.draggingHue = false;
      this.draggingAlpha = false;
      return false;
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.draggingPicker) {
         this.updatePicker(mouseX, mouseY);
         return true;
      } else if (this.draggingHue) {
         this.updateHue(mouseY);
         return true;
      } else if (this.draggingAlpha) {
         this.updateAlpha(mouseX);
         return true;
      } else {
         return false;
      }
   }

   private void updatePicker(double mouseX, double mouseY) {
      this.colorSetting.setSaturation(clamp((float)((mouseX - this.pickerX) / this.pickerWidth)));
      this.colorSetting.setBrightness(1.0F - clamp((float)((mouseY - this.pickerY) / this.pickerHeight)));
   }

   private void updateHue(double mouseY) {
      this.colorSetting.setHue(clamp((float)((mouseY - this.pickerY) / this.pickerHeight)));
   }

   private void updateAlpha(double mouseX) {
      this.colorSetting.setAlpha(clamp((float)((mouseX - this.alphaX) / this.alphaWidth)));
   }

   public float getHeight() {
      return ClickGuiLayout.physicalToRender(24.0F + 138.0F * this.expandAnimation);
   }

   public boolean isExpanded() {
      return this.expanded;
   }

   @Override
   public void tick() {
   }

   private void initializeAnimatedColor() {
      if (!(this.animatedHue >= 0.0F)) {
         this.animatedHue = this.colorSetting.getHue();
         this.animatedSaturation = this.colorSetting.getSaturation();
         this.animatedBrightness = this.colorSetting.getBrightness();
         this.animatedAlpha = this.colorSetting.getAlpha();
      }
   }

   private static float approach(float current, float target, float amount) {
      return current + (target - current) * Math.min(1.0F, amount);
   }

   private static float clamp(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   private static boolean isInside(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   private static List<String> wrapText(String text, float maxWidth, float size, MsdfFont font) {
      List<String> lines = new ArrayList<>();
      if (text != null && !text.isBlank()) {
         StringBuilder line = new StringBuilder();

         for (String word : text.trim().split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && Msdf.width(font, candidate, size) > maxWidth) {
               lines.add(line.toString());
               line = new StringBuilder(word);
            } else {
               line = new StringBuilder(candidate);
            }
         }

         if (!line.isEmpty()) {
            lines.add(line.toString());
         }

         return lines;
      } else {
         return lines;
      }
   }
}
