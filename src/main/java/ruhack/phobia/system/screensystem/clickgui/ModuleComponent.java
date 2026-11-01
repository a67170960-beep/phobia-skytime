package ruhack.phobia.system.screensystem.clickgui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.setting.implement.BindSetting;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.ButtonSetting;
import ruhack.phobia.system.modulesystem.setting.implement.ColorSetting;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.system.modulesystem.setting.implement.TextSetting;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.system.screensystem.clickgui.settingcomponent.BindComponent;
import ruhack.phobia.system.screensystem.clickgui.settingcomponent.BooleanComponent;
import ruhack.phobia.system.screensystem.clickgui.settingcomponent.ButtonComponent;
import ruhack.phobia.system.screensystem.clickgui.settingcomponent.ColorComponent;
import ruhack.phobia.system.screensystem.clickgui.settingcomponent.MultiSelectComponent;
import ruhack.phobia.system.screensystem.clickgui.settingcomponent.SelectComponent;
import ruhack.phobia.system.screensystem.clickgui.settingcomponent.SliderComponent;
import ruhack.phobia.system.screensystem.clickgui.settingcomponent.TextComponent;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.string.KeyHelper;

public class ModuleComponent {
   private final ModuleStructure module;
   private float x;
   private float y;
   private float width;
   private float height;
   private List<Object> settingComponents = null;
   private boolean selected = false;
   private float animProgress;
   private float alpha = 1.0F;
   private boolean grouped;
   private float hoverAnim = 0.0F;
   private long lastUpdateTime = System.currentTimeMillis();
   private static final int TOGGLE_OFF_BG = ClickGuiTheme.TOGGLE_OFF;
   private static final int TOGGLE_OFF_KNOB = ClickGuiTheme.TOGGLE_KNOB;
   private static final int TOGGLE_ON_KNOB = ColorUtil.rgba(255, 255, 255, 255);
   private boolean hovered;
   private boolean binding;

   private float getDeltaTime() {
      long currentTime = System.currentTimeMillis();
      float dt = Math.min((float)(currentTime - this.lastUpdateTime) / 1000.0F, 0.1F);
      this.lastUpdateTime = currentTime;
      return dt;
   }

   public ModuleComponent(ModuleStructure module) {
      this.module = module;
   }

   public void setAlpha(float alpha) {
      this.alpha = alpha;
   }

   public void setGrouped(boolean grouped) {
      this.grouped = grouped;
   }

   public void render(DrawContext context, float x, float y, float width, int mouseX, int mouseY) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = ClickGuiLayout.physicalToRender(this.grouped ? 34.0F : 40.0F);
      float visualWidth = width;
      float dt = this.getDeltaTime();
      this.hovered = this.isInside(mouseX, mouseY, x, y, visualWidth, this.height);
      this.hoverAnim = this.hoverAnim + ((this.hovered ? 1.0F : 0.0F) - this.hoverAnim) * dt * 10.0F;
      int bgColor = ColorUtil.interpolateColor(ClickGuiTheme.SURFACE, ClickGuiTheme.SURFACE_HOVER, this.hoverAnim * 0.72F);
      float radius = ClickGuiLayout.physicalToRender(9.0F);
      if (this.grouped) {
         if (this.hoverAnim > 0.01F) {
            Render2D.rect(
               context,
               x,
               y,
               visualWidth,
               this.height,
               ClickGuiLayout.physicalToRender(6.0F),
               ClickGuiTheme.alpha(bgColor, this.alpha * this.hoverAnim * 0.82F),
               true
            );
         }
      } else {
         ClickGuiEffects.shadow(
            context, x, y, visualWidth, this.height, radius, ClickGuiEffects.ShadowLevel.CARD, this.alpha * (0.35F + this.hoverAnim * 0.35F)
         );
         if (this.hoverAnim > 0.01F) {
            ClickGuiEffects.accentGlow(context, x, y, visualWidth, this.height, radius, this.alpha * this.hoverAnim * 0.16F);
         }

         Render2D.rect(context, x, y, visualWidth, this.height, radius, ClickGuiTheme.alpha(bgColor, this.alpha), true);
         ClickGuiEffects.topHighlight(context, x, y, visualWidth, radius, this.alpha * (0.35F + this.hoverAnim * 0.35F));
      }

      if (MsdfManager.BOLD != null) {
         String displayName = this.binding ? "Bind: " + this.bindDisplayName() : this.module.getName();
         Msdf.text(
            context,
            MsdfManager.BOLD,
            displayName,
            x + ClickGuiLayout.physicalToRender(this.grouped ? 8.0F : 12.0F),
            y + ClickGuiLayout.physicalToRender(this.grouped ? 10.0F : 13.0F),
            ClickGuiLayout.physicalToRender(this.grouped ? 11.0F : 13.0F),
            this.binding ? ClickGuiTheme.accentAt(x, Math.round(this.alpha * 255.0F)) : ClickGuiTheme.alpha(ClickGuiTheme.TEXT, this.alpha),
            true
         );
      }

      float toggleW = ClickGuiLayout.physicalToRender(30.0F);
      float toggleH = ClickGuiLayout.physicalToRender(16.0F);
      float toggleX = x + visualWidth - ClickGuiLayout.physicalToRender(10.0F) - toggleW;
      float toggleY = y + (this.height - toggleH) / 2.0F;
      this.drawToggle(context, toggleX, toggleY, this.module.isState(), dt);
      boolean hasSettings = !this.module.settings().isEmpty();
      if (hasSettings && MsdfManager.ICONS != null) {
         int settingsIconColor = this.selected ? ClickGuiTheme.accentAt(toggleX, 255) : ClickGuiTheme.TEXT_MUTED;
         Msdf.text(
            context,
            MsdfManager.ICONS,
            "T",
            toggleX - ClickGuiLayout.physicalToRender(23.0F),
            y + ClickGuiLayout.physicalToRender(this.grouped ? 11.0F : 14.0F),
            ClickGuiLayout.physicalToRender(18.0F),
            ClickGuiTheme.alpha(settingsIconColor, this.alpha),
            true
         );
      }
   }

   public void renderSettings(DrawContext context, float x, float y, float width, int mouseX, int mouseY, float settingsAlpha) {
      if (this.settingComponents == null) {
         this.initSettingComponents();
      }

      float curY = y;
      boolean isFirst = true;

      for (int i = 0; i < this.settingComponents.size(); i++) {
         Object sc = this.settingComponents.get(i);
         if (this.isSettingVisible(sc)) {
            if (!isFirst) {
               curY += ClickGuiLayout.physicalToRender(5.0F);
            }

            isFirst = false;
            if (sc instanceof TextComponent) {
               ((TextComponent)sc).setAlphaMultiplier(settingsAlpha);
               ((TextComponent)sc).render(context, x, curY, width, mouseX, mouseY, 0.0F);
               curY += ((TextComponent)sc).getHeight();
            } else if (sc instanceof ColorComponent) {
               ((ColorComponent)sc).setAlphaMultiplier(settingsAlpha);
               ((ColorComponent)sc).render(context, x, curY, width, mouseX, mouseY, 0.0F);
               curY += ((ColorComponent)sc).getHeight();
            } else if (sc instanceof ButtonComponent) {
               ((ButtonComponent)sc).render(context, x, curY, width, mouseX, mouseY, settingsAlpha);
               curY += ((ButtonComponent)sc).getHeight();
            } else if (sc instanceof SliderComponent) {
               ((SliderComponent)sc).render(context, x, curY, width, mouseX, mouseY, settingsAlpha);
               curY += ((SliderComponent)sc).getHeight();
            } else if (sc instanceof BooleanComponent) {
               ((BooleanComponent)sc).render(context, x, curY, width, mouseX, mouseY, settingsAlpha);
               curY += ((BooleanComponent)sc).getHeight();
            } else if (sc instanceof BindComponent) {
               ((BindComponent)sc).render(context, x, curY, width, mouseX, mouseY, settingsAlpha);
               curY += ((BindComponent)sc).getHeight();
            } else if (sc instanceof SelectComponent) {
               ((SelectComponent)sc).setAlphaMultiplier(settingsAlpha);
               ((SelectComponent)sc).render(context, x, curY, width, mouseX, mouseY, 0.0F);
               curY += ((SelectComponent)sc).getHeight();
            } else if (sc instanceof MultiSelectComponent) {
               ((MultiSelectComponent)sc).setAlphaMultiplier(settingsAlpha);
               ((MultiSelectComponent)sc).render(context, x, curY, width, mouseX, mouseY, 0.0F);
               curY += ((MultiSelectComponent)sc).getHeight();
            }
         }
      }
   }

   public void renderDropdowns(DrawContext context, int mouseX, int mouseY) {
      if (this.settingComponents != null) {
         for (Object sc : this.settingComponents) {
            if (this.isSettingVisible(sc)) {
               if (sc instanceof SelectComponent) {
                  ((SelectComponent)sc).renderDropdown(context, mouseX, mouseY);
               } else if (sc instanceof MultiSelectComponent) {
                  ((MultiSelectComponent)sc).renderDropdown(context, mouseX, mouseY);
               }
            }
         }
      }
   }

   public void closeAllDropdowns() {
      if (this.settingComponents != null) {
         for (Object sc : this.settingComponents) {
            if (sc instanceof SelectComponent) {
               ((SelectComponent)sc).closeDropdown();
            } else if (sc instanceof MultiSelectComponent) {
               ((MultiSelectComponent)sc).closeDropdown();
            }
         }
      }
   }

   public boolean dropdownConsumesClick(double mx, double my) {
      if (this.settingComponents == null) {
         return false;
      }

      for (Object sc : this.settingComponents) {
         if (this.isSettingVisible(sc)) {
            if (sc instanceof SelectComponent) {
               if (((SelectComponent)sc).isExpanded()) {
                  return true;
               }
            } else if (sc instanceof MultiSelectComponent && ((MultiSelectComponent)sc).isExpanded()) {
               return true;
            }
         }
      }

      return false;
   }

   private void initSettingComponents() {
      this.settingComponents = new ArrayList<>();
      this.module.settings().forEach(s -> {
         if (s instanceof SliderSettings) {
            this.settingComponents.add(new SliderComponent((SliderSettings)s));
         } else if (s instanceof BooleanSetting) {
            this.settingComponents.add(new BooleanComponent((BooleanSetting)s));
         } else if (s instanceof BindSetting) {
            this.settingComponents.add(new BindComponent((BindSetting)s));
         } else if (s instanceof TextSetting) {
            this.settingComponents.add(new TextComponent((TextSetting)s));
         } else if (s instanceof SelectSetting) {
            this.settingComponents.add(new SelectComponent((SelectSetting)s));
         } else if (s instanceof MultiSelectSetting) {
            this.settingComponents.add(new MultiSelectComponent((MultiSelectSetting)s));
         } else if (s instanceof ColorSetting) {
            this.settingComponents.add(new ColorComponent((ColorSetting)s));
         } else if (s instanceof ButtonSetting) {
            this.settingComponents.add(new ButtonComponent((ButtonSetting)s));
         }
      });
   }

   private boolean isSettingVisible(Object sc) {
      if (sc instanceof SliderComponent) {
         return ((SliderComponent)sc).getSetting().isVisible();
      } else if (sc instanceof BooleanComponent) {
         return ((BooleanComponent)sc).getSetting().isVisible();
      } else if (sc instanceof SelectComponent) {
         return ((SelectComponent)sc).getSetting().isVisible();
      } else if (sc instanceof MultiSelectComponent) {
         return ((MultiSelectComponent)sc).getSetting().isVisible();
      } else if (sc instanceof BindComponent) {
         return ((BindComponent)sc).getSetting().isVisible();
      } else if (sc instanceof TextComponent) {
         return ((TextComponent)sc).getSetting().isVisible();
      } else if (sc instanceof ColorComponent) {
         return ((ColorComponent)sc).getSetting().isVisible();
      } else {
         return sc instanceof ButtonComponent ? ((ButtonComponent)sc).getSetting().isVisible() : true;
      }
   }

   public float getSettingsHeight() {
      if (this.settingComponents == null) {
         this.initSettingComponents();
      }

      float h = 0.0F;
      boolean isFirst = true;

      for (int i = 0; i < this.settingComponents.size(); i++) {
         Object sc = this.settingComponents.get(i);
         if (this.isSettingVisible(sc)) {
            if (!isFirst) {
               h += ClickGuiLayout.physicalToRender(5.0F);
            }

            isFirst = false;
            if (sc instanceof TextComponent) {
               h += ((TextComponent)sc).getHeight();
            } else if (sc instanceof ColorComponent) {
               h += ((ColorComponent)sc).getHeight();
            } else if (sc instanceof ButtonComponent) {
               h += ((ButtonComponent)sc).getHeight();
            } else if (sc instanceof SliderComponent) {
               h += ((SliderComponent)sc).getHeight();
            } else if (sc instanceof BooleanComponent) {
               h += ((BooleanComponent)sc).getHeight();
            } else if (sc instanceof BindComponent) {
               h += ((BindComponent)sc).getHeight();
            } else if (sc instanceof SelectComponent) {
               h += ((SelectComponent)sc).getHeight();
            } else if (sc instanceof MultiSelectComponent) {
               h += ((MultiSelectComponent)sc).getHeight();
            }
         }
      }

      return h;
   }

   private void drawToggle(DrawContext context, float x, float y, boolean enabled, float dt) {
      float toggleW = ClickGuiLayout.physicalToRender(30.0F);
      float toggleH = ClickGuiLayout.physicalToRender(16.0F);
      float target = enabled ? 1.0F : 0.0F;
      this.animProgress = this.animProgress + (target - this.animProgress) * dt * 10.0F;
      int toggleOn = ClickGuiTheme.accentAt(x, 255);
      int bgR = (int)((TOGGLE_OFF_BG >> 16 & 0xFF) + ((toggleOn >> 16 & 0xFF) - (TOGGLE_OFF_BG >> 16 & 0xFF)) * this.animProgress);
      int bgG = (int)((TOGGLE_OFF_BG >> 8 & 0xFF) + ((toggleOn >> 8 & 0xFF) - (TOGGLE_OFF_BG >> 8 & 0xFF)) * this.animProgress);
      int bgB = (int)((TOGGLE_OFF_BG & 0xFF) + ((toggleOn & 0xFF) - (TOGGLE_OFF_BG & 0xFF)) * this.animProgress);
      int bg = ColorUtil.rgba(bgR, bgG, bgB, (int)(255.0F * this.alpha));
      int knobR = (int)((TOGGLE_OFF_KNOB >> 16 & 0xFF) + ((TOGGLE_ON_KNOB >> 16 & 0xFF) - (TOGGLE_OFF_KNOB >> 16 & 0xFF)) * this.animProgress);
      int knobG = (int)((TOGGLE_OFF_KNOB >> 8 & 0xFF) + ((TOGGLE_ON_KNOB >> 8 & 0xFF) - (TOGGLE_OFF_KNOB >> 8 & 0xFF)) * this.animProgress);
      int knobB = (int)((TOGGLE_OFF_KNOB & 0xFF) + ((TOGGLE_ON_KNOB & 0xFF) - (TOGGLE_OFF_KNOB & 0xFF)) * this.animProgress);
      int knob = ColorUtil.rgba(knobR, knobG, knobB, (int)(255.0F * this.alpha));
      if (this.animProgress > 0.01F) {
         ClickGuiEffects.accentGlow(context, x, y, toggleW, toggleH, toggleH / 2.0F, this.alpha * this.animProgress * 0.72F);
      }

      if (this.animProgress > 0.01F) {
         Render2D.rect(
            context, x, y, toggleW, toggleH, toggleH / 2.0F, true, ClickGuiTheme.mixedAccentGradient(TOGGLE_OFF_BG, x, toggleW, this.animProgress, this.alpha)
         );
      } else {
         Render2D.rect(context, x, y, toggleW, toggleH, toggleH / 2.0F, bg, true);
      }

      float knobBaseSize = ClickGuiLayout.physicalToRender(12.0F);
      float stretch = 1.0F - Math.abs(this.animProgress - 0.5F) * 2.0F;
      float knobW = knobBaseSize + stretch * ClickGuiLayout.physicalToRender(2.0F);
      float knobH = knobBaseSize;
      float knobPadding = ClickGuiLayout.physicalToRender(2.0F);
      float knobStartX = x + knobPadding;
      float knobEndX = x + toggleW - knobW - knobPadding;
      float knobX = knobStartX + (knobEndX - knobStartX) * this.animProgress;
      float knobY = y + (toggleH - knobH) / 2.0F;
      Render2D.rect(context, knobX, knobY, knobW, knobH, knobH / 2.0F, knob, true);
   }

   public ModuleComponent.Interaction mouseClicked(double mx, double my, int button) {
      float visualWidth = this.width;
      if (button == 2 && this.isInside(mx, my, this.x, this.y, visualWidth, this.height)) {
         return ModuleComponent.Interaction.BIND;
      } else {
         float toggleW = ClickGuiLayout.physicalToRender(30.0F);
         float toggleH = ClickGuiLayout.physicalToRender(16.0F);
         float toggleX = this.x + visualWidth - ClickGuiLayout.physicalToRender(10.0F) - toggleW;
         float toggleY = this.y + (this.height - toggleH) / 2.0F;
         if (button == 0 && this.isInside(mx, my, toggleX, toggleY, toggleW, toggleH)) {
            this.module.switchState();
            return ModuleComponent.Interaction.TOGGLE;
         } else {
            float gearSize = ClickGuiLayout.physicalToRender(22.0F);
            float gearX = toggleX - ClickGuiLayout.physicalToRender(28.0F);
            float gearY = this.y + (this.height - gearSize) / 2.0F;
            return button == 0 && !this.module.settings().isEmpty() && this.isInside(mx, my, gearX, gearY, gearSize, gearSize)
               ? ModuleComponent.Interaction.SETTINGS
               : ModuleComponent.Interaction.NONE;
         }
      }
   }

   public boolean dropdownScrolled(double mx, double my, double amount) {
      if (this.settingComponents == null) {
         return false;
      }

      for (Object component : this.settingComponents) {
         if (component instanceof SelectComponent selectComponent && selectComponent.mouseScrolled(mx, my, amount)) {
            return true;
         }
      }

      return false;
   }

   public boolean isBinding() {
      return this.binding;
   }

   public void setBinding(boolean binding) {
      this.binding = binding;
   }

   public boolean handleBindKey(KeyInput input) {
      if (!this.binding) {
         return false;
      } else {
         int key = input.key();
         if (key == 256) {
            this.binding = false;
            return true;
         } else if (key == 261) {
            this.module.setKey(-1);
            this.module.setType(1);
            this.binding = false;
            return true;
         } else {
            this.module.setKey(key);
            this.module.setType(1);
            this.binding = false;
            return true;
         }
      }
   }

   public boolean handleBindMouse(int button) {
      if (this.binding && button >= 2) {
         this.module.setKey(button);
         this.module.setType(1);
         this.binding = false;
         return true;
      } else {
         return false;
      }
   }

   private String bindDisplayName() {
      return this.module.getKey() != -1 && this.module.getKey() != -1 ? KeyHelper.getKeyName(this.module.getKey()) : "...";
   }

   public void mouseClickedSettings(double mx, double my, int button) {
      if (this.settingComponents != null) {
         for (Object sc : this.settingComponents) {
            if (this.isSettingVisible(sc)) {
               boolean consumed = false;
               if (sc instanceof SliderComponent) {
                  consumed = ((SliderComponent)sc).mouseClicked(mx, my, button);
               } else if (sc instanceof BooleanComponent) {
                  consumed = ((BooleanComponent)sc).mouseClicked(mx, my, button);
               } else if (sc instanceof BindComponent) {
                  consumed = ((BindComponent)sc).mouseClicked(mx, my, button);
               } else if (sc instanceof TextComponent) {
                  consumed = ((TextComponent)sc).mouseClicked(mx, my, button);
               } else if (sc instanceof ColorComponent) {
                  consumed = ((ColorComponent)sc).mouseClicked(mx, my, button);
               } else if (sc instanceof ButtonComponent) {
                  consumed = ((ButtonComponent)sc).mouseClicked(mx, my, button);
               } else if (sc instanceof SelectComponent selectComponent) {
                  consumed = selectComponent.mouseClicked(mx, my, button);
                  if (consumed && selectComponent.isExpanded()) {
                     this.closeOtherDropdowns(selectComponent);
                  }
               } else if (sc instanceof MultiSelectComponent multiSelectComponent) {
                  consumed = multiSelectComponent.mouseClicked(mx, my, button);
                  if (consumed && multiSelectComponent.isExpanded()) {
                     this.closeOtherDropdowns(multiSelectComponent);
                  }
               }

               if (consumed) {
                  return;
               }
            }
         }
      }
   }

   private void closeOtherDropdowns(Object activeDropdown) {
      for (Object component : this.settingComponents) {
         if (component != activeDropdown) {
            if (component instanceof SelectComponent selectComponent) {
               selectComponent.closeDropdown();
            } else if (component instanceof MultiSelectComponent multiSelectComponent) {
               multiSelectComponent.closeDropdown();
            }
         }
      }
   }

   public void mouseReleased(double mx, double my, int button) {
      if (this.settingComponents != null) {
         this.settingComponents.forEach(sc -> {
            if (sc instanceof SliderComponent) {
               ((SliderComponent)sc).mouseReleased(mx, my, button);
            } else if (sc instanceof BooleanComponent) {
               ((BooleanComponent)sc).mouseReleased(mx, my, button);
            } else if (sc instanceof BindComponent) {
               ((BindComponent)sc).mouseReleased(mx, my, button);
            } else if (sc instanceof TextComponent) {
               ((TextComponent)sc).mouseReleased(mx, my, button);
            } else if (sc instanceof ColorComponent) {
               ((ColorComponent)sc).mouseReleased(mx, my, button);
            } else if (sc instanceof SelectComponent) {
               ((SelectComponent)sc).mouseReleased(mx, my, button);
            } else if (sc instanceof MultiSelectComponent) {
               ((MultiSelectComponent)sc).mouseReleased(mx, my, button);
            }
         });
      }
   }

   public void mouseDragged(double mx, double my, int button, double deltaX, double deltaY) {
      if (this.settingComponents != null) {
         for (Object sc : this.settingComponents) {
            if (sc instanceof ColorComponent) {
               ((ColorComponent)sc).mouseDragged(mx, my, button, deltaX, deltaY);
            } else if (sc instanceof TextComponent) {
               ((TextComponent)sc).mouseDragged(mx, my, button, deltaX, deltaY);
            }
         }
      }
   }

   public boolean keyPressed(KeyInput input) {
      if (this.settingComponents != null) {
         for (Object sc : this.settingComponents) {
            if (sc instanceof BindComponent) {
               if (((BindComponent)sc).keyPressed(input)) {
                  return true;
               }
            } else if (sc instanceof TextComponent && ((TextComponent)sc).keyPressed(input.key(), -1, input.modifiers())) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean charTyped(CharInput input) {
      if (this.settingComponents != null) {
         for (Object sc : this.settingComponents) {
            if (sc instanceof TextComponent && ((TextComponent)sc).charTyped((char)input.codepoint(), input.modifiers())) {
               return true;
            }
         }
      }

      return false;
   }

   public ModuleStructure getModule() {
      return this.module;
   }

   public boolean isHovered() {
      return this.hovered;
   }

   public ModuleCardBounds getBounds() {
      return new ModuleCardBounds(this.module, this.x, this.y, this.width, this.height);
   }

   public void setSelected(boolean selected) {
      this.selected = selected;
   }

   private boolean isInside(double mx, double my, float x, float y, float w, float h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }

   private List<String> wrapText(String text, float maxWidth, float size, MsdfFont font) {
      List<String> lines = new ArrayList<>();
      String[] words = text.split(" ");
      String currentLine = "";

      for (String word : words) {
         String test = currentLine.isEmpty() ? word : currentLine + " " + word;
         if (font.getStringWidth(test, size) <= maxWidth) {
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
   }

   public float getHeight() {
      return this.height;
   }

   public enum Interaction {
      NONE,
      TOGGLE,
      SETTINGS,
      BIND;
   }
}
