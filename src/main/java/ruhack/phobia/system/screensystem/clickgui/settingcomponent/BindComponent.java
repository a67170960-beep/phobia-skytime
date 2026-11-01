package ruhack.phobia.system.screensystem.clickgui.settingcomponent;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import ruhack.phobia.system.modulesystem.setting.implement.BindSetting;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.system.screensystem.clickgui.ClickGuiLayout;
import ruhack.phobia.system.screensystem.clickgui.ClickGuiTheme;

public final class BindComponent {
   private final BindSetting setting;
   private float x;
   private float y;
   private float width;
   private boolean binding;

   public BindComponent(BindSetting setting) {
      this.setting = setting;
   }

   public BindSetting getSetting() {
      return this.setting;
   }

   public void render(DrawContext context, float x, float y, float width, int mouseX, int mouseY, float alpha) {
      this.x = x;
      this.y = y;
      this.width = width;
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
      String key = this.binding ? "..." : this.setting.getKeyDisplayName();
      float textSize = ClickGuiLayout.physicalToRender(9.0F);
      float textWidth = Msdf.width(MsdfManager.BOLD, key, textSize);
      float plateWidth = Math.max(ClickGuiLayout.physicalToRender(44.0F), textWidth + ClickGuiLayout.physicalToRender(12.0F));
      float plateHeight = ClickGuiLayout.physicalToRender(18.0F);
      float plateX = x + width - plateWidth;
      float plateY = y + (this.getHeight() - plateHeight) / 2.0F;
      Render2D.rect(
         context,
         plateX,
         plateY,
         plateWidth,
         plateHeight,
         ClickGuiLayout.physicalToRender(4.0F),
         ClickGuiTheme.alpha(this.binding ? ClickGuiTheme.SURFACE_ACTIVE : ClickGuiTheme.GLASS_INSET, alpha),
         true
      );
      Msdf.text(
         context,
         MsdfManager.BOLD,
         key,
         plateX + (plateWidth - textWidth) / 2.0F,
         plateY + ClickGuiLayout.physicalToRender(5.0F),
         textSize,
         this.binding ? ClickGuiTheme.accentAt(x + width, Math.round(alpha * 255.0F)) : ClickGuiTheme.alpha(ClickGuiTheme.TEXT_MUTED, alpha),
         true
      );
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.binding) {
         if (button >= 2) {
            this.setting.setKeyBind(button, true);
            this.binding = false;
            return true;
         } else {
            return false;
         }
      } else if (button == 0 && inside(mouseX, mouseY, this.x, this.y, this.width, this.getHeight())) {
         this.binding = true;
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseButtonPressed(MouseInput input) {
      if (this.binding && input.button() >= 2) {
         this.setting.setKeyBind(input.button(), true);
         this.binding = false;
         return true;
      } else {
         return false;
      }
   }

   public boolean keyPressed(KeyInput input) {
      if (!this.binding) {
         return false;
      }

      int key = input.key();
      if (key != 256 && key != 261 && key != 259) {
         this.setting.setKeyBind(key, false);
      } else {
         this.setting.reset();
      }

      this.binding = false;
      return true;
   }

   public boolean isBinding() {
      return this.binding;
   }

   public void mouseReleased(double mouseX, double mouseY, int button) {
   }

   public float getHeight() {
      return ClickGuiLayout.physicalToRender(24.0F);
   }

   private static boolean inside(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }
}
