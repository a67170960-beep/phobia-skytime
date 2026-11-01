package ruhack.phobia.system.screensystem.clickgui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import ruhack.phobia.system.configsystem.impl.userconfig.UserConfig;
import ruhack.phobia.system.configsystem.impl.userconfig.UserConfigManager;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.render.scissor.Scissor;

public class ConfigGui {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final int TEXT_MAIN = ColorUtil.rgba(255, 255, 255, 255);
   private int selectedTab = 0;
   private float scroll = 0.0F;
   private float targetScroll = 0.0F;
   private float contentHeight = 0.0F;
   private boolean creatingConfig = false;
   private String text = "";
   private boolean inputFocused = false;
   private int cursorPosition = 0;
   private int selectionStart = -1;
   private int selectionEnd = -1;
   private long lastClickTime = 0L;
   private float focusAnimation = 0.0F;
   private float textScrollOffset = 0.0F;
   private float targetScrollOffset = 0.0F;
   private float cursorBlinkAnimation = 0.0F;
   private float selectionAnimation = 0.0F;
   private float animatedCursorX = 0.0F;
   private float bgRed = 10.0F;
   private float outlineRed = 26.0F;
   private float outlineGreen = 26.0F;
   private float outlineBlue = 26.0F;
   private long lastUpdateTime = System.currentTimeMillis();
   private long lastInputTime = 0L;
   private char lastChar = 0;
   private static final float ANIMATION_SPEED = 2.0F;
   private static final float COLOR_ANIMATION_SPEED = 1.5F;
   private static final float CURSOR_ANIMATION_SPEED = 12.0F;
   private static final float SCROLL_ANIMATION_SPEED = 5.0F;
   private static final float TEXT_PADDING = 8.0F;
   private String selectedConfigName = "";
   private final Map<String, Float> loadBtnAnimations = new HashMap<>();
   private final Map<String, Float> deleteBtnAnimations = new HashMap<>();
   private final Map<Integer, Float> tabAnimations = new HashMap<>();
   private float createBtnAnimation = 0.0F;
   private float refreshBtnAnimation = 0.0F;
   private float createPanelAnimation = 0.0F;
   private float saveBtnAnimation = 0.0F;
   private float cancelBtnAnimation = 0.0F;
   private final Map<String, Float> configPopAnimations = new HashMap<>();
   private final Map<String, UserConfig> configsCache = new HashMap<>();
   private final List<String> knownNamesOrder = new ArrayList<>();
   private float currentDeltaTime = 0.0F;
   private float globalAlpha = 0.0F;
   private boolean visible = false;
   private float x;
   private float y;
   private float width;
   private float height;

   private float getDeltaTime() {
      long currentTime = System.currentTimeMillis();
      float deltaTime = Math.min((float)(currentTime - this.lastUpdateTime) / 1000.0F, 0.1F);
      this.lastUpdateTime = currentTime;
      return deltaTime;
   }

   private float lerp(float current, float target, float speed) {
      float diff = target - current;
      return Math.abs(diff) < 0.001F ? target : current + diff * Math.min(speed, 1.0F);
   }

   public void render(DrawContext context, float x, float y, float width, float height, int mouseX, int mouseY, float alpha) {
      ClickGuiTheme.syncAccent();
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.currentDeltaTime = this.getDeltaTime();
      this.globalAlpha = this.lerp(this.globalAlpha, (this.visible ? 1.0F : 0.0F) * alpha, this.currentDeltaTime * 6.0F);
      if (!(this.globalAlpha < 0.001F)) {
         double scaledMouseX = Render2D.convertX(mouseX);
         double scaledMouseY = Render2D.convertY(mouseY);
         this.renderTabs(context, x, y, width, scaledMouseX, scaledMouseY);
         this.renderContent(context, x, y + 30.0F, width, height - 30.0F, (int)scaledMouseX, (int)scaledMouseY);
      }
   }

   public void setVisible(boolean visible) {
      this.visible = visible;
   }

   public float getGlobalAlpha() {
      return this.globalAlpha;
   }

   public boolean isVisible() {
      return this.visible;
   }

   private void renderTabs(DrawContext context, float x, float y, float width, double mouseX, double mouseY) {
      float tabHeight = 16.0F;
      float tabSpacing = 6.0F;
      float tabY = y + 5.0F;
      float startX = x + 8.0F;
      String[] tabLabels = new String[]{"Все", "Облачные", "Локальные"};
      String[] tabIcons = new String[]{"\ue009", "\ue006", "\ue00a"};
      float currentX = startX;

      for (int i = 0; i < tabLabels.length; i++) {
         float iconWidth = MsdfManager.ICONS != null ? Msdf.width(MsdfManager.ICONS, tabIcons[i], 10.0F) : 0.0F;
         float textWidth = MsdfManager.BOLD != null ? Msdf.width(MsdfManager.BOLD, tabLabels[i], 6.5F) : 0.0F;
         float tabWidth = iconWidth + textWidth + 16.0F;
         boolean active = this.selectedTab == i;
         boolean hovered = this.isInside(mouseX, mouseY, currentX, tabY, tabWidth, tabHeight);
         float tabAnim = this.tabAnimations.getOrDefault(i, 0.0F);
         tabAnim = this.lerp(tabAnim, !active && !hovered ? 0.0F : 1.0F, this.currentDeltaTime * 3.0F);
         this.tabAnimations.put(i, tabAnim);
         if (active) {
            Render2D.zippy(context, currentX, tabY, tabWidth, tabHeight, 4.0F, ClickGuiTheme.accent((int)(105.0F * this.globalAlpha)), true);
            Render2D.outline(context, currentX, tabY, tabWidth, tabHeight, 4.0F, 0.8F, ClickGuiTheme.accent((int)(255.0F * this.globalAlpha)), true);
         } else {
            int bgAlpha = (int)((1.0F - tabAnim) * 255.0F * this.globalAlpha);
            Render2D.rect(context, currentX, tabY, tabWidth, tabHeight, 4.0F, ColorUtil.rgba(10, 10, 10, bgAlpha), true);
            int zippyAlpha = (int)(tabAnim * 105.0F * this.globalAlpha);
            if (zippyAlpha > 0) {
               Render2D.zippy(context, currentX, tabY, tabWidth, tabHeight, 4.0F, ClickGuiTheme.accent(zippyAlpha), true);
            }

            int outlineAlpha = (int)Math.max(0.0F, Math.min(255.0F, tabAnim * 255.0F * this.globalAlpha));
            int outlineR = (int)(26.0F + tabAnim * 229.0F);
            int outlineG = (int)(26.0F + tabAnim * 16.0F);
            Render2D.outline(context, currentX, tabY, tabWidth, tabHeight, 4.0F, 0.8F, ColorUtil.rgba(outlineR, outlineG, outlineG, outlineAlpha), true);
         }

         int iconGray = (int)(102.0F + tabAnim * 153.0F);
         int textGray = (int)(102.0F + tabAnim * 153.0F);
         if (MsdfManager.ICONS != null) {
            Msdf.text(
               context,
               MsdfManager.ICONS,
               tabIcons[i],
               currentX + 5.0F,
               tabY + 4.0F,
               10.0F,
               active
                  ? ClickGuiTheme.accentAt(currentX, Math.round(this.globalAlpha * 255.0F))
                  : ColorUtil.rgba(iconGray, iconGray, iconGray, (int)(255.0F * this.globalAlpha)),
               true
            );
         }

         if (MsdfManager.BOLD != null) {
            Msdf.text(
               context,
               MsdfManager.BOLD,
               tabLabels[i],
               currentX + iconWidth + 10.0F,
               tabY + 4.0F,
               6.5F,
               active ? ColorUtil.multiplyAlpha(TEXT_MAIN, this.globalAlpha) : ColorUtil.rgba(textGray, textGray, textGray, (int)(255.0F * this.globalAlpha)),
               true
            );
         }

         currentX += tabWidth + tabSpacing;
      }

      String refreshIcon = "\ue011";
      String refreshText = "Обновить список";
      float refreshIconWidth = MsdfManager.ICONS != null ? Msdf.width(MsdfManager.ICONS, refreshIcon, 10.0F) : 0.0F;
      float refreshTextWidth = MsdfManager.BOLD != null ? Msdf.width(MsdfManager.BOLD, refreshText, 6.5F) : 0.0F;
      float btnWidth = refreshIconWidth + refreshTextWidth + 16.0F;
      float btnX = x + width - btnWidth - 8.0F;
      boolean btnHovered = this.isInside(mouseX, mouseY, btnX, tabY, btnWidth, tabHeight);
      this.refreshBtnAnimation = this.lerp(this.refreshBtnAnimation, btnHovered ? 1.0F : 0.0F, this.currentDeltaTime * 3.0F);
      int refreshBgAlpha = (int)((1.0F - this.refreshBtnAnimation) * 255.0F * this.globalAlpha);
      Render2D.rect(context, btnX, tabY, btnWidth, tabHeight, 4.0F, ColorUtil.rgba(10, 10, 10, refreshBgAlpha), true);
      int refreshZippyAlpha = (int)(this.refreshBtnAnimation * 105.0F * this.globalAlpha);
      if (refreshZippyAlpha > 0) {
         Render2D.zippy(context, btnX, tabY, btnWidth, tabHeight, 4.0F, ClickGuiTheme.accent(refreshZippyAlpha), true);
      }

      int refreshOutlineAlpha = (int)Math.max(0.0F, Math.min(255.0F, this.refreshBtnAnimation * 255.0F * this.globalAlpha));
      int refreshOutlineR = (int)(26.0F + this.refreshBtnAnimation * 229.0F);
      int refreshOutlineG = (int)(26.0F + this.refreshBtnAnimation * 16.0F);
      Render2D.outline(
         context, btnX, tabY, btnWidth, tabHeight, 4.0F, 0.8F, ColorUtil.rgba(refreshOutlineR, refreshOutlineG, refreshOutlineG, refreshOutlineAlpha), true
      );
      if (MsdfManager.ICONS != null) {
         Msdf.text(
            context, MsdfManager.ICONS, refreshIcon, btnX + 5.0F, tabY + 4.0F, 10.0F, ClickGuiTheme.accentAt(btnX, Math.round(this.globalAlpha * 255.0F)), true
         );
      }

      if (MsdfManager.BOLD != null) {
         Msdf.text(
            context,
            MsdfManager.BOLD,
            refreshText,
            btnX + refreshIconWidth + 10.0F,
            tabY + 4.0F,
            6.5F,
            ColorUtil.multiplyAlpha(TEXT_MAIN, this.globalAlpha),
            true
         );
      }
   }

   private void renderContent(DrawContext context, float x, float y, float width, float height, int mouseX, int mouseY) {
      Scissor.push(x, y, width, height, true);
      this.scroll = this.scroll + (this.targetScroll - this.scroll) * 0.06F;
      this.createPanelAnimation = this.lerp(this.createPanelAnimation, this.creatingConfig ? 1.0F : 0.0F, this.currentDeltaTime * 2.0F);
      this.renderConfigList(context, x, y + this.scroll, width, height, mouseX, mouseY);
      if (this.createPanelAnimation > 0.001F) {
         this.renderCreateConfigPanel(context, x, y, width, height, mouseX, mouseY);
      }

      Scissor.pop(true);
      if (this.contentHeight > height) {
         float viewableRatio = height / this.contentHeight;
         float scrollbarHeight = Math.max(20.0F, height * viewableRatio);
         float scrollableTrack = height - scrollbarHeight;
         float scrollableContent = this.contentHeight - height;
         float scrollPercentage = Math.min(1.0F, Math.max(0.0F, -this.scroll / scrollableContent));
         float scrollbarY = y + scrollPercentage * scrollableTrack;
         Render2D.rect(context, x + width - 4.0F, scrollbarY, 2.0F, scrollbarHeight, 1.0F, ClickGuiTheme.accent((int)(200.0F * this.globalAlpha)), true);
      }
   }

   private void renderConfigList(DrawContext context, float x, float y, float width, float height, int mouseX, int mouseY) {
      float curY = y;
      float itemHeight = 28.0F;
      float spacing = 6.0F;
      String createIcon = "+";
      String createText = "Создать конфиг";
      float createIconWidth = MsdfManager.ICONS != null ? Msdf.width(MsdfManager.ICONS, createIcon, 10.0F) : 0.0F;
      float createTextWidth = MsdfManager.BOLD != null ? Msdf.width(MsdfManager.BOLD, createText, 6.5F) : 0.0F;
      float createBtnWidth = createIconWidth + createTextWidth + 16.0F;
      float createBtnHeight = 16.0F;
      float createBtnX = x + 8.0F;
      boolean createHovered = this.isInside(mouseX, mouseY, createBtnX, curY, createBtnWidth, createBtnHeight);
      this.createBtnAnimation = this.lerp(this.createBtnAnimation, createHovered ? 1.0F : 0.0F, this.currentDeltaTime * 3.0F);
      int zippyAlpha = (int)(this.createBtnAnimation * 105.0F * this.globalAlpha);
      if (zippyAlpha > 0) {
         Render2D.zippy(context, createBtnX, curY, createBtnWidth, createBtnHeight, 4.0F, ClickGuiTheme.accent(zippyAlpha), true);
      }

      if (this.createBtnAnimation < 1.0F) {
         int rectAlpha = (int)((1.0F - this.createBtnAnimation) * 255.0F * this.globalAlpha);
         Render2D.rect(context, createBtnX, curY, createBtnWidth, createBtnHeight, 4.0F, ColorUtil.rgba(10, 10, 10, rectAlpha), true);
      }

      int outlineAlpha = (int)Math.max(0.0F, Math.min(255.0F, this.createBtnAnimation * 255.0F * this.globalAlpha));
      Render2D.outline(context, createBtnX, curY, createBtnWidth, createBtnHeight, 4.0F, 0.8F, ClickGuiTheme.accent(outlineAlpha), true);
      if (MsdfManager.ICONS != null) {
         Msdf.text(
            context,
            MsdfManager.ICONS,
            createIcon,
            createBtnX + 5.0F,
            curY + 4.0F,
            10.0F,
            ClickGuiTheme.accentAt(createBtnX, Math.round(this.globalAlpha * 255.0F)),
            true
         );
      }

      if (MsdfManager.BOLD != null) {
         Msdf.text(
            context,
            MsdfManager.BOLD,
            createText,
            createBtnX + createIconWidth + 10.0F,
            curY + 4.0F,
            6.5F,
            ColorUtil.multiplyAlpha(TEXT_MAIN, this.globalAlpha),
            true
         );
      }

      curY += createBtnHeight + spacing;
      List<UserConfig> managerConfigs = UserConfigManager.getInstance().getConfigs();
      Set<String> visibleInTab = new HashSet<>();

      for (UserConfig c : managerConfigs) {
         if (!this.configsCache.containsKey(c.getName())) {
            this.knownNamesOrder.add(c.getName());
         }

         this.configsCache.put(c.getName(), c);
         if (this.selectedTab == 0 || this.selectedTab == 1 && c.isCloud() || this.selectedTab == 2 && !c.isCloud()) {
            visibleInTab.add(c.getName());
         }
      }

      boolean anyVisible = false;
      float speed = 2.0F;

      for (int i = 0; i < this.knownNamesOrder.size(); i++) {
         String name = this.knownNamesOrder.get(i);
         float target = visibleInTab.contains(name) ? 1.0F : 0.0F;
         float current = this.configPopAnimations.getOrDefault(name, 0.0F);
         if (current < target) {
            current = Math.min(target, current + this.currentDeltaTime * speed);
         } else if (current > target) {
            current = Math.max(target, current - this.currentDeltaTime * speed);
         }

         this.configPopAnimations.put(name, current);
         if (current > 0.001F) {
            anyVisible = true;
            UserConfig config = this.configsCache.get(name);
            if (config != null) {
               boolean selected = this.selectedConfigName.equals(name);
               float currentH = itemHeight * current;
               boolean hovered = this.isInside(mouseX, mouseY, x + 8.0F, curY, width - 16.0F, currentH);
               this.renderConfigItem(context, config, x + 8.0F, curY, width - 16.0F, itemHeight, selected, hovered, mouseX, mouseY, name, current);
               curY += (itemHeight + spacing) * current;
            }
         } else {
            boolean exists = false;

            for (UserConfig mc : managerConfigs) {
               if (mc.getName().equals(name)) {
                  exists = true;
                  break;
               }
            }

            if (!exists) {
               this.knownNamesOrder.remove(i--);
               this.configsCache.remove(name);
               this.configPopAnimations.remove(name);
            }
         }
      }

      if (!anyVisible && managerConfigs.isEmpty() && MsdfManager.BOLD != null) {
         String emptyText = "Нет сохранённых конфигов";
         float textW = Msdf.width(MsdfManager.BOLD, emptyText, 7.0F);
         Msdf.text(
            context,
            MsdfManager.BOLD,
            emptyText,
            x + (width - textW) / 2.0F,
            curY + 15.0F,
            7.0F,
            ColorUtil.rgba(102, 102, 102, (int)(255.0F * this.globalAlpha)),
            true
         );
      }

      this.contentHeight = curY - y + 8.0F;
   }

   private void renderConfigItem(
      DrawContext context,
      UserConfig config,
      float x,
      float y,
      float width,
      float height,
      boolean selected,
      boolean hovered,
      int mouseX,
      int mouseY,
      String name,
      float anim
   ) {
      float finalAlpha = anim * this.globalAlpha;
      int itemBgCol = hovered ? ColorUtil.rgba(15, 15, 15, (int)(255.0F * finalAlpha)) : ColorUtil.rgba(10, 10, 10, (int)(255.0F * finalAlpha));
      int itemBorderCol = ColorUtil.rgba(26, 26, 26, (int)(255.0F * finalAlpha));
      if (selected) {
         Render2D.zippy(context, x, y, width, height, 4.0F, ClickGuiTheme.accent((int)(60.0F * finalAlpha)), true);
         Render2D.outline(context, x, y, width, height, 4.0F, 0.8F, ClickGuiTheme.accent((int)(255.0F * finalAlpha)), true);
      } else {
         Render2D.rect(context, x, y, width, height, 4.0F, itemBgCol, true);
         Render2D.outline(context, x, y, width, height, 4.0F, 0.8F, itemBorderCol, true);
      }

      float iconSize = 20.0F;
      float iconX = x + 5.0F;
      float iconY = y + (height - iconSize) / 2.0F;
      Render2D.rect(context, iconX, iconY, iconSize, iconSize, 3.0F, ColorUtil.rgba(20, 20, 20, (int)(255.0F * finalAlpha)), true);
      Render2D.outline(context, iconX, iconY, iconSize, iconSize, 3.0F, 0.8F, itemBorderCol, true);
      if (MsdfManager.ICONS != null) {
         Msdf.text(context, MsdfManager.ICONS, "q", iconX + 2.0F, iconY + 4.0F, 16.0F, ClickGuiTheme.accent((int)(255.0F * finalAlpha)), true);
      }

      float textX = iconX + iconSize + 8.0F;
      float topY = y + 4.0F;
      if (MsdfManager.BOLD != null) {
         Msdf.text(context, MsdfManager.BOLD, config.getName(), textX, topY, 7.0F, ColorUtil.rgba(255, 255, 255, (int)(255.0F * finalAlpha)), true);
         float nameWidth = Msdf.width(MsdfManager.BOLD, config.getName(), 7.0F);
         String typeIcon = config.isCloud() ? "\ue006" : "\ue00a";
         String typeText = config.isCloud() ? "Cloud" : "Local";
         float iconWidth = MsdfManager.ICONS != null ? Msdf.width(MsdfManager.ICONS, typeIcon, 8.0F) : 0.0F;
         float typeTextWidth = Msdf.width(MsdfManager.BOLD, typeText, 5.0F);
         float typeBadgeW = iconWidth + typeTextWidth + 14.0F;
         float typeBadgeH = 12.0F;
         float typeBadgeX = textX + nameWidth + 6.0F;
         float typeBadgeY = topY - 1.0F;
         Render2D.rect(context, typeBadgeX, typeBadgeY, typeBadgeW, typeBadgeH, 4.0F, itemBgCol, true);
         Render2D.outline(context, typeBadgeX, typeBadgeY, typeBadgeW, typeBadgeH, 4.0F, 0.8F, itemBorderCol, true);
         if (MsdfManager.ICONS != null) {
            Msdf.text(context, MsdfManager.ICONS, typeIcon, typeBadgeX + 4.0F, typeBadgeY + 2.5F, 8.0F, ClickGuiTheme.accent((int)(255.0F * finalAlpha)), true);
         }

         Msdf.text(
            context, MsdfManager.BOLD, typeText, typeBadgeX + iconWidth + 7.0F, typeBadgeY + 2.5F, 5.0F, ClickGuiTheme.accent((int)(255.0F * finalAlpha)), true
         );
      }

      float bottomY = y + height - 11.0F;
      if (MsdfManager.ICONS != null && MsdfManager.BOLD != null) {
         float curX = textX;
         Msdf.text(context, MsdfManager.ICONS, "\ue00e", curX, bottomY, 8.0F, ClickGuiTheme.accent((int)(255.0F * finalAlpha)), true);
         curX += 10.0F;
         Msdf.text(context, MsdfManager.BOLD, "Автор:", curX, bottomY, 5.5F, ColorUtil.rgba(180, 180, 180, (int)(255.0F * finalAlpha)), true);
         curX += Msdf.width(MsdfManager.BOLD, "Автор:", 5.5F) + 3.0F;
         Msdf.text(context, MsdfManager.BOLD, config.getAuthor(), curX, bottomY, 5.5F, ColorUtil.rgba(102, 102, 102, (int)(255.0F * finalAlpha)), true);
         curX += Msdf.width(MsdfManager.BOLD, config.getAuthor(), 5.5F) + 10.0F;
         Msdf.text(context, MsdfManager.ICONS, "J", curX, bottomY, 8.0F, ClickGuiTheme.accent((int)(255.0F * finalAlpha)), true);
         curX += 10.0F;
         Msdf.text(context, MsdfManager.BOLD, "Обновлен:", curX, bottomY, 5.5F, ColorUtil.rgba(180, 180, 180, (int)(255.0F * finalAlpha)), true);
         curX += Msdf.width(MsdfManager.BOLD, "Обновлен:", 5.5F) + 3.0F;
         Msdf.text(context, MsdfManager.BOLD, config.getFormattedDate(), curX, bottomY, 5.5F, ColorUtil.rgba(102, 102, 102, (int)(255.0F * finalAlpha)), true);
      }

      float btnWidth = 50.0F;
      float btnHeight = 14.0F;
      float btnY = y + (height - btnHeight) / 2.0F;
      float loadBtnX = x + width - btnWidth - 6.0F;
      boolean loadHovered = this.isInside(mouseX, mouseY, loadBtnX, btnY, btnWidth, btnHeight);
      float loadAnim = this.loadBtnAnimations.getOrDefault(name, 0.0F);
      loadAnim = this.lerp(loadAnim, loadHovered ? 1.0F : 0.0F, this.currentDeltaTime * 3.0F);
      this.loadBtnAnimations.put(name, loadAnim);
      int loadGreen = (int)(180.0F + loadAnim * 75.0F);
      Render2D.zippy(
         context, loadBtnX, btnY, btnWidth, btnHeight, 3.0F, ColorUtil.rgba(42, loadGreen, 42, (int)((105.0F + loadAnim * 50.0F) * finalAlpha)), true
      );
      Render2D.outline(
         context, loadBtnX, btnY, btnWidth, btnHeight, 3.0F, 0.8F, ColorUtil.rgba(42, loadGreen, 42, (int)((60.0F + loadAnim * 40.0F) * finalAlpha)), true
      );
      if (MsdfManager.BOLD != null) {
         float loadTextWidth = Msdf.width(MsdfManager.BOLD, "Загрузить", 5.5F);
         Msdf.text(
            context,
            MsdfManager.BOLD,
            "Загрузить",
            loadBtnX + (btnWidth - loadTextWidth) / 2.0F,
            btnY + 3.5F,
            5.5F,
            ColorUtil.rgba(255, 255, 255, (int)(255.0F * finalAlpha)),
            true
         );
      }

      float deleteBtnX = loadBtnX - btnWidth - 4.0F;
      boolean deleteHovered = this.isInside(mouseX, mouseY, deleteBtnX, btnY, btnWidth, btnHeight);
      float deleteAnim = this.deleteBtnAnimations.getOrDefault(name, 0.0F);
      deleteAnim = this.lerp(deleteAnim, deleteHovered ? 1.0F : 0.0F, this.currentDeltaTime * 3.0F);
      this.deleteBtnAnimations.put(name, deleteAnim);
      int deleteRed = (int)(180.0F + deleteAnim * 75.0F);
      Render2D.zippy(
         context, deleteBtnX, btnY, btnWidth, btnHeight, 3.0F, ColorUtil.rgba(deleteRed, 42, 42, (int)((105.0F + deleteAnim * 50.0F) * finalAlpha)), true
      );
      Render2D.outline(
         context, deleteBtnX, btnY, btnWidth, btnHeight, 3.0F, 0.8F, ColorUtil.rgba(deleteRed, 42, 42, (int)((60.0F + deleteAnim * 40.0F) * finalAlpha)), true
      );
      if (MsdfManager.BOLD != null) {
         float delTextWidth = Msdf.width(MsdfManager.BOLD, "Удалить", 5.5F);
         Msdf.text(
            context,
            MsdfManager.BOLD,
            "Удалить",
            deleteBtnX + (btnWidth - delTextWidth) / 2.0F,
            btnY + 3.5F,
            5.5F,
            ColorUtil.rgba(255, 255, 255, (int)(255.0F * finalAlpha)),
            true
         );
      }
   }

   private void renderCreateConfigPanel(DrawContext context, float x, float y, float width, float height, int mouseX, int mouseY) {
      float deltaTime = this.currentDeltaTime;
      float panelWidth = 160.0F;
      float panelHeight = 80.0F;
      float panelX = x + (width - panelWidth) / 2.0F;
      float panelY = y + (height - panelHeight) / 2.0F - 20.0F * (1.0F - this.createPanelAnimation);
      int panelAlpha = (int)(255.0F * this.createPanelAnimation);
      int bgCol = ColorUtil.rgba(13, 13, 13, panelAlpha);
      int borderCol = ColorUtil.rgba(30, 30, 30, panelAlpha);
      Render2D.rect(context, panelX, panelY, panelWidth, panelHeight, 6.0F, bgCol, true);
      Render2D.outline(context, panelX, panelY, panelWidth, panelHeight, 6.0F, 0.8F, borderCol, true);
      if (MsdfManager.BOLD != null) {
         Msdf.text(context, MsdfManager.BOLD, "Новый конфиг", panelX + 10.0F, panelY + 10.0F, 7.5F, ColorUtil.rgba(255, 255, 255, panelAlpha), true);
      }

      float inputX = panelX + 10.0F;
      float inputY = panelY + 28.0F;
      float inputW = panelWidth - 20.0F;
      float inputH = 18.0F;
      this.focusAnimation = this.lerp(this.focusAnimation, this.inputFocused ? 1.0F : 0.0F, deltaTime * 2.0F);
      if (this.inputFocused) {
         this.cursorBlinkAnimation += deltaTime * 2.0F;
         if (this.cursorBlinkAnimation > 1.0F) {
            this.cursorBlinkAnimation--;
         }
      } else {
         this.cursorBlinkAnimation = 0.0F;
      }

      float targetBgR = this.inputFocused ? 15.0F : 10.0F;
      this.bgRed = this.lerp(this.bgRed, targetBgR, deltaTime * 1.5F);
      float targetOutR = this.inputFocused ? 255.0F : 26.0F;
      float targetOutG = this.inputFocused ? 42.0F : 26.0F;
      this.outlineRed = this.lerp(this.outlineRed, targetOutR, deltaTime * 1.5F);
      this.outlineGreen = this.lerp(this.outlineGreen, targetOutG, deltaTime * 1.5F);
      this.outlineBlue = this.outlineGreen;
      int inputBgColor = ColorUtil.rgba((int)this.bgRed, (int)this.bgRed, (int)this.bgRed, panelAlpha);
      Render2D.rect(context, inputX, inputY, inputW, inputH, 4.0F, inputBgColor, true);
      int inputOutlineColor = ColorUtil.rgba((int)this.outlineRed, (int)this.outlineGreen, (int)this.outlineBlue, panelAlpha);
      Render2D.outline(context, inputX, inputY, inputW, inputH, 4.0F, 0.8F, inputOutlineColor, true);
      this.renderTextInput(context, inputX, inputY, inputW, inputH, deltaTime, panelAlpha);
      float btnW = 65.0F;
      float btnH = 14.0F;
      float btnY = panelY + panelHeight - btnH - 10.0F;
      float saveBtnX = panelX + panelWidth - btnW - 10.0F;
      boolean saveHovered = this.isInside(mouseX, mouseY, saveBtnX, btnY, btnW, btnH);
      this.saveBtnAnimation = this.lerp(this.saveBtnAnimation, saveHovered ? 1.0F : 0.0F, deltaTime * 3.0F);
      int saveGreen = (int)(180.0F + this.saveBtnAnimation * 75.0F);
      int saveAlpha = (int)(105.0F + this.saveBtnAnimation * 50.0F);
      Render2D.zippy(context, saveBtnX, btnY, btnW, btnH, 3.0F, ColorUtil.rgba(42, saveGreen, 42, (int)(saveAlpha * this.createPanelAnimation)), true);
      Render2D.outline(
         context,
         saveBtnX,
         btnY,
         btnW,
         btnH,
         3.0F,
         0.8F,
         ColorUtil.rgba(42, saveGreen, 42, (int)((60.0F + this.saveBtnAnimation * 40.0F) * this.createPanelAnimation)),
         true
      );
      if (MsdfManager.BOLD != null) {
         float tw = Msdf.width(MsdfManager.BOLD, "Сохранить", 5.5F);
         Msdf.text(context, MsdfManager.BOLD, "Сохранить", saveBtnX + (btnW - tw) / 2.0F, btnY + 3.5F, 5.5F, ColorUtil.rgba(255, 255, 255, panelAlpha), true);
      }

      float cancelBtnX = panelX + 10.0F;
      boolean cancelHovered = this.isInside(mouseX, mouseY, cancelBtnX, btnY, btnW, btnH);
      this.cancelBtnAnimation = this.lerp(this.cancelBtnAnimation, cancelHovered ? 1.0F : 0.0F, deltaTime * 3.0F);
      int cancelRed = (int)(180.0F + this.cancelBtnAnimation * 75.0F);
      int cancelAlpha = (int)(105.0F + this.cancelBtnAnimation * 50.0F);
      Render2D.zippy(context, cancelBtnX, btnY, btnW, btnH, 3.0F, ColorUtil.rgba(cancelRed, 42, 42, (int)(cancelAlpha * this.createPanelAnimation)), true);
      Render2D.outline(
         context,
         cancelBtnX,
         btnY,
         btnW,
         btnH,
         3.0F,
         0.8F,
         ColorUtil.rgba(cancelRed, 42, 42, (int)((60.0F + this.cancelBtnAnimation * 40.0F) * this.createPanelAnimation)),
         true
      );
      if (MsdfManager.BOLD != null) {
         float tw = Msdf.width(MsdfManager.BOLD, "Отмена", 5.5F);
         Msdf.text(context, MsdfManager.BOLD, "Отмена", cancelBtnX + (btnW - tw) / 2.0F, btnY + 3.5F, 5.5F, ColorUtil.rgba(255, 255, 255, panelAlpha), true);
      }
   }

   private void renderTextInput(DrawContext context, float boxX, float boxY, float boxWidth, float boxHeight, float deltaTime, int panelAlpha) {
      float textAreaX = boxX + 8.0F;
      float textAreaWidth = boxWidth - 16.0F;
      float textY = boxY + boxHeight / 2.0F - 5.0F;
      float fullTextWidth = MsdfManager.BOLD != null ? MsdfManager.BOLD.getStringWidth(this.text, 7.5F) : 0.0F;
      if (this.inputFocused) {
         String beforeCursor = this.text.substring(0, this.cursorPosition);
         float targetCursorX = MsdfManager.BOLD != null ? MsdfManager.BOLD.getStringWidth(beforeCursor, 7.5F) : 0.0F;
         if (this.focusAnimation > 0.95F) {
            this.animatedCursorX = this.lerp(this.animatedCursorX, targetCursorX, deltaTime * 12.0F);
         } else {
            this.animatedCursorX = targetCursorX;
         }

         if (this.animatedCursorX - this.targetScrollOffset > textAreaWidth - 2.0F) {
            this.targetScrollOffset = this.animatedCursorX - textAreaWidth + 2.0F;
         } else if (this.animatedCursorX - this.targetScrollOffset < 0.0F) {
            this.targetScrollOffset = this.animatedCursorX;
         }

         if (fullTextWidth <= textAreaWidth) {
            this.targetScrollOffset = 0.0F;
         }

         this.targetScrollOffset = Math.max(0.0F, Math.min(this.targetScrollOffset, Math.max(0.0F, fullTextWidth - textAreaWidth)));
      } else {
         this.targetScrollOffset = 0.0F;
         this.animatedCursorX = 0.0F;
      }

      this.textScrollOffset = this.lerp(this.textScrollOffset, this.targetScrollOffset, deltaTime * 5.0F);
      Scissor.push(textAreaX, boxY, textAreaWidth, boxHeight, true);
      if (this.text.isEmpty() && !this.inputFocused) {
         if (MsdfManager.BOLD != null) {
            float hintAlpha = 200.0F * (1.0F - this.focusAnimation) * (panelAlpha / 255.0F);
            int hintColor = ColorUtil.rgba(80, 80, 80, (int)hintAlpha);
            Msdf.text(context, MsdfManager.BOLD, "Название конфига...", textAreaX, textY, 7.5F, hintColor, true);
         }
      } else {
         if (this.inputFocused && this.hasSelection() && this.selectionAnimation > 0.01F) {
            int start = this.getStartOfSelection();
            int end = this.getEndOfSelection();
            String beforeSelection = this.text.substring(0, start);
            String selection = this.text.substring(start, end);
            float selectionX = textAreaX + (MsdfManager.BOLD != null ? MsdfManager.BOLD.getStringWidth(beforeSelection, 7.5F) : 0.0F) - this.textScrollOffset;
            float selectionWidth = MsdfManager.BOLD != null ? MsdfManager.BOLD.getStringWidth(selection, 7.5F) : 0.0F;
            int selAlpha = (int)(60.0F * this.selectionAnimation * (panelAlpha / 255.0F));
            int selColor = ClickGuiTheme.accent(selAlpha);
            Render2D.rect(context, selectionX, boxY + 4.0F, selectionWidth, boxHeight - 8.0F, 2.0F, selColor, true);
         }

         int currentTextAlpha = (int)((200.0F + this.focusAnimation * 55.0F) * (panelAlpha / 255.0F));
         int textColor = ColorUtil.rgba(255, 255, 255, currentTextAlpha);
         if (MsdfManager.BOLD != null) {
            Msdf.text(context, MsdfManager.BOLD, this.text, textAreaX - this.textScrollOffset, textY, 7.5F, textColor, true);
         }

         if (this.inputFocused && !this.hasSelection()) {
            float cursorAlpha = (float)(Math.sin(this.cursorBlinkAnimation * Math.PI * 2.0) * 0.5 + 0.5);
            if (cursorAlpha > 0.3F) {
               float cursorXPos = textAreaX + this.animatedCursorX - this.textScrollOffset;
               int cursorAlphaInt = (int)(255.0F * cursorAlpha * this.focusAnimation * (panelAlpha / 255.0F));
               int cursorColor = ClickGuiTheme.accent(cursorAlphaInt);
               Render2D.rect(context, cursorXPos, boxY + 5.0F, 1.0F, boxHeight - 10.0F, 0.0F, cursorColor, true);
            }
         }
      }

      Scissor.pop(true);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      double scaledX = Render2D.convertX((float)mouseX);
      double scaledY = Render2D.convertY((float)mouseY);
      List<UserConfig> managerConfigs = UserConfigManager.getInstance().getConfigs();
      float tabHeight = 16.0F;
      float tabSpacing = 6.0F;
      float tabY = this.y + 5.0F;
      float startX = this.x + 8.0F;
      String[] tabLabels = new String[]{"Все", "Облачные", "Локальные"};
      String[] tabIcons = new String[]{"\ue009", "\ue006", "\ue00a"};
      float currentX = startX;

      for (int i = 0; i < tabLabels.length; i++) {
         float iconWidth = MsdfManager.ICONS != null ? Msdf.width(MsdfManager.ICONS, tabIcons[i], 10.0F) : 0.0F;
         float textWidth = MsdfManager.BOLD != null ? Msdf.width(MsdfManager.BOLD, tabLabels[i], 6.5F) : 0.0F;
         float tabWidth = iconWidth + textWidth + 16.0F;
         if (this.isInside(scaledX, scaledY, currentX, tabY, tabWidth, tabHeight)) {
            this.selectedTab = i;
            return true;
         }

         currentX += tabWidth + tabSpacing;
      }

      String refreshIcon = "\ue011";
      String refreshText = "Обновить список";
      float refreshIconWidth = MsdfManager.ICONS != null ? Msdf.width(MsdfManager.ICONS, refreshIcon, 10.0F) : 0.0F;
      float refreshTextWidth = MsdfManager.BOLD != null ? Msdf.width(MsdfManager.BOLD, refreshText, 6.5F) : 0.0F;
      float btnWidth = refreshIconWidth + refreshTextWidth + 16.0F;
      float btnX = this.x + this.width - btnWidth - 8.0F;
      if (this.isInside(scaledX, scaledY, btnX, tabY, btnWidth, tabHeight)) {
         UserConfigManager.getInstance().loadConfigList();
         return true;
      }

      if (this.creatingConfig) {
         return this.handleCreatePanelClick(scaledX, scaledY, button);
      }

      float contentY = this.y + 30.0F;
      float curY = contentY + this.scroll;
      String createIcon = "+";
      String createText = "Создать конфиг";
      float createIconWidth = MsdfManager.ICONS != null ? Msdf.width(MsdfManager.ICONS, createIcon, 10.0F) : 0.0F;
      float createTextWidth = MsdfManager.BOLD != null ? Msdf.width(MsdfManager.BOLD, createText, 6.5F) : 0.0F;
      float createBtnWidth = createIconWidth + createTextWidth + 16.0F;
      float createBtnHeight = 16.0F;
      float createBtnX = this.x + 8.0F;
      if (this.isInside(scaledX, scaledY, createBtnX, curY, createBtnWidth, createBtnHeight)) {
         this.creatingConfig = true;
         this.text = "";
         this.cursorPosition = 0;
         this.inputFocused = true;
         this.clearSelection();
         return true;
      }

      curY += createBtnHeight + 6.0F;
      float itemHeight = 28.0F;
      float spacing = 6.0F;

      for (String name : this.knownNamesOrder) {
         float anim = this.configPopAnimations.getOrDefault(name, 0.0F);
         UserConfig config = this.configsCache.get(name);
         if (config != null && anim > 0.5F) {
            float itemX = this.x + 8.0F;
            float itemWidth = this.width - 16.0F;
            float clickH = itemHeight * anim;
            if (this.isInside(scaledX, scaledY, itemX, curY, itemWidth, clickH)) {
               boolean stillExists = false;

               for (UserConfig mc : managerConfigs) {
                  if (mc.getName().equals(name)) {
                     stillExists = true;
                     break;
                  }
               }

               if (stillExists) {
                  float btnW = 50.0F;
                  float btnH = 14.0F;
                  float btnY = curY + (itemHeight - btnH) / 2.0F;
                  float loadBtnX = itemX + itemWidth - btnW - 6.0F;
                  float deleteBtnX = loadBtnX - btnW - 4.0F;
                  if (this.isInside(scaledX, scaledY, loadBtnX, btnY, btnW, btnH)) {
                     UserConfigManager.getInstance().loadConfig(config);
                     return true;
                  }

                  if (this.isInside(scaledX, scaledY, deleteBtnX, btnY, btnW, btnH)) {
                     UserConfigManager.getInstance().deleteConfig(config);
                     return true;
                  }

                  this.selectedConfigName = name;
                  return true;
               }
            }
         }

         curY += (itemHeight + spacing) * anim;
      }

      return false;
   }

   private boolean handleCreatePanelClick(double mouseX, double mouseY, int button) {
      float contentY = this.y + 30.0F;
      float contentH = this.height - 30.0F;
      float panelWidth = 160.0F;
      float panelHeight = 80.0F;
      float panelX = this.x + (this.width - panelWidth) / 2.0F;
      float panelY = contentY + (contentH - panelHeight) / 2.0F - 20.0F * (1.0F - this.createPanelAnimation);
      float inputX = panelX + 10.0F;
      float inputY = panelY + 28.0F;
      float inputW = panelWidth - 20.0F;
      float inputH = 18.0F;
      boolean wasInsideInput = this.isInside(mouseX, mouseY, inputX, inputY, inputW, inputH);
      if (wasInsideInput && button == 0) {
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastClickTime < 250L && this.inputFocused) {
            this.selectAllText();
         } else {
            this.cursorPosition = this.getCursorIndexAt(mouseX, inputX);
            this.inputFocused = true;
            this.selectionStart = this.cursorPosition;
            this.selectionEnd = this.cursorPosition;
         }

         this.lastClickTime = currentTime;
         return true;
      } else {
         if (!wasInsideInput && this.inputFocused) {
            this.inputFocused = false;
            this.clearSelection();
         }

         float btnW = 65.0F;
         float btnH = 14.0F;
         float btnY = panelY + panelHeight - btnH - 10.0F;
         float saveBtnX = panelX + panelWidth - btnW - 10.0F;
         if (this.isInside(mouseX, mouseY, saveBtnX, btnY, btnW, btnH)) {
            if (this.text.length() > 0) {
               UserConfigManager.getInstance().saveConfig(this.text);
               this.creatingConfig = false;
               this.text = "";
               this.inputFocused = false;
            }

            return true;
         } else {
            float cancelBtnX = panelX + 10.0F;
            if (this.isInside(mouseX, mouseY, cancelBtnX, btnY, btnW, btnH)) {
               this.creatingConfig = false;
               this.text = "";
               this.inputFocused = false;
               return true;
            } else {
               return this.isInside(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight);
            }
         }
      }
   }

   public void mouseScrolled(double mouseX, double mouseY, double amount) {
      float contentY = this.y + 30.0F;
      float contentH = this.height - 30.0F;
      double scaledX = Render2D.convertX((float)mouseX);
      double scaledY = Render2D.convertY((float)mouseY);
      if (this.isInside(scaledX, scaledY, this.x, contentY, this.width, contentH)) {
         this.targetScroll += (float)(amount * 30.0);
         float maxScroll = Math.min(0.0F, contentH - this.contentHeight);
         if (this.targetScroll < maxScroll) {
            this.targetScroll = maxScroll;
         }

         if (this.targetScroll > 0.0F) {
            this.targetScroll = 0.0F;
         }
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button) {
      if (this.creatingConfig && this.inputFocused && button == 0) {
         double scaledX = Render2D.convertX((float)mouseX);
         float panelX = this.x + 8.0F;
         float inputX = panelX + 12.0F;
         this.cursorPosition = this.getCursorIndexAt(scaledX, inputX);
         this.selectionEnd = this.cursorPosition;
         return true;
      } else {
         return false;
      }
   }

   public boolean keyPressed(KeyInput input) {
      if (!this.creatingConfig || !this.inputFocused) {
         return false;
      }

      if (this.isControlDown()) {
         switch (input.key()) {
            case 65:
               this.selectAllText();
               return true;
            case 67:
               this.copyToClipboard();
               return true;
            case 86:
               this.pasteFromClipboard();
               return true;
            case 88:
               if (this.hasSelection()) {
                  this.copyToClipboard();
                  this.deleteSelectedText();
               }

               return true;
            default:
               return false;
         }
      } else {
         switch (input.key()) {
            case 256:
               this.creatingConfig = false;
               this.text = "";
               this.inputFocused = false;
               return true;
            case 257:
               if (this.text.length() > 0) {
                  UserConfigManager.getInstance().saveConfig(this.text);
                  this.creatingConfig = false;
                  this.text = "";
                  this.inputFocused = false;
               }

               return true;
            case 258:
            case 260:
            case 264:
            case 265:
            case 266:
            case 267:
            default:
               return false;
            case 259:
               this.handleBackspace();
               return true;
            case 261:
               this.handleDelete();
               return true;
            case 262:
               this.moveCursor(1);
               return true;
            case 263:
               this.moveCursor(-1);
               return true;
            case 268:
               this.cursorPosition = 0;
               this.updateSelectionAfterCursorMove();
               return true;
            case 269:
               this.cursorPosition = this.text.length();
               this.updateSelectionAfterCursorMove();
               return true;
         }
      }
   }

   public boolean charTyped(CharInput input) {
      if (this.creatingConfig && this.inputFocused) {
         char chr = (char)input.codepoint();
         if (Character.isISOControl(chr)) {
            return false;
         }

         boolean isEnglishLetter = chr >= 'a' && chr <= 'z' || chr >= 'A' && chr <= 'Z';
         boolean isDigit = chr >= '0' && chr <= '9';
         boolean isAllowed = chr == '_' || chr == '-' || chr == ' ';
         if (!isEnglishLetter && !isDigit && !isAllowed) {
            return false;
         }

         long currentTime = System.currentTimeMillis();
         if (chr == this.lastChar && currentTime - this.lastInputTime < 10L) {
            return false;
         }

         this.lastChar = chr;
         this.lastInputTime = currentTime;
         if (this.text.length() < 32 || this.hasSelection()) {
            this.deleteSelectedText();
            this.text = this.text.substring(0, this.cursorPosition) + chr + this.text.substring(this.cursorPosition);
            this.cursorPosition++;
            this.clearSelection();
            return true;
         }
      }

      return false;
   }

   private boolean isControlDown() {
      long window = mc.getWindow().getHandle();
      return GLFW.glfwGetKey(window, 341) == 1 || GLFW.glfwGetKey(window, 345) == 1;
   }

   private boolean isShiftDown() {
      long window = mc.getWindow().getHandle();
      return GLFW.glfwGetKey(window, 340) == 1 || GLFW.glfwGetKey(window, 344) == 1;
   }

   private void handleBackspace() {
      if (this.hasSelection()) {
         this.deleteSelectedText();
      } else if (this.cursorPosition > 0) {
         this.text = this.text.substring(0, this.cursorPosition - 1) + this.text.substring(this.cursorPosition);
         this.cursorPosition--;
      }
   }

   private void handleDelete() {
      if (this.hasSelection()) {
         this.deleteSelectedText();
      } else if (this.cursorPosition < this.text.length()) {
         this.text = this.text.substring(0, this.cursorPosition) + this.text.substring(this.cursorPosition + 1);
      }
   }

   private void moveCursor(int direction) {
      if (this.hasSelection() && !this.isShiftDown()) {
         this.cursorPosition = direction < 0 ? this.getStartOfSelection() : this.getEndOfSelection();
         this.clearSelection();
      } else {
         if (direction < 0 && this.cursorPosition > 0) {
            this.cursorPosition--;
         } else if (direction > 0 && this.cursorPosition < this.text.length()) {
            this.cursorPosition++;
         }

         this.updateSelectionAfterCursorMove();
      }
   }

   private void updateSelectionAfterCursorMove() {
      if (this.isShiftDown()) {
         if (this.selectionStart == -1) {
            this.selectionStart = this.selectionEnd != -1 ? this.selectionEnd : this.cursorPosition;
         }

         this.selectionEnd = this.cursorPosition;
      } else {
         this.clearSelection();
      }
   }

   private void pasteFromClipboard() {
      String clipboardText = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
      if (clipboardText != null && !clipboardText.isEmpty()) {
         clipboardText = clipboardText.replaceAll("[\n\r\t]", "");
         if (this.hasSelection()) {
            this.deleteSelectedText();
         }

         int remainingSpace = 32 - this.text.length();
         if (clipboardText.length() > remainingSpace) {
            clipboardText = clipboardText.substring(0, remainingSpace);
         }

         if (!clipboardText.isEmpty()) {
            this.text = this.text.substring(0, this.cursorPosition) + clipboardText + this.text.substring(this.cursorPosition);
            this.cursorPosition = this.cursorPosition + clipboardText.length();
         }
      }
   }

   private void copyToClipboard() {
      if (this.hasSelection()) {
         GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), this.getSelectedText());
      }
   }

   private void selectAllText() {
      this.selectionStart = 0;
      this.selectionEnd = this.text.length();
      this.cursorPosition = this.text.length();
   }

   private void deleteSelectedText() {
      if (this.hasSelection()) {
         int start = this.getStartOfSelection();
         int end = this.getEndOfSelection();
         this.text = this.text.substring(0, start) + this.text.substring(end);
         this.cursorPosition = start;
         this.clearSelection();
      }
   }

   private boolean hasSelection() {
      return this.selectionStart != -1 && this.selectionEnd != -1 && this.selectionStart != this.selectionEnd;
   }

   private String getSelectedText() {
      return !this.hasSelection() ? "" : this.text.substring(this.getStartOfSelection(), this.getEndOfSelection());
   }

   private int getStartOfSelection() {
      return Math.min(this.selectionStart, this.selectionEnd);
   }

   private int getEndOfSelection() {
      return Math.max(this.selectionStart, this.selectionEnd);
   }

   private void clearSelection() {
      this.selectionStart = -1;
      this.selectionEnd = -1;
   }

   private int getCursorIndexAt(double mouseX, float inputX) {
      float textAreaX = inputX + 8.0F;
      float relativeX = (float)(mouseX - textAreaX + this.textScrollOffset);
      if (relativeX <= 0.0F) {
         return 0;
      }

      int position = 0;
      float lastWidth = 0.0F;

      while (position < this.text.length() && MsdfManager.BOLD != null) {
         float currentWidth = MsdfManager.BOLD.getStringWidth(this.text.substring(0, position + 1), 7.5F);
         float midPoint = (lastWidth + currentWidth) / 2.0F;
         if (relativeX < midPoint) {
            return position;
         }

         lastWidth = currentWidth;
         position++;
      }

      return this.text.length();
   }

   private boolean isInside(double mx, double my, float x, float y, float w, float h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }

   public boolean isCreatingConfig() {
      return this.creatingConfig;
   }
}
