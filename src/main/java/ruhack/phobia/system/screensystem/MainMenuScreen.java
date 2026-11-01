package ruhack.phobia.system.screensystem;

import java.lang.reflect.Proxy;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.system.rendersystem.render2d.menu.MainMenuBackground;
import ruhack.phobia.system.rendersystem.render2d.menu.MainMenuShader;
import ruhack.phobia.util.colorschem.ColorUtil;

public final class MainMenuScreen extends Screen {
   private static final int TEXT = ColorUtil.rgba(225, 225, 228, 255);
   private static final int MUTED = ColorUtil.rgba(174, 174, 180, 255);
   private static final int FAINT = ColorUtil.rgba(112, 112, 119, 255);
   private static final int DANGER = ColorUtil.rgba(239, 64, 78, 255);
   private static final String UPDATE_ICON = "M";
   private final MainMenuScreen.MenuButton singleplayer = new MainMenuScreen.MenuButton(MainMenuScreen.Action.SINGLEPLAYER, "Singleplayer", "\ue012");
   private final MainMenuScreen.MenuButton multiplayer = new MainMenuScreen.MenuButton(MainMenuScreen.Action.MULTIPLAYER, "Multiplayer", "3");
   private final MainMenuScreen.MenuButton settings = new MainMenuScreen.MenuButton(MainMenuScreen.Action.OPTIONS, "Settings", "U");
   private final MainMenuScreen.MenuButton accounts = new MainMenuScreen.MenuButton(MainMenuScreen.Action.ACCOUNTS, "Accounts", "\ue012");
   private final MainMenuScreen.MenuButton exit = new MainMenuScreen.MenuButton(MainMenuScreen.Action.EXIT, "", "F");
   private final MainMenuScreen.MenuButton[] buttons = new MainMenuScreen.MenuButton[]{
      this.singleplayer, this.multiplayer, this.settings, this.accounts, this.exit
   };
   private long lastFrame = System.nanoTime();
   private float intro;
   private float updateHover;
   private float updateX;
   private float updateY;
   private float updateWidth;
   private float updateHeight;
   private static Object fancyMenuBlacklistRule;

   public MainMenuScreen() {
      super(Text.literal("Phobia"));
      protectFromFancyMenu(this);
   }

   public static synchronized void protectFromFancyMenu(Screen screen) {
      try {
         Class<?> customization = Class.forName("de.keksuccino.fancymenu.customization.ScreenCustomization");
         Class<?> ruleType = Class.forName("de.keksuccino.fancymenu.customization.ScreenCustomization$ScreenBlacklistRule");
         if (fancyMenuBlacklistRule == null) {
            String mainScreen = MainMenuScreen.class.getName();
            String updateScreen = MainMenuUpdateScreen.class.getName();
            fancyMenuBlacklistRule = Proxy.newProxyInstance(
               MainMenuScreen.class.getClassLoader(),
               new Class[]{ruleType},
               (proxy, method, args) -> {
                  String name = method.getName();
                  if ("isScreenBlacklisted".equals(name)) {
                     return args != null && args.length == 1 && (mainScreen.equals(args[0]) || updateScreen.equals(args[0]));
                  } else if ("toString".equals(name)) {
                     return "PhobiaMainMenuBlacklistRule";
                  } else if ("hashCode".equals(name)) {
                     return System.identityHashCode(proxy);
                  } else if ("equals".equals(name)) {
                     return args != null && args.length == 1 && proxy == args[0];
                  }
                  return null;
               }
            );
         }

         if (customization.getMethod("getScreenBlacklistRules").invoke(null) instanceof List<?> list && !list.contains(fancyMenuBlacklistRule)) {
            customization.getMethod("addScreenBlacklistRule", ruleType).invoke(null, fancyMenuBlacklistRule);
         }

         if (screen instanceof MainMenuScreen || screen instanceof MainMenuUpdateScreen) {
            customization.getMethod("setCustomizationForScreenEnabled", Screen.class, boolean.class).invoke(null, screen, false);
         }
      } catch (ClassNotFoundException var5) {
      } catch (ReflectiveOperationException var6) {
      }
   }

   protected void init() {
      protectFromFancyMenu(this);
      this.updateLayout();
      this.intro = 0.0F;
      this.lastFrame = System.nanoTime();
   }

   private void updateLayout() {
      float w = Render2D.getFixedScaledWidth();
      float h = Render2D.getFixedScaledHeight();
      float fullWidth = Math.max(75.0F, Math.min(110.0F, w * 0.2F));
      float buttonHeight = 22.0F;
      float gap = 5.0F;
      float x = (w - fullWidth) * 0.5F;
      float y = Math.max(132.0F, h * 0.4F);
      this.singleplayer.setBounds(x, y, fullWidth, buttonHeight);
      this.multiplayer.setBounds(x, y + buttonHeight + gap, fullWidth, buttonHeight);
      this.settings.setBounds(x, y + (buttonHeight + gap) * 2.0F, fullWidth, buttonHeight);
      float exitWidth = 18.0F;
      this.accounts.setBounds(x, y + (buttonHeight + gap) * 3.0F, fullWidth - exitWidth - gap, buttonHeight);
      this.exit.setBounds(x + fullWidth - exitWidth, this.accounts.y, exitWidth, buttonHeight);
   }

   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      if (this.client != null && MainMenuBackground.hasCustomPanorama(this.client)) {
         this.renderPanoramaBackground(context, deltaTicks);
         this.applyBlur(context);
      }
   }

   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      if (this.client == null || !MainMenuBackground.hasCustomPanorama(this.client)) {
         try {
            MainMenuShader.render(this.width <= 0 ? 0.5F : (float)mouseX / this.width, this.height <= 0 ? 0.5F : 1.0F - (float)mouseY / this.height);
         } catch (Throwable ignored) {
            MainMenuShader.shutdown();
            context.fillGradient(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), -15263716, -16250613);
         }
      }

      long now = System.nanoTime();
      float dt = Math.min(0.05F, (float)(now - this.lastFrame) * 1.0E-9F);
      this.lastFrame = now;
      this.intro = this.intro + (1.0F - this.intro) * Math.min(1.0F, dt * 4.2F);
      float mx = Render2D.convertX(mouseX);
      float my = Render2D.convertY(mouseY);
      float w = Render2D.getFixedScaledWidth();
      float h = Render2D.getFixedScaledHeight();
      this.updateLayout();
      this.renderBrand(context, w, h);
      this.renderUpdateNotice(context, mx, my, dt);

      for (MainMenuScreen.MenuButton button : this.buttons) {
         button.render(context, mx, my, dt, this.intro);
      }
   }

   private void renderUpdateNotice(DrawContext context, float mouseX, float mouseY, float dt) {
      if (MsdfManager.BOLD != null) {
         float textSize = 5.4F;
         float iconSize = 7.2F;
         float gap = 4.0F;
         float dotSize = 1.4F;
         float dotGap = 4.0F;
         float detailsGap = 4.0F;
         String title = "Вышло обновление!";
         String details = "Подробнее";
         float iconWidth = Msdf.width(MsdfManager.ICONS, "M", iconSize);
         float titleWidth = Msdf.width(MsdfManager.BOLD, title, textSize);
         float detailsWidth = Msdf.width(MsdfManager.BOLD, details, textSize);
         this.updateHeight = 18.0F;
         this.updateWidth = 9.0F + iconWidth + gap + dotSize + dotGap + titleWidth + detailsGap + detailsWidth + 9.0F;
         this.updateX = (Render2D.getFixedScaledWidth() - this.updateWidth) * 0.5F;
         this.updateY = 17.0F;
         boolean hovered = this.containsUpdate(mouseX, mouseY);
         float target = hovered ? 1.0F : 0.0F;
         this.updateHover = this.updateHover + (target - this.updateHover) * Math.min(1.0F, dt * 13.0F);
         Render2D.rect(
            context,
            this.updateX,
            this.updateY - this.updateHover * 0.5F,
            this.updateWidth,
            this.updateHeight,
            9.0F,
            ColorUtil.rgba(24, 24, 28, Math.round((218.0F + this.updateHover * 20.0F) * this.intro)),
            true
         );
         Render2D.outline(
            context,
            this.updateX,
            this.updateY - this.updateHover * 0.5F,
            this.updateWidth,
            this.updateHeight,
            9.0F,
            0.5F,
            ColorUtil.rgba(255, 255, 255, Math.round((15.0F + this.updateHover * 25.0F) * this.intro)),
            true
         );
         float contentY = this.updateY - this.updateHover * 0.5F;
         float iconX = this.updateX + 9.0F;
         int gray = ColorUtil.multAlpha(MUTED, this.intro);
         float centerY = contentY + this.updateHeight * 0.5F;
         float iconY = centerY - Msdf.height(MsdfManager.ICONS, iconSize) * 0.5F;
         Msdf.text(context, MsdfManager.ICONS, "M", iconX, iconY, iconSize, gray, true);
         float textY = contentY + (this.updateHeight - Msdf.height(MsdfManager.BOLD, textSize)) * 0.5F + 0.25F;
         float dotX = iconX + iconWidth + gap;
         Render2D.rect(context, dotX, centerY - dotSize * 0.5F, dotSize, dotSize, dotSize * 0.5F, ColorUtil.multAlpha(MUTED, this.intro * 0.45F), true);
         float titleX = dotX + dotSize + dotGap;
         Msdf.text(context, MsdfManager.BOLD, title, titleX, textY, textSize, ColorUtil.multAlpha(TEXT, this.intro), true);
         Msdf.text(
            context,
            MsdfManager.BOLD,
            details,
            titleX + titleWidth + detailsGap,
            textY,
            textSize,
            ColorUtil.multAlpha(MUTED, this.intro * (0.82F + this.updateHover * 0.18F)),
            true
         );
      }
   }

   private boolean containsUpdate(float mouseX, float mouseY) {
      return mouseX >= this.updateX && mouseX <= this.updateX + this.updateWidth && mouseY >= this.updateY - 1.0F && mouseY <= this.updateY + this.updateHeight;
   }

   private void renderBrand(DrawContext context, float w, float h) {
      float titleY = Math.max(79.0F, h * 0.29F);
      centered(context, MsdfManager.BOLD, "PHOBIA", w * 0.5F, titleY, 20.0F, ColorUtil.multAlpha(TEXT, this.intro * 0.82F));
      centered(context, MsdfManager.HAMBURG_REGULAR, "BETA  /  1.21.11", w * 0.5F, titleY + 25.0F, 5.5F, ColorUtil.multAlpha(FAINT, this.intro * 0.94F));
   }

   public boolean mouseClicked(Click click, boolean doubled) {
      if (click.button() != 0) {
         return super.mouseClicked(click, doubled);
      }

      this.updateLayout();
      float mx = Render2D.convertX((float)click.x());
      float my = Render2D.convertY((float)click.y());
      if (this.containsUpdate(mx, my)) {
         this.client.setScreen(new MainMenuUpdateScreen(this));
         return true;
      }

      for (MainMenuScreen.MenuButton button : this.buttons) {
         if (button.contains(mx, my)) {
            this.activate(button.action);
            return true;
         }
      }

      return super.mouseClicked(click, doubled);
   }

   private void activate(MainMenuScreen.Action action) {
      if (this.client != null) {
         switch (action) {
            case MULTIPLAYER:
               this.client.setScreen(new MultiplayerScreen(this));
               break;
            case SINGLEPLAYER:
               this.client.setScreen(new SelectWorldScreen(this));
               break;
            case OPTIONS:
               this.client.setScreen(new OptionsScreen(this, this.client.options));
               break;
            case ACCOUNTS:
               this.openAccountSwitcher();
               break;
            case EXIT:
               this.client.scheduleStop();
         }
      }
   }

   private void openAccountSwitcher() {
      try {
         Class<?> accountScreen = Class.forName("ru.vidtu.ias.screen.AccountScreen");
         Object screen = accountScreen.getConstructor(Screen.class).newInstance(this);
         if (screen instanceof Screen) {
            this.client.setScreen((Screen)screen);
         }
      } catch (ReflectiveOperationException var3) {
      }
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   private static void centered(DrawContext context, MsdfFont font, String text, float centerX, float y, float size, int color) {
      Msdf.text(context, font, text, centerX - Msdf.width(font, text, size) * 0.5F, y, size, color, true);
   }

   private enum Action {
      MULTIPLAYER,
      SINGLEPLAYER,
      OPTIONS,
      ACCOUNTS,
      EXIT;
   }

   private static final class MenuButton {
      final MainMenuScreen.Action action;
      final String label;
      final String icon;
      float x;
      float y;
      float width;
      float height;
      float hover;

      MenuButton(MainMenuScreen.Action action, String label, String icon) {
         this.action = action;
         this.label = label;
         this.icon = icon;
      }

      void setBounds(float x, float y, float width, float height) {
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
      }

      boolean contains(float mx, float my) {
         return mx >= this.x && mx <= this.x + this.width && my >= this.y && my <= this.y + this.height;
      }

      void render(DrawContext context, float mx, float my, float dt, float alpha) {
         float target = this.contains(mx, my) ? 1.0F : 0.0F;
         this.hover = this.hover + (target - this.hover) * Math.min(1.0F, dt * 13.0F);
         boolean danger = this.action == MainMenuScreen.Action.EXIT;
         float drawY = this.y - this.hover * 0.7F;
         Render2D.rect(
            context, this.x, drawY, this.width, this.height, 5.0F, ColorUtil.rgba(28, 28, 31, Math.round((214.0F + this.hover * 27.0F) * alpha)), true
         );
         Render2D.outline(
            context,
            this.x,
            drawY,
            this.width,
            this.height,
            5.0F,
            0.55F,
            danger
               ? ColorUtil.multAlpha(MainMenuScreen.DANGER, alpha * (0.22F + this.hover * 0.55F))
               : ColorUtil.rgba(255, 255, 255, Math.round((18.0F + this.hover * 38.0F) * alpha)),
            true
         );
         int textColor = danger
            ? ColorUtil.multAlpha(MainMenuScreen.DANGER, alpha * (0.74F + this.hover * 0.26F))
            : ColorUtil.multAlpha(this.hover > 0.35F ? MainMenuScreen.TEXT : MainMenuScreen.MUTED, alpha);
         if (MsdfManager.ICONS != null && !this.icon.isEmpty()) {
            float iconSize = danger ? 10.0F : 7.4F;
            float iconY = drawY + (this.height - Msdf.height(MsdfManager.ICONS, iconSize)) * 0.5F;
            if (this.label.isEmpty()) {
               MainMenuScreen.centered(context, MsdfManager.ICONS, this.icon, this.x + this.width * 0.5F, iconY, iconSize, textColor);
               return;
            }

            float textSize = 5.7F;
            float iconX = this.x + 8.0F;
            Msdf.text(context, MsdfManager.ICONS, this.icon, iconX, iconY, iconSize, textColor, true);
            MainMenuScreen.centered(context, MsdfManager.BOLD, this.label, this.x + this.width * 0.5F, drawY + 7.3F, textSize, textColor);
         } else {
            MainMenuScreen.centered(context, MsdfManager.BOLD, this.label, this.x + this.width * 0.5F, drawY + 7.3F, 5.7F, textColor);
         }
      }
   }
}
