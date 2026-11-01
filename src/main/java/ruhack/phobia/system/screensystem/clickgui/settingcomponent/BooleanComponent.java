package ruhack.phobia.system.screensystem.clickgui.settingcomponent;

import net.minecraft.client.gui.DrawContext;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.system.screensystem.clickgui.ClickGuiEffects;
import ruhack.phobia.system.screensystem.clickgui.ClickGuiLayout;
import ruhack.phobia.system.screensystem.clickgui.ClickGuiTheme;
import ruhack.phobia.util.colorschem.ColorUtil;

public final class BooleanComponent {
   private static final float HEIGHT_PX = 24.0F;
   private final BooleanSetting setting;
   private float x;
   private float y;
   private float width;
   private float animation;
   private long lastFrame = System.nanoTime();

   public BooleanComponent(BooleanSetting setting) {
      this.setting = setting;
   }

   public BooleanSetting getSetting() {
      return this.setting;
   }

   public void render(DrawContext context, float x, float y, float width, int mouseX, int mouseY, float alpha) {
      this.x = x;
      this.y = y;
      this.width = width;
      float dt = Math.min((float)(System.nanoTime() - this.lastFrame) / 1.0E9F, 0.1F);
      this.lastFrame = System.nanoTime();
      this.animation = this.animation + ((this.setting.isValue() ? 1.0F : 0.0F) - this.animation) * Math.min(1.0F, dt * 14.0F);
      float height = this.getHeight();
      Msdf.text(
         context,
         MsdfManager.BOLD,
         this.setting.getName(),
         x,
         y + ClickGuiLayout.physicalToRender(7.0F),
         ClickGuiLayout.physicalToRender(10.0F),
         ClickGuiTheme.alpha(ClickGuiTheme.TEXT, alpha),
         true
      );
      float toggleWidth = ClickGuiLayout.physicalToRender(24.0F);
      float toggleHeight = ClickGuiLayout.physicalToRender(12.0F);
      float toggleX = x + width - toggleWidth;
      float toggleY = y + (height - toggleHeight) / 2.0F;
      int off = ClickGuiTheme.TOGGLE_OFF;
      int on = ClickGuiTheme.accentAt(toggleX, 255);
      int track = ColorUtil.rgba(
         mix(off >> 16 & 0xFF, on >> 16 & 0xFF, this.animation),
         mix(off >> 8 & 0xFF, on >> 8 & 0xFF, this.animation),
         mix(off & 0xFF, on & 0xFF, this.animation),
         (int)(255.0F * alpha)
      );
      if (this.animation > 0.02F) {
         ClickGuiEffects.accentGlow(context, toggleX, toggleY, toggleWidth, toggleHeight, toggleHeight / 2.0F, alpha * this.animation * 0.35F);
      }

      if (this.animation > 0.01F) {
         Render2D.rect(
            context,
            toggleX,
            toggleY,
            toggleWidth,
            toggleHeight,
            toggleHeight / 2.0F,
            true,
            ClickGuiTheme.mixedAccentGradient(off, toggleX, toggleWidth, this.animation, alpha)
         );
      } else {
         Render2D.rect(context, toggleX, toggleY, toggleWidth, toggleHeight, toggleHeight / 2.0F, track, true);
      }

      float knob = ClickGuiLayout.physicalToRender(8.0F);
      float padding = ClickGuiLayout.physicalToRender(2.0F);
      float knobX = toggleX + padding + this.animation * (toggleWidth - knob - padding * 2.0F);
      Render2D.rect(context, knobX, toggleY + (toggleHeight - knob) / 2.0F, knob, knob, knob / 2.0F, ClickGuiTheme.alpha(-1, alpha), true);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && inside(mouseX, mouseY, this.x, this.y, this.width, this.getHeight())) {
         this.setting.setValue(!this.setting.isValue());
         return true;
      } else {
         return false;
      }
   }

   public void mouseReleased(double mouseX, double mouseY, int button) {
   }

   public float getHeight() {
      return ClickGuiLayout.physicalToRender(24.0F);
   }

   private static int mix(int from, int to, float amount) {
      return Math.round(from + (to - from) * amount);
   }

   private static boolean inside(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }
}
