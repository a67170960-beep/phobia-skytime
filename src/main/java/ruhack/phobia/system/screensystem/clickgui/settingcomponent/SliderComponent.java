package ruhack.phobia.system.screensystem.clickgui.settingcomponent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.minecraft.client.gui.DrawContext;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.system.screensystem.clickgui.ClickGuiLayout;
import ruhack.phobia.system.screensystem.clickgui.ClickGuiTheme;

public final class SliderComponent {
   private static final float HEIGHT_PX = 34.0F;
   private final SliderSettings setting;
   private float x;
   private float y;
   private float width;
   private boolean dragging;
   private float animatedPercent = -1.0F;
   private long lastFrame = System.nanoTime();

   public SliderComponent(SliderSettings setting) {
      this.setting = setting;
   }

   public SliderSettings getSetting() {
      return this.setting;
   }

   public void render(DrawContext context, float x, float y, float width, int mouseX, int mouseY, float alpha) {
      this.x = x;
      this.y = y;
      this.width = width;
      if (this.dragging) {
         this.updateValue(mouseX);
      }

      float dt = Math.min((float)(System.nanoTime() - this.lastFrame) / 1.0E9F, 0.1F);
      this.lastFrame = System.nanoTime();
      float target = (this.setting.getValue() - this.setting.getMin()) / (this.setting.getMax() - this.setting.getMin());
      if (this.animatedPercent < 0.0F) {
         this.animatedPercent = target;
      }

      this.animatedPercent = this.animatedPercent + (target - this.animatedPercent) * Math.min(1.0F, dt * 15.0F);
      Msdf.text(
         context,
         MsdfManager.BOLD,
         this.setting.getName(),
         x,
         y + ClickGuiLayout.physicalToRender(3.0F),
         ClickGuiLayout.physicalToRender(10.0F),
         ClickGuiTheme.alpha(ClickGuiTheme.TEXT, alpha),
         true
      );
      String value = this.format(this.setting.getValue());
      float valueSize = ClickGuiLayout.physicalToRender(9.0F);
      float valueWidth = Math.max(
         ClickGuiLayout.physicalToRender(24.0F), Msdf.width(MsdfManager.BOLD, value, valueSize) + ClickGuiLayout.physicalToRender(10.0F)
      );
      float badgeHeight = ClickGuiLayout.physicalToRender(16.0F);
      float badgeX = x + width - valueWidth;
      Render2D.rect(
         context, badgeX, y, valueWidth, badgeHeight, ClickGuiLayout.physicalToRender(4.0F), ClickGuiTheme.alpha(ClickGuiTheme.GLASS_INSET, alpha), true
      );
      Msdf.text(
         context,
         MsdfManager.BOLD,
         value,
         badgeX + (valueWidth - Msdf.width(MsdfManager.BOLD, value, valueSize)) / 2.0F,
         y + ClickGuiLayout.physicalToRender(5.0F),
         valueSize,
         ClickGuiTheme.alpha(ClickGuiTheme.TEXT, alpha),
         true
      );
      float trackY = y + ClickGuiLayout.physicalToRender(25.0F);
      float trackHeight = ClickGuiLayout.physicalToRender(2.0F);
      Render2D.rect(context, x, trackY, width, trackHeight, trackHeight / 2.0F, ClickGuiTheme.alpha(ClickGuiTheme.TOGGLE_OFF, alpha), true);
      float progress = width * this.animatedPercent;
      Render2D.rect(context, x, trackY, progress, trackHeight, trackHeight / 2.0F, true, ClickGuiTheme.accentGradient(x, progress, alpha));
      float knob = ClickGuiLayout.physicalToRender(6.0F);
      float knobX = x + Math.max(knob / 2.0F, Math.min(width - knob / 2.0F, progress));
      Render2D.glow(
         context, knobX, trackY + trackHeight / 2.0F, ClickGuiLayout.physicalToRender(7.0F), ClickGuiTheme.accentAt(knobX, Math.round(alpha * 70.0F)), true
      );
      Render2D.rect(context, knobX - knob / 2.0F, trackY - (knob - trackHeight) / 2.0F, knob, knob, knob / 2.0F, ClickGuiTheme.alpha(-1, alpha), true);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      float trackY = this.y + ClickGuiLayout.physicalToRender(25.0F);
      if (button == 0 && inside(mouseX, mouseY, this.x, trackY - ClickGuiLayout.physicalToRender(6.0F), this.width, ClickGuiLayout.physicalToRender(14.0F))) {
         this.dragging = true;
         this.updateValue(mouseX);
         return true;
      } else {
         return false;
      }
   }

   public void mouseReleased(double mouseX, double mouseY, int button) {
      this.dragging = false;
   }

   public float getHeight() {
      return ClickGuiLayout.physicalToRender(34.0F);
   }

   private void updateValue(double mouseX) {
      float percent = Math.max(0.0F, Math.min(1.0F, (float)((mouseX - this.x) / this.width)));
      float value = this.setting.getMin() + (this.setting.getMax() - this.setting.getMin()) * percent;
      if (this.setting.isInteger()) {
         this.setting.setValue(Math.round(value));
      } else {
         this.setting.setValue(new BigDecimal(Float.toString(value)).setScale(2, RoundingMode.HALF_UP).floatValue());
      }
   }

   private String format(float value) {
      return (this.setting.isInteger() ? Integer.toString(Math.round(value)) : String.format("%.1f", value)) + this.setting.getSuffix();
   }

   private static boolean inside(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }
}
