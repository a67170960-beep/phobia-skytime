package ruhack.phobia.system.screensystem.clickgui.settingcomponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.system.screensystem.clickgui.ClickGuiEffects;
import ruhack.phobia.system.screensystem.clickgui.ClickGuiLayout;
import ruhack.phobia.system.screensystem.clickgui.ClickGuiTheme;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.interfaces.AbstractSettingComponent;
import ruhack.phobia.util.render.FadeText;
import ruhack.phobia.util.render.scissor.TextScissor;

public class MultiSelectComponent extends AbstractSettingComponent {
   private final MultiSelectSetting multiSelectSetting;
   private boolean expanded = false;
   private float dropdownWidthBonus = ClickGuiLayout.physicalToRender(98.0F);
   private float dropdownX;
   private float dropdownY;
   private float dropdownWidth;
   private float dropdownHeight;
   private final Map<String, Float> hoverAnims = new HashMap<>();
   private final Map<String, Float> selectAnims = new HashMap<>();
   private float animation = 0.0F;
   private long lastUpdateTime = System.currentTimeMillis();
   private float cachedDeltaTime = 0.0F;

   public boolean isExpanded() {
      return this.expanded;
   }

   public MultiSelectComponent(MultiSelectSetting setting) {
      super(setting);
      this.multiSelectSetting = setting;
   }

   private void updateDeltaTime() {
      long currentTime = System.currentTimeMillis();
      this.cachedDeltaTime = Math.min((float)(currentTime - this.lastUpdateTime) / 1000.0F, 0.05F);
      this.lastUpdateTime = currentTime;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   public void render(DrawContext context, float x, float y, float width, int mouseX, int mouseY, float delta) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.updateDeltaTime();
      this.height = ClickGuiLayout.physicalToRender(24.0F);
      String currentVal = this.getSelectedText();
      this.animation = this.lerp(this.animation, this.expanded ? 1.0F : 0.0F, this.cachedDeltaTime * 12.0F);
      int textColor = ClickGuiTheme.alpha(ClickGuiTheme.TEXT, this.alphaMultiplier);
      int accentColor = ClickGuiTheme.accentAt(x + width, Math.round(this.alphaMultiplier * 255.0F));
      int outlineColor = ClickGuiTheme.alpha(ClickGuiTheme.BORDER, this.alphaMultiplier * 0.62F);
      int bgColor = ClickGuiTheme.alpha(ClickGuiTheme.WINDOW, this.alphaMultiplier);
      float plateWidth = ClickGuiLayout.physicalToRender(82.0F);
      float plateHeight = ClickGuiLayout.physicalToRender(18.0F);
      float plateX = x + width - plateWidth;
      float plateY = y + this.height / 2.0F - plateHeight / 2.0F;
      if (MsdfManager.BOLD != null) {
         float nameMaxWidth = plateX - (x + 6.0F) - 8.0F;
         String scissorKey = "ms_" + this.multiSelectSetting.getName();
         TextScissor.render(
            context,
            MsdfManager.BOLD,
            this.multiSelectSetting.getName(),
            x,
            y + ClickGuiLayout.physicalToRender(7.0F),
            ClickGuiLayout.physicalToRender(10.0F),
            nameMaxWidth,
            ClickGuiLayout.physicalToRender(12.0F),
            textColor,
            bgColor,
            true,
            scissorKey,
            mouseX,
            mouseY,
            this.height
         );
      }

      float plateRadius = ClickGuiLayout.physicalToRender(6.0F);
      Render2D.rect(context, plateX, plateY, plateWidth, plateHeight, plateRadius, bgColor, true);
      Render2D.outline(context, plateX, plateY, plateWidth, plateHeight, plateRadius, ClickGuiLayout.physicalToRender(1.0F), outlineColor, true);
      if (MsdfManager.BOLD != null) {
         float textMaxWidth = plateWidth - ClickGuiLayout.physicalToRender(22.0F);
         FadeText.renderGradient(
            context,
            MsdfManager.BOLD,
            currentVal,
            plateX + ClickGuiLayout.physicalToRender(6.0F),
            plateY + ClickGuiLayout.physicalToRender(5.0F),
            ClickGuiLayout.physicalToRender(9.0F),
            textMaxWidth,
            ClickGuiLayout.physicalToRender(10.0F),
            accentColor,
            this.alphaMultiplier,
            true
         );
         if (MsdfManager.ICONS != null) {
            float iconSize = ClickGuiLayout.physicalToRender(8.0F);
            float iconX = plateX + plateWidth - ClickGuiLayout.physicalToRender(14.0F);
            float iconY = plateY + ClickGuiLayout.physicalToRender(6.0F);
            float iconRotation = -45.0F + 90.0F * this.animation;
            Msdf.text(context, MsdfManager.ICONS, "p", iconX, iconY, iconSize, accentColor, iconRotation, true);
         }
      }

      float itemHeight = ClickGuiLayout.physicalToRender(26.0F);
      this.dropdownHeight = this.multiSelectSetting.getList().size() * itemHeight + ClickGuiLayout.physicalToRender(4.0F);
      this.dropdownWidth = plateWidth + this.dropdownWidthBonus;
      this.dropdownX = plateX + plateWidth - this.dropdownWidth;
      this.dropdownY = plateY + plateHeight + ClickGuiLayout.physicalToRender(4.0F);
      float margin = ClickGuiLayout.physicalToRender(8.0F);
      this.dropdownX = Math.max(margin, Math.min(this.dropdownX, Render2D.getFixedScaledWidth() - this.dropdownWidth - margin));
      if (this.dropdownY + this.dropdownHeight > Render2D.getFixedScaledHeight() - margin) {
         this.dropdownY = plateY - this.dropdownHeight - ClickGuiLayout.physicalToRender(4.0F);
      }

      this.dropdownY = Math.max(margin, this.dropdownY);
   }

   private String getSelectedText() {
      if (this.multiSelectSetting.getSelected().isEmpty()) {
         return "None";
      }

      int count = this.multiSelectSetting.getSelected().size();
      return count == 1 ? this.multiSelectSetting.getSelected().get(0) : count + " выбрано";
   }

   public void renderDropdown(DrawContext context, int mouseX, int mouseY) {
      if (!(this.animation <= 0.01F)) {
         float dropdownAlpha = this.animation * this.alphaMultiplier;
         float itemHeight = ClickGuiLayout.physicalToRender(26.0F);
         float animatedHeight = this.dropdownHeight * this.animation;
         float dropRadius = ClickGuiLayout.physicalToRender(7.0F);
         ClickGuiEffects.shadow(
            context, this.dropdownX, this.dropdownY, this.dropdownWidth, animatedHeight, dropRadius, ClickGuiEffects.ShadowLevel.FLOATING, dropdownAlpha
         );
         Render2D.rect(
            context,
            this.dropdownX,
            this.dropdownY,
            this.dropdownWidth,
            animatedHeight,
            dropRadius,
            ClickGuiTheme.alpha(ClickGuiTheme.FLOATING, dropdownAlpha),
            true
         );
         Render2D.outline(
            context,
            this.dropdownX,
            this.dropdownY,
            this.dropdownWidth,
            animatedHeight,
            dropRadius,
            ClickGuiLayout.physicalToRender(1.0F),
            ClickGuiTheme.alpha(ClickGuiTheme.BORDER, dropdownAlpha * 0.62F),
            true
         );
         ClickGuiEffects.topHighlight(context, this.dropdownX, this.dropdownY, this.dropdownWidth, dropRadius, dropdownAlpha);
         if (this.animation > 0.05F) {
            float curY = this.dropdownY + ClickGuiLayout.physicalToRender(2.0F);
            int index = 0;

            for (String option : this.multiSelectSetting.getList()) {
               boolean hovered = mouseX >= this.dropdownX && mouseX <= this.dropdownX + this.dropdownWidth && mouseY >= curY && mouseY <= curY + itemHeight;
               boolean selected = this.multiSelectSetting.isSelected(option);
               float hAnim = this.hoverAnims.getOrDefault(option, 0.0F);
               float sAnim = this.selectAnims.getOrDefault(option, selected ? 1.0F : 0.0F);
               hAnim = this.lerp(hAnim, hovered ? 1.0F : 0.0F, this.cachedDeltaTime * 18.0F);
               sAnim = this.lerp(sAnim, selected ? 1.0F : 0.0F, this.cachedDeltaTime * 18.0F);
               this.hoverAnims.put(option, hAnim);
               this.selectAnims.put(option, sAnim);
               float itemFade = this.expanded ? Math.max(0.0F, Math.min(1.0F, this.animation * 1.2F - index * 0.05F)) : (float)Math.pow(this.animation, 2.0);
               if (MsdfManager.BOLD != null) {
                  int baseR = (int)(102.0F + 153.0F * hAnim);
                  int baseG = (int)(102.0F + 153.0F * hAnim);
                  int baseB = (int)(102.0F + 153.0F * hAnim);
                  int itemAlpha = (int)(255.0F * itemFade * this.alphaMultiplier);
                  int baseColor = ColorUtil.rgba(baseR, baseG, baseB, itemAlpha);
                  int selectedColor = ClickGuiTheme.accentAt(this.dropdownX + ClickGuiLayout.physicalToRender(8.0F), itemAlpha);
                  int finalColor = ColorUtil.interpolateColor(baseColor, selectedColor, sAnim);
                  float optionMaxWidth = this.dropdownWidth - ClickGuiLayout.physicalToRender(16.0F);
                  int dropBgColor = ClickGuiTheme.alpha(ClickGuiTheme.FLOATING, dropdownAlpha);
                  String optionKey = "msd_" + this.multiSelectSetting.getName() + "_" + option;
                  TextScissor.render(
                     context,
                     MsdfManager.BOLD,
                     option,
                     this.dropdownX + ClickGuiLayout.physicalToRender(8.0F),
                     curY + ClickGuiLayout.physicalToRender(8.0F),
                     ClickGuiLayout.physicalToRender(10.0F),
                     optionMaxWidth,
                     10.0F,
                     finalColor,
                     dropBgColor,
                     true,
                     optionKey,
                     mouseX,
                     mouseY,
                     itemHeight
                  );
               }

               curY += itemHeight;
               index++;
            }
         }
      }
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      float plateWidth = ClickGuiLayout.physicalToRender(82.0F);
      float plateX = this.x + this.width - plateWidth;
      float plateHeight = ClickGuiLayout.physicalToRender(18.0F);
      float plateY = this.y + this.height / 2.0F - plateHeight / 2.0F;
      if (button == 0 && this.isInside(mx, my, plateX, plateY, plateWidth, plateHeight)) {
         this.expanded = !this.expanded;
         return true;
      }

      if (this.expanded && this.animation > 0.8F) {
         float itemHeight = ClickGuiLayout.physicalToRender(26.0F);
         float curY = this.dropdownY + ClickGuiLayout.physicalToRender(2.0F);

         for (String option : this.multiSelectSetting.getList()) {
            if (mx >= this.dropdownX && mx <= this.dropdownX + this.dropdownWidth && my >= curY && my <= curY + itemHeight) {
               this.toggleSelection(option);
               return true;
            }

            curY += itemHeight;
         }

         if (this.isInside(mx, my, this.dropdownX, this.dropdownY, this.dropdownWidth, this.dropdownHeight)) {
            return true;
         }

         this.expanded = false;
         return true;
      } else {
         return false;
      }
   }

   private void toggleSelection(String option) {
      if (this.multiSelectSetting.isSelected(option)) {
         this.multiSelectSetting.getSelected().remove(option);
      } else {
         this.multiSelectSetting.getSelected().add(option);
      }
   }

   public void closeDropdown() {
      this.expanded = false;
      this.animation = 0.0F;
   }

   public boolean consumesClick(double mx, double my) {
      return this.expanded && this.animation > 0.5F && this.isInside(mx, my, this.dropdownX, this.dropdownY, this.dropdownWidth, this.dropdownHeight);
   }

   private boolean isInside(double mx, double my, float x, float y, float w, float h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }

   private float lerp(float current, float target, float factor) {
      return current + (target - current) * Math.min(factor, 1.0F);
   }

   public float getHeight() {
      return ClickGuiLayout.physicalToRender(24.0F);
   }

   private List<String> wrapText(String text, float maxWidth, float size, MsdfFont font) {
      List<String> lines = new ArrayList<>();
      if (text != null && !text.isEmpty()) {
         String[] words = text.split(" ");
         String currentLine = "";

         for (String word : words) {
            String test = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (font != null && font.getStringWidth(test, size) <= maxWidth) {
               currentLine = test;
            } else {
               if (!currentLine.isEmpty()) {
                  lines.add(currentLine);
               }

               currentLine = word;
            }
         }

         if (!currentLine.isEmpty()) {
            lines.add(currentLine);
         }

         return lines;
      } else {
         return lines;
      }
   }

   public float getDropdownHeight() {
      return this.dropdownHeight;
   }
}
