package ruhack.phobia.system.screensystem.clickgui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import ruhack.phobia.system.configsystem.impl.userconfig.UserConfig;
import ruhack.phobia.system.configsystem.impl.userconfig.UserConfigManager;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.util.render.scissor.Scissor;

public final class ConfigPopupComponent {
   private float x;
   private float y;
   private float width;
   private float height;
   private float animation;
   private float scroll;
   private float targetScroll;
   private boolean searchFocused;
   private boolean creating;
   private boolean closeRequested;
   private String search = "";
   private String newConfigName = "";

   public void render(DrawContext context, ClickGuiLayout layout, float x, float y, boolean open, double mouseX, double mouseY, float parentAlpha, float dt) {
      this.animation = this.animation + ((open ? 1.0F : 0.0F) - this.animation) * Math.min(1.0F, dt * 12.0F);
      if (!(this.animation < 0.01F)) {
         this.x = x;
         this.y = y + ClickGuiLayout.physicalToRender(5.0F) * this.animation;
         this.width = layout.configPopupWidth;
         float rowHeight = ClickGuiLayout.physicalToRender(27.0F);
         float titleHeight = ClickGuiLayout.physicalToRender(32.0F);
         float searchHeight = ClickGuiLayout.physicalToRender(27.0F);
         float actionsHeight = ClickGuiLayout.physicalToRender(34.0F);
         int visibleRows = Math.min(6, Math.max(2, this.filteredConfigs().size()));
         float listHeight = rowHeight * visibleRows;
         this.height = titleHeight + searchHeight + listHeight + actionsHeight + layout.padding * 2.0F;
         float animatedHeight = this.height * this.animation;
         float alpha = parentAlpha * this.animation;
         ClickGuiEffects.accentGlow(context, this.x, this.y, this.width, animatedHeight, layout.popupRadius, alpha * 0.1F);
         ClickGuiEffects.glass(
            context, this.x, this.y, this.width, animatedHeight, layout.popupRadius, ClickGuiTheme.FLOATING, 11.0F, ClickGuiEffects.ShadowLevel.FLOATING, alpha
         );
         Render2D.outline(
            context, this.x, this.y, this.width, animatedHeight, layout.popupRadius, 1.0F, ClickGuiTheme.alpha(ClickGuiTheme.BORDER, alpha * 0.62F), true
         );
         if (!(this.animation < 0.18F)) {
            float px = this.x + layout.padding;
            float cursorY = this.y + layout.padding;
            float text = ClickGuiLayout.physicalToRender(13.0F);
            float secondary = ClickGuiLayout.physicalToRender(10.0F);
            Msdf.text(
               context,
               MsdfManager.BOLD,
               "Конфиги",
               px,
               cursorY + ClickGuiLayout.physicalToRender(4.0F),
               text,
               ClickGuiTheme.alpha(ClickGuiTheme.TEXT, alpha),
               true
            );
            Msdf.text(
               context,
               MsdfManager.ICONS,
               "\ue006",
               this.x + this.width - layout.padding - text,
               cursorY + 2.0F,
               text,
               ClickGuiTheme.accentAt(this.x + this.width, Math.round(alpha * 255.0F)),
               true
            );
            cursorY += titleHeight;
            float innerWidth = this.width - layout.padding * 2.0F;
            Render2D.rect(context, px, cursorY, innerWidth, searchHeight, layout.cardRadius, ClickGuiTheme.alpha(ClickGuiTheme.SURFACE, alpha), true);
            String input = this.creating ? this.newConfigName : this.search;
            String placeholder = this.creating ? "Название конфига" : "Поиск...";
            Msdf.text(
               context,
               MsdfManager.BOLD,
               input.isEmpty() ? placeholder : input,
               px + ClickGuiLayout.physicalToRender(9.0F),
               cursorY + ClickGuiLayout.physicalToRender(8.0F),
               secondary,
               ClickGuiTheme.alpha(input.isEmpty() ? ClickGuiTheme.TEXT_FAINT : ClickGuiTheme.TEXT, alpha),
               true
            );
            cursorY += searchHeight + ClickGuiLayout.physicalToRender(5.0F);
            List<UserConfig> configs = this.filteredConfigs();
            float listTop = cursorY;
            Scissor.push(px, listTop, innerWidth, listHeight, true);
            this.scroll = this.scroll + (this.targetScroll - this.scroll) * Math.min(1.0F, dt * 14.0F);
            cursorY += this.scroll;
            UserConfig active = UserConfigManager.getInstance().getActiveConfig();

            for (UserConfig config : configs) {
               boolean selected = active != null && active.getFileName().equals(config.getFileName());
               boolean hovered = inside(mouseX, mouseY, px, cursorY, innerWidth, rowHeight);
               if (selected || hovered) {
                  Render2D.rect(
                     context,
                     px,
                     cursorY,
                     innerWidth,
                     rowHeight - ClickGuiLayout.physicalToRender(3.0F),
                     layout.cardRadius,
                     ClickGuiTheme.alpha(selected ? ClickGuiTheme.SURFACE_ACTIVE : ClickGuiTheme.SURFACE_HOVER, alpha),
                     true
                  );
               }

               float rowTextY = cursorY + (rowHeight - Msdf.height(MsdfManager.BOLD, secondary)) / 2.0F;
               Msdf.text(
                  context,
                  MsdfManager.BOLD,
                  config.getName(),
                  px + ClickGuiLayout.physicalToRender(8.0F),
                  rowTextY,
                  secondary,
                  ClickGuiTheme.alpha(selected ? ClickGuiTheme.TEXT : ClickGuiTheme.TEXT_MUTED, alpha),
                  true
               );
               cursorY += rowHeight;
            }

            Scissor.pop(true);
            float actionsY = listTop + listHeight + ClickGuiLayout.physicalToRender(5.0F);
            float buttonGap = ClickGuiLayout.physicalToRender(5.0F);
            float buttonWidth = (innerWidth - buttonGap) / 2.0F;
            this.drawButton(
               context, px, actionsY, buttonWidth, actionsHeight - ClickGuiLayout.physicalToRender(6.0F), this.creating ? "Создать" : "Новый", alpha, layout
            );
            this.drawButton(
               context, px + buttonWidth + buttonGap, actionsY, buttonWidth, actionsHeight - ClickGuiLayout.physicalToRender(6.0F), "Сохранить", alpha, layout
            );
         }
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button, ClickGuiLayout layout) {
      if (!(this.animation < 0.2F) && this.contains(mouseX, mouseY)) {
         float rowHeight = ClickGuiLayout.physicalToRender(27.0F);
         float titleHeight = ClickGuiLayout.physicalToRender(32.0F);
         float searchHeight = ClickGuiLayout.physicalToRender(27.0F);
         float actionsHeight = ClickGuiLayout.physicalToRender(34.0F);
         float px = this.x + layout.padding;
         float innerWidth = this.width - layout.padding * 2.0F;
         float searchY = this.y + layout.padding + titleHeight;
         if (inside(mouseX, mouseY, px, searchY, innerWidth, searchHeight)) {
            this.searchFocused = true;
            return true;
         }

         List<UserConfig> configs = this.filteredConfigs();
         int visibleRows = Math.min(6, Math.max(2, configs.size()));
         float listHeight = rowHeight * visibleRows;
         float cursorY = searchY + searchHeight + ClickGuiLayout.physicalToRender(5.0F) + this.scroll;

         for (UserConfig config : configs) {
            if (inside(mouseX, mouseY, px, cursorY, innerWidth, rowHeight)) {
               UserConfigManager.getInstance().loadConfig(config);
               return true;
            }

            cursorY += rowHeight;
         }

         float actionsY = searchY + searchHeight + ClickGuiLayout.physicalToRender(5.0F) + listHeight + ClickGuiLayout.physicalToRender(5.0F);
         float buttonGap = ClickGuiLayout.physicalToRender(5.0F);
         float buttonWidth = (innerWidth - buttonGap) / 2.0F;
         if (!inside(mouseX, mouseY, px, actionsY, buttonWidth, actionsHeight)) {
            if (inside(mouseX, mouseY, px + buttonWidth + buttonGap, actionsY, buttonWidth, actionsHeight)) {
               UserConfig active = UserConfigManager.getInstance().getActiveConfig();
               if (active != null) {
                  UserConfigManager.getInstance().saveConfig(active.getName());
               }

               return true;
            } else {
               return true;
            }
         } else {
            if (this.creating && !this.newConfigName.isBlank()) {
               UserConfigManager.getInstance().saveConfig(this.newConfigName.trim());
               this.newConfigName = "";
               this.creating = false;
            } else {
               this.creating = true;
               this.searchFocused = true;
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public boolean keyPressed(KeyInput input) {
      if (input.key() == 256) {
         if (this.creating) {
            this.creating = false;
            this.newConfigName = "";
         } else {
            this.closeRequested = true;
         }

         return true;
      } else {
         if (!this.searchFocused) {
            return false;
         }

         if (input.key() == 259) {
            String value = this.creating ? this.newConfigName : this.search;
            if (!value.isEmpty()) {
               value = value.substring(0, value.length() - 1);
            }

            if (this.creating) {
               this.newConfigName = value;
            } else {
               this.search = value;
            }

            return true;
         } else if (input.key() == 257 && this.creating && !this.newConfigName.isBlank()) {
            UserConfigManager.getInstance().saveConfig(this.newConfigName.trim());
            this.newConfigName = "";
            this.creating = false;
            return true;
         } else {
            return false;
         }
      }
   }

   public boolean charTyped(CharInput input) {
      if (this.searchFocused && !Character.isISOControl(input.codepoint())) {
         String chars = new String(Character.toChars(input.codepoint()));
         if (this.creating) {
            if (this.newConfigName.length() < 28) {
               this.newConfigName = this.newConfigName + chars;
            }
         } else if (this.search.length() < 40) {
            this.search = this.search + chars;
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double amount, ClickGuiLayout layout) {
      int count = this.filteredConfigs().size();
      int visible = Math.min(6, Math.max(2, count));
      float maxScroll = Math.min(0.0F, (visible - count) * ClickGuiLayout.physicalToRender(27.0F));
      this.targetScroll = Math.max(maxScroll, Math.min(0.0F, this.targetScroll + (float)amount * ClickGuiLayout.physicalToRender(24.0F)));
      return true;
   }

   public boolean contains(double mouseX, double mouseY) {
      return inside(mouseX, mouseY, this.x, this.y, this.width, this.height);
   }

   public float getAnimation() {
      return this.animation;
   }

   public boolean consumeCloseRequest() {
      boolean value = this.closeRequested;
      this.closeRequested = false;
      return value;
   }

   public void resetInput() {
      this.searchFocused = false;
      this.creating = false;
      this.newConfigName = "";
   }

   private List<UserConfig> filteredConfigs() {
      List<UserConfig> result = new ArrayList<>();
      String query = this.search.trim().toLowerCase(Locale.ROOT);

      for (UserConfig config : UserConfigManager.getInstance().getConfigs()) {
         if (query.isEmpty() || config.getName().toLowerCase(Locale.ROOT).contains(query)) {
            result.add(config);
         }
      }

      return result;
   }

   private void drawButton(DrawContext context, float x, float y, float width, float height, String text, float alpha, ClickGuiLayout layout) {
      Render2D.rect(context, x, y, width, height, layout.cardRadius, ClickGuiTheme.alpha(ClickGuiTheme.SURFACE_HOVER, alpha), true);
      float fontSize = ClickGuiLayout.physicalToRender(10.0F);
      float textWidth = Msdf.width(MsdfManager.BOLD, text, fontSize);
      Msdf.text(
         context,
         MsdfManager.BOLD,
         text,
         x + (width - textWidth) / 2.0F,
         y + (height - fontSize) / 2.0F,
         fontSize,
         ClickGuiTheme.alpha(ClickGuiTheme.TEXT, alpha),
         true
      );
   }

   private static boolean inside(double mx, double my, float x, float y, float width, float height) {
      return mx >= x && mx <= x + width && my >= y && my <= y + height;
   }
}
