package ruhack.phobia.system.screensystem.clickgui.settingcomponent;

import net.minecraft.client.gui.DrawContext;
import ruhack.phobia.system.modulesystem.setting.implement.ButtonSetting;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.system.screensystem.clickgui.ClickGuiLayout;
import ruhack.phobia.system.screensystem.clickgui.ClickGuiTheme;

public final class ButtonComponent {
   private final ButtonSetting setting;
   private float x;
   private float y;
   private float width;

   public ButtonComponent(ButtonSetting setting) {
      this.setting = setting;
   }

   public ButtonSetting getSetting() {
      return this.setting;
   }

   public void render(DrawContext context, float x, float y, float width, int mouseX, int mouseY, float alpha) {
      this.x = x;
      this.y = y;
      this.width = width;
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
      float buttonWidth = ClickGuiLayout.physicalToRender(70.0F);
      float buttonX = x + width - buttonWidth;
      float buttonY = y + ClickGuiLayout.physicalToRender(3.0F);
      float buttonHeight = ClickGuiLayout.physicalToRender(18.0F);
      Render2D.rect(
         context,
         buttonX,
         buttonY,
         buttonWidth,
         buttonHeight,
         ClickGuiLayout.physicalToRender(4.0F),
         true,
         ClickGuiTheme.accentGradient(x, width, alpha * 0.24F)
      );
      String label = this.setting.getButtonName() != null && !this.setting.getButtonName().isBlank() ? this.setting.getButtonName() : "Выполнить";
      float labelSize = ClickGuiLayout.physicalToRender(10.0F);
      float labelWidth = Msdf.width(MsdfManager.BOLD, label, labelSize);
      Msdf.text(
         context,
         MsdfManager.BOLD,
         label,
         buttonX + (buttonWidth - labelWidth) / 2.0F,
         buttonY + ClickGuiLayout.physicalToRender(5.0F),
         labelSize,
         ClickGuiTheme.alpha(ClickGuiTheme.TEXT, alpha),
         true
      );
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && !(mouseX < this.x) && !(mouseX > this.x + this.width) && !(mouseY < this.y) && !(mouseY > this.y + this.getHeight())) {
         if (this.setting.getRunnable() != null) {
            this.setting.getRunnable().run();
         }

         return true;
      } else {
         return false;
      }
   }

   public float getHeight() {
      return ClickGuiLayout.physicalToRender(24.0F);
   }
}
