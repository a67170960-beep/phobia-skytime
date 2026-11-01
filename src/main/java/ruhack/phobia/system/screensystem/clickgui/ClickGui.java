package ruhack.phobia.system.screensystem.clickgui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.configsystem.ConfigSystem;
import ruhack.phobia.system.configsystem.impl.userconfig.UserConfig;
import ruhack.phobia.system.configsystem.impl.userconfig.UserConfigManager;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.impl.other.Theme;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.util.render.scissor.Scissor;

public final class ClickGui extends Screen {
   public static final ClickGui INSTANCE = new ClickGui();
   private static final Identifier SEARCH_ICON = Identifier.of("phobia", "images/search.png");
   private final SidebarComponent sidebar = new SidebarComponent();
   private final SearchComponent search = new SearchComponent();
   private final DescriptionPanelComponent descriptionPanel = new DescriptionPanelComponent();
   private final ConfigPopupComponent configPopup = new ConfigPopupComponent();
   private final IrcComponent irc = new IrcComponent();
   private final Map<ClickGuiCategory, List<ModuleComponent>> components = new EnumMap<>(ClickGuiCategory.class);
   private final Map<ClickGuiCategory, Float> scrolls = new EnumMap<>(ClickGuiCategory.class);
   private final Map<ClickGuiCategory, Float> targetScrolls = new EnumMap<>(ClickGuiCategory.class);
   private final List<ClickGui.ThemeCardBounds> themeCards = new ArrayList<>();
   private ClickGuiCategory selectedCategory = ClickGuiCategory.COMBAT;
   private ModuleComponent selectedModule;
   private ModuleComponent bindingModule;
   private ModuleComponent hoveredComponent;
   private ModuleStructure hoveredModule;
   private boolean configPopupOpen;
   private boolean settingsPanelOpen;
   private ClickGuiLayout layout;
   private float panelX;
   private float panelY;
   private float panelWidth;
   private float panelHeight;
   private float contentX;
   private float contentY;
   private float contentWidth;
   private float contentHeight;
   private float moduleAreaWidth;
   private float settingsX;
   private float settingsY;
   private float settingsRenderX;
   private float settingsRenderY;
   private float settingsPanelWidth;
   private float settingsPanelHeight;
   private float settingsHeaderHeight;
   private float settingsListY;
   private float settingsListHeight;
   private float settingsScroll;
   private float settingsTargetScroll;
   private float settingsContentHeight;
   private float moduleContentHeight;
   private float guiAlpha;
   private float contentAlpha;
   private float settingsAnimation;
   private long lastFrame = System.nanoTime();
   private boolean closing;
   private boolean layoutInitialized;

   private ClickGui() {
      super(Text.literal("Phobia ClickGUI"));
   }

   public void openGui() {
      this.closing = false;
      this.configPopupOpen = false;
      this.settingsPanelOpen = false;
      if (this.selectedModule != null) {
         this.selectedModule.setSelected(false);
         this.selectedModule.closeAllDropdowns();
      }

      this.selectedModule = null;
      if (this.bindingModule != null) {
         this.bindingModule.setBinding(false);
      }

      this.bindingModule = null;
      this.irc.resetFocus();
      this.guiAlpha = 0.0F;
      this.contentAlpha = 0.0F;
      this.settingsAnimation = 0.0F;
      this.layoutInitialized = false;
      this.lastFrame = System.nanoTime();
      MinecraftClient.getInstance().setScreen(this);
   }

   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      if (this.closing) {
         Render2D.clearOverrideTasks();
         super.close();
      } else {
         ClickGuiTheme.syncAccent();
         if (MsdfManager.BOLD == null || MsdfManager.ICONS == null) {
            Msdf.hasFonts();
         }

         if (MsdfManager.BOLD != null && MsdfManager.ICONS != null) {
            float dt = Math.min((float)(System.nanoTime() - this.lastFrame) / 1.0E9F, 0.1F);
            this.lastFrame = System.nanoTime();
            this.guiAlpha = this.guiAlpha + (1.0F - this.guiAlpha) * Math.min(1.0F, dt * 11.0F);
            this.contentAlpha = this.contentAlpha + (1.0F - this.contentAlpha) * Math.min(1.0F, dt * 9.0F);
            this.settingsPanelOpen = this.selectedModule != null;
            this.settingsAnimation = this.settingsAnimation + ((this.settingsPanelOpen ? 1.0F : 0.0F) - this.settingsAnimation) * Math.min(1.0F, dt * 11.0F);
            this.calculateLayout(dt);
            double scaledMouseX = Render2D.convertX(mouseX);
            double scaledMouseY = Render2D.convertY(mouseY);
            ClickGuiEffects.shadow(
               context,
               this.panelX,
               this.panelY,
               this.panelWidth,
               this.panelHeight,
               this.layout.windowRadius,
               ClickGuiEffects.ShadowLevel.WINDOW,
               this.guiAlpha
            );
            ClickGuiEffects.accentGlow(context, this.panelX, this.panelY, this.panelWidth, this.panelHeight, this.layout.windowRadius, this.guiAlpha * 0.12F);
            ClickGuiEffects.glass(
               context, this.panelX, this.panelY, this.panelWidth, this.panelHeight, this.layout.windowRadius, ClickGuiTheme.WINDOW, 13.0F, null, this.guiAlpha
            );
            Render2D.outline(
               context,
               this.panelX,
               this.panelY,
               this.panelWidth,
               this.panelHeight,
               this.layout.windowRadius,
               ClickGuiLayout.physicalToRender(1.0F),
               ClickGuiTheme.alpha(ClickGuiTheme.BORDER, this.guiAlpha * 0.72F),
               true
            );
            this.sidebar
               .render(
                  context,
                  this.panelX,
                  this.panelY,
                  this.layout.sidebarWidth,
                  this.panelHeight,
                  this.selectedCategory,
                  this.guiAlpha,
                  scaledMouseX,
                  scaledMouseY
               );
            this.renderHeader(context, scaledMouseX, scaledMouseY);
            Scissor.push(this.contentX, this.contentY, this.contentWidth, this.contentHeight, true);
            this.renderModules(context, scaledMouseX, scaledMouseY, dt);
            Scissor.pop(true);
            this.renderSettings(context, scaledMouseX, scaledMouseY);
            this.descriptionPanel
               .render(
                  context,
                  !this.configPopupOpen && this.hoveredComponent != null ? this.hoveredComponent.getBounds() : null,
                  Render2D.getFixedScaledWidth(),
                  Render2D.getFixedScaledHeight(),
                  this.settingsPanelOpen ? this.settingsRenderX : -1.0F,
                  this.settingsRenderY,
                  this.settingsPanelOpen ? this.settingsPanelWidth : 0.0F,
                  this.settingsPanelOpen ? this.settingsPanelHeight : 0.0F,
                  this.guiAlpha
               );
            if (this.selectedModule != null) {
               this.selectedModule.renderDropdowns(context, (int)scaledMouseX, (int)scaledMouseY);
            }

            this.configPopup
               .render(
                  context,
                  this.layout,
                  this.contentX,
                  this.panelY + this.layout.headerHeight - ClickGuiLayout.physicalToRender(5.0F),
                  this.configPopupOpen,
                  scaledMouseX,
                  scaledMouseY,
                  this.guiAlpha,
                  dt
               );
            if (this.configPopup.consumeCloseRequest()) {
               this.configPopupOpen = false;
            }
         } else {
            super.render(context, mouseX, mouseY, deltaTicks);
         }
      }
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
   }

   @Override
   protected void applyBlur(DrawContext context) {
   }

   @Override
   public void renderInGameBackground(DrawContext context) {
   }

   private void calculateLayout(float dt) {
      this.layout = ClickGuiLayout.current();
      float screenWidth = Render2D.getFixedScaledWidth();
      float screenHeight = Render2D.getFixedScaledHeight();
      this.panelWidth = this.layout.windowWidth;
      this.panelHeight = this.layout.windowHeight;
      this.settingsPanelWidth = this.layout.settingsWidth;
      float externalGap = ClickGuiLayout.physicalToRender(12.0F);
      float margin = ClickGuiLayout.physicalToRender(12.0F);
      float centeredX = (screenWidth - this.panelWidth) / 2.0F;
      float targetPanelX = centeredX;
      if (this.settingsPanelOpen) {
         float totalWidth = this.panelWidth + externalGap + this.settingsPanelWidth;
         if (totalWidth <= screenWidth - margin * 2.0F) {
            targetPanelX = clamp((screenWidth - totalWidth) / 2.0F, margin, screenWidth - totalWidth - margin);
         }
      }

      if (!this.layoutInitialized) {
         this.panelX = targetPanelX;
         this.layoutInitialized = true;
      } else {
         this.panelX = this.panelX + (targetPanelX - this.panelX) * Math.min(1.0F, dt * 10.0F);
      }

      this.panelY = (screenHeight - this.panelHeight) / 2.0F;
      this.contentX = this.panelX + this.layout.sidebarWidth + this.layout.padding;
      this.contentY = this.panelY + this.layout.headerHeight + this.layout.padding;
      this.contentWidth = this.panelWidth - this.layout.sidebarWidth - this.layout.padding * 2.0F;
      this.contentHeight = this.panelHeight - this.layout.headerHeight - this.layout.padding * 2.0F;
      this.moduleAreaWidth = this.contentWidth;
      this.settingsHeaderHeight = ClickGuiLayout.physicalToRender(36.0F);
      this.settingsContentHeight = this.selectedModule == null ? 0.0F : this.selectedModule.getSettingsHeight();
      float desiredSettingsHeight = this.settingsHeaderHeight + this.settingsContentHeight + this.layout.padding;
      this.settingsPanelHeight = clamp(desiredSettingsHeight, ClickGuiLayout.physicalToRender(41.0F), this.panelHeight);
      this.settingsX = this.panelX + this.panelWidth + externalGap;
      if (this.settingsX + this.settingsPanelWidth > screenWidth - margin) {
         this.settingsX = this.panelX - externalGap - this.settingsPanelWidth;
      }

      this.settingsX = clamp(this.settingsX, margin, screenWidth - this.settingsPanelWidth - margin);
      this.settingsY = this.panelY;
      this.settingsRenderX = this.settingsX + (1.0F - this.settingsAnimation) * ClickGuiLayout.physicalToRender(10.0F);
      this.settingsRenderY = this.settingsY + (1.0F - this.settingsAnimation) * ClickGuiLayout.physicalToRender(5.0F);
      this.settingsListY = this.settingsRenderY + this.settingsHeaderHeight;
      this.settingsListHeight = this.settingsPanelHeight - this.settingsHeaderHeight - this.layout.padding;
   }

   public boolean overlapsPanel(float x, float y, float width, float height) {
      return intersects(x, y, width, height, this.panelX, this.panelY, this.panelWidth, this.panelHeight)
         ? true
         : this.settingsPanelOpen
            && this.settingsAnimation > 0.02F
            && intersects(x, y, width, height, this.settingsRenderX, this.settingsRenderY, this.settingsPanelWidth, this.settingsPanelHeight);
   }

   private static boolean intersects(float x, float y, float width, float height, float otherX, float otherY, float otherWidth, float otherHeight) {
      return x < otherX + otherWidth && x + width > otherX && y < otherY + otherHeight && y + height > otherY;
   }

   private void renderHeader(DrawContext context, double mouseX, double mouseY) {
      float headerX = this.panelX + this.layout.sidebarWidth;
      float headerWidth = this.panelWidth - this.layout.sidebarWidth;
      Render2D.blur(context, headerX, this.panelY, headerWidth, this.layout.headerHeight, 0.0F, this.layout.windowRadius, 0.0F, 0.0F, 9.0F, true);
      Render2D.rect(
         context,
         headerX,
         this.panelY,
         headerWidth,
         this.layout.headerHeight,
         0.0F,
         this.layout.windowRadius,
         0.0F,
         0.0F,
         true,
         ClickGuiTheme.alpha(ClickGuiTheme.HEADER, this.guiAlpha)
      );
      Render2D.rect(
         context,
         headerX,
         this.panelY + this.layout.headerHeight - ClickGuiLayout.physicalToRender(1.0F),
         headerWidth,
         ClickGuiLayout.physicalToRender(1.0F),
         0.0F,
         ClickGuiTheme.alpha(ClickGuiTheme.BORDER, this.guiAlpha),
         true
      );
      String configName = activeConfigName();
      float configY = this.panelY + ClickGuiLayout.physicalToRender(9.0F);
      float configWidth = ClickGuiLayout.physicalToRender(142.0F);
      float configHeight = ClickGuiLayout.physicalToRender(27.0F);
      ClickGuiEffects.glass(
         context,
         this.contentX,
         configY,
         configWidth,
         configHeight,
         this.layout.cardRadius,
         ClickGuiTheme.GLASS_INSET,
         6.0F,
         ClickGuiEffects.ShadowLevel.CARD,
         this.guiAlpha,
         false
      );
      Msdf.text(
         context,
         MsdfManager.ICONS,
         "\ue006",
         this.contentX + ClickGuiLayout.physicalToRender(9.0F),
         configY + ClickGuiLayout.physicalToRender(10.0F),
         ClickGuiLayout.physicalToRender(16.0F),
         ClickGuiTheme.accentAt(this.contentX, Math.round(this.guiAlpha * 255.0F)),
         true
      );
      float configTextSize = ClickGuiLayout.physicalToRender(11.0F);
      float configTextY = configY + (configHeight - Msdf.height(MsdfManager.BOLD, configTextSize)) / 2.0F;
      Msdf.text(
         context,
         MsdfManager.BOLD,
         configName,
         this.contentX + ClickGuiLayout.physicalToRender(27.0F),
         configTextY,
         configTextSize,
         ClickGuiTheme.alpha(ClickGuiTheme.TEXT, this.guiAlpha),
         true
      );
      float arrowSize = ClickGuiLayout.physicalToRender(8.0F);
      float arrowY = configY + (configHeight - Msdf.height(MsdfManager.ICONS, arrowSize)) / 2.0F;
      Msdf.text(
         context,
         MsdfManager.ICONS,
         "p",
         this.contentX + configWidth - ClickGuiLayout.physicalToRender(15.0F),
         arrowY,
         arrowSize,
         ClickGuiTheme.alpha(ClickGuiTheme.TEXT_MUTED, this.guiAlpha),
         -45.0F + 90.0F * this.configPopup.getAnimation(),
         true
      );
      float searchWidth = this.search.isActive() ? ClickGuiLayout.physicalToRender(160.0F) : ClickGuiLayout.physicalToRender(28.0F);
      float searchX = this.panelX + this.panelWidth - this.layout.padding - searchWidth;
      boolean hovered = inside(mouseX, mouseY, searchX, configY, searchWidth, configHeight);
      ClickGuiEffects.glass(
         context,
         searchX,
         configY,
         searchWidth,
         configHeight,
         this.layout.cardRadius,
         hovered ? ClickGuiTheme.SURFACE_HOVER : ClickGuiTheme.GLASS_INSET,
         6.0F,
         ClickGuiEffects.ShadowLevel.CARD,
         this.guiAlpha
      );
      if (this.search.isActive()) {
         String value = this.search.getQuery().isEmpty() ? "Поиск модулей..." : this.search.getQuery();
         int color = this.search.getQuery().isEmpty() ? ClickGuiTheme.TEXT_FAINT : ClickGuiTheme.TEXT;
         Msdf.text(
            context,
            MsdfManager.BOLD,
            value,
            searchX + ClickGuiLayout.physicalToRender(9.0F),
            configY + ClickGuiLayout.physicalToRender(8.0F),
            ClickGuiLayout.physicalToRender(10.0F),
            ClickGuiTheme.alpha(color, this.guiAlpha),
            true
         );
         if (System.currentTimeMillis() / 500L % 2L == 0L) {
            float caretX = searchX
               + ClickGuiLayout.physicalToRender(9.0F)
               + Msdf.width(MsdfManager.BOLD, this.search.getQuery(), ClickGuiLayout.physicalToRender(10.0F));
            Render2D.rect(
               context,
               caretX,
               configY + ClickGuiLayout.physicalToRender(7.0F),
               ClickGuiLayout.physicalToRender(1.0F),
               ClickGuiLayout.physicalToRender(12.0F),
               0.0F,
               ClickGuiTheme.accentAt(caretX, Math.round(this.guiAlpha * 255.0F)),
               true
            );
         }
      } else {
         float iconSize = ClickGuiLayout.physicalToRender(15.0F);
         Render2D.imageMask(
            context,
            searchX + (searchWidth - iconSize) / 2.0F,
            configY + (configHeight - iconSize) / 2.0F,
            iconSize,
            SEARCH_ICON,
            ClickGuiTheme.accentAt(searchX + searchWidth / 2.0F, Math.round(this.guiAlpha * 255.0F)),
            0.0F,
            true
         );
      }
   }

   private void renderModules(DrawContext context, double mouseX, double mouseY, float dt) {
      if (this.selectedCategory == ClickGuiCategory.IRC && !this.search.hasQuery()) {
         this.hoveredModule = null;
         this.hoveredComponent = null;
         this.moduleContentHeight = this.contentHeight;
         this.irc.render(context, this.contentX, this.contentY, this.contentWidth, this.contentHeight, this.guiAlpha * this.contentAlpha, mouseX, mouseY, dt);
      } else if (this.selectedCategory == ClickGuiCategory.THEME && !this.search.hasQuery()) {
         this.renderThemes(context, mouseX, mouseY);
      } else {
         List<ModuleComponent> visible = this.visibleComponents();
         visible.sort(ModulePresentation.comparator());
         float current = this.scrolls.getOrDefault(this.selectedCategory, 0.0F);
         float target = this.targetScrolls.getOrDefault(this.selectedCategory, 0.0F);
         current += (target - current) * Math.min(1.0F, dt * 12.0F);
         this.scrolls.put(this.selectedCategory, current);
         this.hoveredModule = null;
         this.hoveredComponent = null;
         Map<String, List<ModuleComponent>> grouped = new LinkedHashMap<>();

         for (ModuleComponent component : visible) {
            grouped.computeIfAbsent(this.groupName(component.getModule()), ignored -> new ArrayList<>()).add(component);
         }

         float columnGap = this.layout.gap;
         float columnWidth = (this.moduleAreaWidth - columnGap) / 2.0F;
         float[] columnY = new float[]{this.contentY + current, this.contentY + current};
         float labelHeight = ClickGuiLayout.physicalToRender(18.0F);
         float rowHeight = ClickGuiLayout.physicalToRender(34.0F);
         float containerPadding = ClickGuiLayout.physicalToRender(4.0F);

         for (Entry<String, List<ModuleComponent>> entry : grouped.entrySet()) {
            int column = columnY[0] <= columnY[1] ? 0 : 1;
            float groupX = this.contentX + column * (columnWidth + columnGap);
            float groupY = columnY[column];
            Msdf.text(
               context,
               MsdfManager.BOLD,
               entry.getKey(),
               groupX,
               groupY + ClickGuiLayout.physicalToRender(1.0F),
               ClickGuiLayout.physicalToRender(10.0F),
               ClickGuiTheme.alpha(ClickGuiTheme.TEXT_MUTED, this.guiAlpha * this.contentAlpha),
               true
            );
            float containerY = groupY + labelHeight;
            float containerHeight = containerPadding * 2.0F + entry.getValue().size() * rowHeight;
            float radius = ClickGuiLayout.physicalToRender(9.0F);
            ClickGuiEffects.glass(
               context,
               groupX,
               containerY,
               columnWidth,
               containerHeight,
               radius,
               ClickGuiTheme.SURFACE,
               8.0F,
               ClickGuiEffects.ShadowLevel.CARD,
               this.guiAlpha * this.contentAlpha,
               false
            );

            for (int i = 0; i < entry.getValue().size(); i++) {
               ModuleComponent component = entry.getValue().get(i);
               float rowY = containerY + containerPadding + i * rowHeight;
               if (i > 0) {
                  Render2D.rect(
                     context,
                     groupX + ClickGuiLayout.physicalToRender(8.0F),
                     rowY,
                     columnWidth - ClickGuiLayout.physicalToRender(16.0F),
                     ClickGuiLayout.physicalToRender(1.0F),
                     0.0F,
                     ClickGuiTheme.alpha(ClickGuiTheme.BORDER, this.guiAlpha * this.contentAlpha * 0.72F),
                     true
                  );
               }

               component.setGrouped(true);
               component.setAlpha(this.guiAlpha * this.contentAlpha);
               component.render(context, groupX + containerPadding, rowY, columnWidth - containerPadding * 2.0F, (int)mouseX, (int)mouseY);
               if (component.isHovered()) {
                  this.hoveredModule = component.getModule();
                  this.hoveredComponent = component;
               }
            }

            columnY[column] = containerY + containerHeight + this.layout.cardGap;
         }

         this.moduleContentHeight = Math.max(columnY[0], columnY[1]) - (this.contentY + current);
         if (visible.isEmpty()) {
            String empty = this.search.hasQuery() ? "Ничего не найдено" : "В этой категории пока нет модулей";
            float fontSize = ClickGuiLayout.physicalToRender(11.0F);
            float textWidth = Msdf.width(MsdfManager.BOLD, empty, fontSize);
            Msdf.text(
               context,
               MsdfManager.BOLD,
               empty,
               this.contentX + (this.moduleAreaWidth - textWidth) / 2.0F,
               this.contentY + this.contentHeight / 2.0F,
               fontSize,
               ClickGuiTheme.alpha(ClickGuiTheme.TEXT_FAINT, this.guiAlpha),
               true
            );
         }
      }
   }

   private String groupName(ModuleStructure module) {
      return this.search.hasQuery() ? module.getCategory().getReadableName() : ModulePresentation.section(module);
   }

   private void renderThemes(DrawContext context, double mouseX, double mouseY) {
      Theme theme = Theme.getInstance();
      this.themeCards.clear();
      this.hoveredModule = null;
      this.hoveredComponent = null;
      float current = this.scrolls.getOrDefault(this.selectedCategory, 0.0F);
      float target = this.targetScrolls.getOrDefault(this.selectedCategory, 0.0F);
      current += (target - current) * 0.16F;
      this.scrolls.put(this.selectedCategory, current);
      Msdf.text(
         context,
         MsdfManager.BOLD,
         "Theme",
         this.contentX,
         this.contentY + current + ClickGuiLayout.physicalToRender(1.0F),
         ClickGuiLayout.physicalToRender(11.0F),
         ClickGuiTheme.alpha(ClickGuiTheme.TEXT_MUTED, this.guiAlpha * this.contentAlpha),
         true
      );
      Msdf.text(
         context,
         MsdfManager.BOLD,
         "Выберите готовую тему или создайте свою",
         this.contentX,
         this.contentY + current + ClickGuiLayout.physicalToRender(19.0F),
         ClickGuiLayout.physicalToRender(10.0F),
         ClickGuiTheme.alpha(ClickGuiTheme.TEXT_FAINT, this.guiAlpha * this.contentAlpha),
         true
      );
      int columns = 3;
      float gap = this.layout.gap;
      float cardWidth = (this.moduleAreaWidth - gap * (columns - 1)) / columns;
      float cardHeight = ClickGuiLayout.physicalToRender(52.0F);
      float startY = this.contentY + current + ClickGuiLayout.physicalToRender(43.0F);
      List<Theme.Palette> palettes = theme.palettes();

      for (int i = 0; i < palettes.size(); i++) {
         Theme.Palette palette = palettes.get(i);
         int column = i % columns;
         int row = i / columns;
         float cardX = this.contentX + column * (cardWidth + gap);
         float cardY = startY + row * (cardHeight + this.layout.cardGap);
         boolean hovered = inside(mouseX, mouseY, cardX, cardY, cardWidth, cardHeight);
         boolean selected = theme.isSelected(palette);
         int background = selected ? ClickGuiTheme.SURFACE_ACTIVE : (hovered ? ClickGuiTheme.SURFACE_HOVER : ClickGuiTheme.SURFACE);
         if (selected) {
            ClickGuiEffects.accentGlow(context, cardX, cardY, cardWidth, cardHeight, this.layout.cardRadius, this.guiAlpha * this.contentAlpha * 0.18F);
         }

         ClickGuiEffects.glass(
            context,
            cardX,
            cardY,
            cardWidth,
            cardHeight,
            this.layout.cardRadius,
            background,
            7.0F,
            ClickGuiEffects.ShadowLevel.CARD,
            this.guiAlpha * this.contentAlpha
         );
         float swatch = ClickGuiLayout.physicalToRender(25.0F);
         float swatchX = cardX + ClickGuiLayout.physicalToRender(11.0F);
         float swatchY = cardY + (cardHeight - swatch) / 2.0F;
         int[] swatchColors = new int[9];

         for (int colorIndex = 0; colorIndex < swatchColors.length; colorIndex++) {
            swatchColors[colorIndex] = ClickGuiTheme.alpha(
               palette.colorAt((float)colorIndex / (swatchColors.length - 1), 255), this.guiAlpha * this.contentAlpha
            );
         }

         Render2D.rect(context, swatchX, swatchY, swatch, swatch, ClickGuiLayout.physicalToRender(6.0F), true, swatchColors);
         if (selected) {
            Render2D.outline(
               context,
               swatchX - ClickGuiLayout.physicalToRender(2.0F),
               swatchY - ClickGuiLayout.physicalToRender(2.0F),
               swatch + ClickGuiLayout.physicalToRender(4.0F),
               swatch + ClickGuiLayout.physicalToRender(4.0F),
               ClickGuiLayout.physicalToRender(7.0F),
               ClickGuiLayout.physicalToRender(1.0F),
               ClickGuiTheme.alpha(palette.colorAt(0.5F, 255), this.guiAlpha * this.contentAlpha),
               true
            );
         }

         Msdf.text(
            context,
            MsdfManager.BOLD,
            palette.name(),
            swatchX + swatch + ClickGuiLayout.physicalToRender(10.0F),
            cardY + ClickGuiLayout.physicalToRender(19.0F),
            ClickGuiLayout.physicalToRender(11.0F),
            ClickGuiTheme.alpha(selected ? ClickGuiTheme.TEXT : ClickGuiTheme.TEXT_MUTED, this.guiAlpha * this.contentAlpha),
            true
         );
         this.themeCards.add(new ClickGui.ThemeCardBounds(palette, cardX, cardY, cardWidth, cardHeight));
      }

      int rows = (palettes.size() + columns - 1) / columns;
      this.moduleContentHeight = ClickGuiLayout.physicalToRender(43.0F) + rows * cardHeight + Math.max(0, rows - 1) * this.layout.cardGap;
      this.targetScrolls
         .put(
            this.selectedCategory,
            clamp(this.targetScrolls.getOrDefault(this.selectedCategory, 0.0F), Math.min(0.0F, this.contentHeight - this.moduleContentHeight), 0.0F)
         );
   }

   private void renderSettings(DrawContext context, double mouseX, double mouseY) {
      if (this.selectedModule != null) {
         float panelAlpha = this.guiAlpha * this.settingsAnimation;
         ClickGuiEffects.accentGlow(
            context, this.settingsRenderX, this.settingsRenderY, this.settingsPanelWidth, this.settingsPanelHeight, this.layout.popupRadius, panelAlpha * 0.1F
         );
         ClickGuiEffects.glass(
            context,
            this.settingsRenderX,
            this.settingsRenderY,
            this.settingsPanelWidth,
            this.settingsPanelHeight,
            this.layout.popupRadius,
            ClickGuiTheme.FLOATING,
            11.0F,
            ClickGuiEffects.ShadowLevel.FLOATING,
            panelAlpha
         );
         Render2D.outline(
            context,
            this.settingsRenderX,
            this.settingsRenderY,
            this.settingsPanelWidth,
            this.settingsPanelHeight,
            this.layout.popupRadius,
            ClickGuiLayout.physicalToRender(1.0F),
            ClickGuiTheme.alpha(ClickGuiTheme.BORDER, panelAlpha * 0.65F),
            true
         );
         this.settingsScroll = this.settingsScroll + (this.settingsTargetScroll - this.settingsScroll) * 0.13F;
         float startY = this.settingsRenderY + this.layout.padding;
         Msdf.text(
            context,
            MsdfManager.BOLD,
            this.selectedModule.getModule().getName(),
            this.settingsRenderX + this.layout.padding,
            startY,
            ClickGuiLayout.physicalToRender(15.0F),
            ClickGuiTheme.alpha(ClickGuiTheme.TEXT, panelAlpha),
            true
         );
         Msdf.text(
            context,
            MsdfManager.BOLD,
            "x",
            this.settingsRenderX + this.settingsPanelWidth - this.layout.padding - ClickGuiLayout.physicalToRender(8.0F),
            startY,
            ClickGuiLayout.physicalToRender(12.0F),
            ClickGuiTheme.alpha(ClickGuiTheme.TEXT_MUTED, panelAlpha),
            true
         );
         Scissor.push(
            this.settingsRenderX + this.layout.padding, this.settingsListY, this.settingsPanelWidth - this.layout.padding * 2.0F, this.settingsListHeight, true
         );
         this.selectedModule
            .renderSettings(
               context,
               this.settingsRenderX + this.layout.padding,
               this.settingsListY + this.settingsScroll,
               this.settingsPanelWidth - this.layout.padding * 2.0F,
               (int)mouseX,
               (int)mouseY,
               panelAlpha
            );
         this.settingsTargetScroll = clamp(this.settingsTargetScroll, Math.min(0.0F, this.settingsListHeight - this.settingsContentHeight), 0.0F);
         Scissor.pop(true);
      }
   }

   private List<ModuleComponent> visibleComponents() {
      String query = this.search.getQuery().trim().toLowerCase(Locale.ROOT);
      List<ModuleComponent> result = new ArrayList<>();
      if (this.search.hasQuery()) {
         for (ClickGuiCategory category : ClickGuiCategory.values()) {
            for (ModuleComponent component : this.componentsFor(category)) {
               ModuleStructure module = component.getModule();
               String description = ModulePresentation.description(module);
               if (module.getName().toLowerCase(Locale.ROOT).contains(query) || description.toLowerCase(Locale.ROOT).contains(query)) {
                  result.add(component);
               }
            }
         }
      } else {
         result.addAll(this.componentsFor(this.selectedCategory));
      }

      return result;
   }

   private List<ModuleComponent> componentsFor(ClickGuiCategory category) {
      return this.components.computeIfAbsent(category, key -> {
         List<ModuleComponent> list = new ArrayList<>();

         for (ModuleStructure module : Initialization.getInstance().getManager().getModuleRepository().allModules()) {
            if (key.contains(module.getCategory())) {
               list.add(new ModuleComponent(module));
            }
         }

         return list;
      });
   }

   public boolean mouseClicked(Click click, boolean doubled) {
      this.ensureLayout();
      double mouseX = Render2D.convertX((float)click.x());
      double mouseY = Render2D.convertY((float)click.y());
      if (this.bindingModule != null && this.bindingModule.isBinding() && this.bindingModule.handleBindMouse(click.button())) {
         this.bindingModule = null;
         ConfigSystem.getInstance().save();
         return true;
      }

      float configY = this.panelY + ClickGuiLayout.physicalToRender(9.0F);
      float configWidth = ClickGuiLayout.physicalToRender(142.0F);
      float configHeight = ClickGuiLayout.physicalToRender(27.0F);
      if (this.configPopupOpen) {
         if (inside(mouseX, mouseY, this.contentX, configY, configWidth, configHeight)) {
            this.configPopupOpen = false;
            this.configPopup.resetInput();
            return true;
         } else if (this.configPopup.contains(mouseX, mouseY)) {
            this.configPopup.mouseClicked(mouseX, mouseY, click.button(), this.layout);
            return true;
         } else {
            this.configPopupOpen = false;
            this.configPopup.resetInput();
            return true;
         }
      } else {
         if (this.selectedModule != null && this.selectedModule.dropdownConsumesClick(mouseX, mouseY)) {
            this.selectedModule.mouseClickedSettings(mouseX, mouseY, click.button());
            return true;
         }

         if (this.selectedModule != null
            && inside(
               mouseX,
               mouseY,
               this.settingsRenderX + this.settingsPanelWidth - ClickGuiLayout.physicalToRender(28.0F),
               this.settingsRenderY,
               ClickGuiLayout.physicalToRender(28.0F),
               ClickGuiLayout.physicalToRender(34.0F)
            )) {
            this.selectedModule.setSelected(false);
            this.selectedModule.closeAllDropdowns();
            this.selectedModule = null;
            this.settingsPanelOpen = false;
            return true;
         }

         if (this.selectedModule != null
            && inside(mouseX, mouseY, this.settingsRenderX, this.settingsRenderY, this.settingsPanelWidth, this.settingsPanelHeight)) {
            this.selectedModule.mouseClickedSettings(mouseX, mouseY, click.button());
            return true;
         }

         if (this.selectedCategory == ClickGuiCategory.IRC && !this.search.hasQuery() && this.irc.mouseClicked(mouseX, mouseY, click.button())) {
            return true;
         }

         if (this.selectedCategory == ClickGuiCategory.THEME && !this.search.hasQuery() && click.button() == 0) {
            Theme theme = Theme.getInstance();

            for (ClickGui.ThemeCardBounds card : this.themeCards) {
               if (inside(mouseX, mouseY, this.contentX, this.contentY, this.contentWidth, this.contentHeight)
                  && inside(mouseX, mouseY, card.x(), card.y(), card.width(), card.height())) {
                  theme.select(card.palette());
                  ClickGuiTheme.syncAccent();
                  if (theme.isCustom(card.palette())) {
                     this.openThemeSettings(theme);
                  } else if (this.selectedModule != null && this.selectedModule.getModule() == theme) {
                     this.selectedModule.setSelected(false);
                     this.selectedModule.closeAllDropdowns();
                     this.selectedModule = null;
                     this.settingsPanelOpen = false;
                  }

                  return true;
               }
            }
         }

         for (ModuleComponent component : this.visibleComponents()) {
            ModuleComponent.Interaction interaction = component.mouseClicked(mouseX, mouseY, click.button());
            if (interaction == ModuleComponent.Interaction.BIND) {
               if (this.bindingModule != null && this.bindingModule != component) {
                  this.bindingModule.setBinding(false);
               }

               this.bindingModule = component;
               this.bindingModule.setBinding(true);
               return true;
            }

            if (interaction == ModuleComponent.Interaction.TOGGLE) {
               return true;
            }

            if (interaction == ModuleComponent.Interaction.SETTINGS) {
               if (this.selectedModule == component) {
                  this.selectedModule.setSelected(false);
                  this.selectedModule.closeAllDropdowns();
                  this.selectedModule = null;
                  this.settingsPanelOpen = false;
                  return true;
               }

               if (this.selectedModule != null) {
                  this.selectedModule.setSelected(false);
                  this.selectedModule.closeAllDropdowns();
               }

               this.selectedModule = component;
               this.configPopupOpen = false;
               this.settingsPanelOpen = true;
               if (this.search.hasQuery()) {
                  this.selectedCategory = ClickGuiCategory.fromModuleCategory(component.getModule().getCategory());
               }

               this.selectedModule.setSelected(true);
               this.settingsScroll = 0.0F;
               this.settingsTargetScroll = 0.0F;
               return true;
            }
         }

         ClickGuiCategory category = this.sidebar.categoryAt(mouseX, mouseY);
         if (category != null) {
            this.selectCategory(category);
            return true;
         }

         float searchWidth = this.search.isActive() ? ClickGuiLayout.physicalToRender(160.0F) : ClickGuiLayout.physicalToRender(28.0F);
         float searchX = this.panelX + this.panelWidth - this.layout.padding - searchWidth;
         if (inside(mouseX, mouseY, searchX, configY, searchWidth, configHeight)) {
            if (this.selectedCategory == ClickGuiCategory.IRC) {
               return true;
            }

            this.search.open();
            return true;
         } else if (inside(mouseX, mouseY, this.contentX, configY, configWidth, configHeight)) {
            this.openConfigs();
            return true;
         } else {
            return super.mouseClicked(click, doubled);
         }
      }
   }

   public boolean mouseReleased(Click click) {
      if (this.selectedModule != null) {
         this.selectedModule.mouseReleased(Render2D.convertX((float)click.x()), Render2D.convertY((float)click.y()), click.button());
      }

      return super.mouseReleased(click);
   }

   public boolean mouseDragged(Click click, double offsetX, double offsetY) {
      if (this.selectedModule != null) {
         this.selectedModule.mouseDragged(Render2D.convertX((float)click.x()), Render2D.convertY((float)click.y()), click.button(), offsetX, offsetY);
      }

      return super.mouseDragged(click, offsetX, offsetY);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.ensureLayout();
      double x = Render2D.convertX((float)mouseX);
      double y = Render2D.convertY((float)mouseY);
      if (this.configPopupOpen && this.configPopup.contains(x, y)) {
         this.configPopup.mouseScrolled(verticalAmount, this.layout);
         return true;
      } else if (this.selectedModule != null && this.selectedModule.dropdownScrolled(x, y, verticalAmount)) {
         return true;
      } else if (this.sidebar.scroll(verticalAmount, x, y)) {
         return true;
      } else if (this.selectedCategory == ClickGuiCategory.IRC && !this.search.hasQuery() && this.irc.mouseScrolled(x, y, verticalAmount)) {
         return true;
      } else if (this.selectedModule != null && inside(x, y, this.settingsRenderX, this.settingsRenderY, this.settingsPanelWidth, this.settingsPanelHeight)) {
         this.settingsTargetScroll = this.settingsTargetScroll + (float)verticalAmount * ClickGuiLayout.physicalToRender(27.0F);
         this.settingsTargetScroll = clamp(this.settingsTargetScroll, Math.min(0.0F, this.settingsListHeight - this.settingsContentHeight), 0.0F);
         return true;
      } else if (inside(x, y, this.contentX, this.contentY, this.moduleAreaWidth, this.contentHeight)) {
         float target = this.targetScrolls.getOrDefault(this.selectedCategory, 0.0F) + (float)verticalAmount * ClickGuiLayout.physicalToRender(27.0F);
         this.targetScrolls.put(this.selectedCategory, clamp(target, Math.min(0.0F, this.contentHeight - this.moduleContentHeight), 0.0F));
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
   }

   public boolean keyPressed(KeyInput input) {
      if (this.selectedCategory == ClickGuiCategory.IRC && this.irc.keyPressed(input)) {
         return true;
      }

      if (this.bindingModule != null && this.bindingModule.isBinding() && this.bindingModule.handleBindKey(input)) {
         this.bindingModule = null;
         ConfigSystem.getInstance().save();
         return true;
      }

      if (this.configPopupOpen) {
         this.configPopup.keyPressed(input);
         if (this.configPopup.consumeCloseRequest()) {
            this.configPopupOpen = false;
            this.configPopup.resetInput();
         }

         return true;
      } else if (this.search.isActive()) {
         this.search.keyPressed(input);
         return true;
      } else if (this.selectedModule != null && this.selectedModule.keyPressed(input)) {
         return true;
      } else if (input.key() == 256 && this.selectedModule != null) {
         this.selectedModule.setSelected(false);
         this.selectedModule.closeAllDropdowns();
         this.selectedModule = null;
         this.settingsPanelOpen = false;
         return true;
      } else if (input.key() == 256) {
         this.closing = true;
         return true;
      } else if (input.key() == 70 && (input.modifiers() & 2) != 0) {
         this.search.open();
         return true;
      } else {
         return super.keyPressed(input);
      }
   }

   public boolean charTyped(CharInput input) {
      if (this.selectedCategory == ClickGuiCategory.IRC && this.irc.charTyped(input)) {
         return true;
      } else if (this.configPopupOpen) {
         return this.configPopup.charTyped(input);
      } else if (this.search.isActive()) {
         return this.search.charTyped(input);
      } else {
         return this.selectedModule != null ? this.selectedModule.charTyped(input) : super.charTyped(input);
      }
   }

   private void selectCategory(ClickGuiCategory category) {
      if (category != this.selectedCategory || this.search.isActive() || this.configPopupOpen) {
         this.search.close();
         this.configPopupOpen = false;
         this.configPopup.resetInput();
         this.irc.resetFocus();
         this.selectedCategory = category;
         if (this.bindingModule != null) {
            this.bindingModule.setBinding(false);
            this.bindingModule = null;
         }

         this.contentAlpha = 0.0F;
         this.settingsScroll = 0.0F;
         this.settingsTargetScroll = 0.0F;
         if (this.selectedModule != null) {
            this.selectedModule.setSelected(false);
            this.selectedModule.closeAllDropdowns();
            this.selectedModule = null;
            this.settingsPanelOpen = false;
         }
      }
   }

   private void openThemeSettings(Theme theme) {
      ModuleComponent component = this.componentsFor(ClickGuiCategory.THEME)
         .stream()
         .filter(candidate -> candidate.getModule() == theme)
         .findFirst()
         .orElse(null);
      if (component != null) {
         if (this.selectedModule != null && this.selectedModule != component) {
            this.selectedModule.setSelected(false);
            this.selectedModule.closeAllDropdowns();
         }

         this.selectedModule = component;
         this.selectedModule.setSelected(true);
         this.configPopupOpen = false;
         this.settingsPanelOpen = true;
         this.settingsScroll = 0.0F;
         this.settingsTargetScroll = 0.0F;
      }
   }

   private void openConfigs() {
      this.search.close();
      this.configPopupOpen = !this.configPopupOpen;
      if (!this.configPopupOpen) {
         this.configPopup.resetInput();
      }
   }

   private static String activeConfigName() {
      UserConfigManager manager = UserConfigManager.getInstance();
      UserConfig active = manager.getActiveConfig();
      return active == null ? "Default" : active.getName();
   }

   public void close() {
      this.closing = true;
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean isClosing() {
      return this.closing;
   }

   private static boolean inside(double mx, double my, float x, float y, float width, float height) {
      return mx >= x && mx <= x + width && my >= y && my <= y + height;
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private void ensureLayout() {
      if (this.layout == null) {
         this.settingsPanelOpen = this.selectedModule != null;
         this.calculateLayout(0.0F);
      }
   }

   private static List<String> wrapText(String text, float maxWidth, float fontSize) {
      List<String> lines = new ArrayList<>();
      if (text != null && !text.isBlank() && MsdfManager.BOLD != null) {
         StringBuilder current = new StringBuilder();

         for (String word : text.trim().split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && Msdf.width(MsdfManager.BOLD, candidate, fontSize) > maxWidth) {
               lines.add(current.toString());
               current = new StringBuilder(word);
            } else {
               current = new StringBuilder(candidate);
            }
         }

         if (!current.isEmpty()) {
            lines.add(current.toString());
         }

         return lines;
      } else {
         return lines;
      }
   }

   private record ThemeCardBounds(Theme.Palette palette, float x, float y, float width, float height) {
   }
}
