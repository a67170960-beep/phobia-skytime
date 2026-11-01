package ruhack.phobia.system.screensystem;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.system.rendersystem.render2d.menu.MainMenuBackground;
import ruhack.phobia.system.rendersystem.render2d.menu.MainMenuShader;
import ruhack.phobia.util.colorschem.ColorUtil;

public final class MainMenuUpdateScreen extends Screen {
   private static final int TEXT = ColorUtil.rgba(231, 231, 235, 255);
   private static final int MUTED = ColorUtil.rgba(151, 151, 160, 255);
   private static final int ADDED = ColorUtil.rgba(83, 205, 137, 255);
   private static final int FIXED = ColorUtil.rgba(168, 168, 176, 255);
   private static final int REMOVED = ColorUtil.rgba(238, 78, 92, 255);
   private static final MainMenuUpdateScreen.Change[] CHANGES = new MainMenuUpdateScreen.Change[]{
      new MainMenuUpdateScreen.Change(MainMenuUpdateScreen.Type.ADDED, "AntiLagMachine", "Фиксит лаг-машины на ReallyWorld, которые снижают FPS до нуля."),
      new MainMenuUpdateScreen.Change(
         MainMenuUpdateScreen.Type.ADDED, "TargetPearl", "Добавлен чек-бокс «Игнорировать друзей»: жемчуг не бросается вслед за друзьями."
      ),
      new MainMenuUpdateScreen.Change(MainMenuUpdateScreen.Type.ADDED, "GuiMove", "Добавлен режим «ReallyWorld» с полным обходом проверок сервера."),
      new MainMenuUpdateScreen.Change(
         MainMenuUpdateScreen.Type.ADDED, "FreeCam", "Добавлен чек-бокс «Зависать»: при включении FreeCam игрок остаётся в воздухе."
      ),
      new MainMenuUpdateScreen.Change(MainMenuUpdateScreen.Type.ADDED, "Aiming Balls", "Наводка на элитры, шары, осколки, Jack, O6 и O7."),
      new MainMenuUpdateScreen.Change(MainMenuUpdateScreen.Type.ADDED, "AntiAFK", "Не позволяет системам Anti-AFK кикать игрока за бездействие."),
      new MainMenuUpdateScreen.Change(MainMenuUpdateScreen.Type.REMOVED, "TargetPearl", "Удалена настройка «Только за таргетом»."),
      new MainMenuUpdateScreen.Change(MainMenuUpdateScreen.Type.FIXED, "AutoContract", "Доработан поиск контрактов и корректный вход на гриф."),
      new MainMenuUpdateScreen.Change(MainMenuUpdateScreen.Type.FIXED, "Interface", "В элементе Watermark старый логотип заменён новым."),
      new MainMenuUpdateScreen.Change(MainMenuUpdateScreen.Type.FIXED, "Discord RPC", "Исправлено отображение IP сервера, на котором играет пользователь.")
   };
   private final Screen parent;
   private long lastFrame = System.nanoTime();
   private float intro;

   public MainMenuUpdateScreen(Screen parent) {
      super(Text.literal("Phobia Changelog"));
      this.parent = parent;
      MainMenuScreen.protectFromFancyMenu(this);
   }

   protected void init() {
      MainMenuScreen.protectFromFancyMenu(this);
      this.intro = 0.0F;
      this.lastFrame = System.nanoTime();
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
      this.intro = this.intro + (1.0F - this.intro) * Math.min(1.0F, dt * 5.0F);
      this.renderPanel(context);
   }

   private void renderPanel(DrawContext context) {
      if (MsdfManager.BOLD != null) {
         MsdfFont bold = MsdfManager.BOLD;
         float screenW = Render2D.getFixedScaledWidth();
         float screenH = Render2D.getFixedScaledHeight();
         float panelW = Math.min(270.0F, Math.max(235.0F, screenW - 36.0F));
         float headerH = 31.0F;
         float pad = 11.0F;
         float textSize = 5.35F;
         float lineHeight = 7.1F;
         float rowGap = 3.5F;
         float contentWidth = panelW - pad * 2.0F - 8.0F;
         List<MainMenuUpdateScreen.WrappedChange> wrappedChanges = new ArrayList<>();
         float rowsHeight = 0.0F;

         for (MainMenuUpdateScreen.Change change : CHANGES) {
            String prefix = change.type.label + " " + change.module + " - ";
            float prefixWidth = Msdf.width(bold, prefix, textSize);
            List<String> lines = wrap(bold, change.description, textSize, Math.max(45.0F, contentWidth - prefixWidth));
            wrappedChanges.add(new MainMenuUpdateScreen.WrappedChange(change, prefix, prefixWidth, lines));
            rowsHeight += Math.max(1, lines.size()) * lineHeight + rowGap;
         }

         float footerH = 18.0F;
         float panelH = headerH + 8.0F + rowsHeight + footerH;
         panelH = Math.min(panelH, Math.max(210.0F, screenH - 30.0F));
         float x = (screenW - panelW) * 0.5F;
         float y = (screenH - panelH) * 0.5F;
         Render2D.blur(context, x, y, panelW, panelH, 7.0F, 14.0F, true);
         Render2D.rect(context, x, y, panelW, panelH, 7.0F, ColorUtil.rgba(15, 15, 20, Math.round(238.0F * this.intro)), true);
         Render2D.outline(context, x, y, panelW, panelH, 7.0F, 0.55F, ColorUtil.rgba(255, 255, 255, Math.round(20.0F * this.intro)), true);
         Render2D.rect(context, x, y + headerH - 0.5F, panelW, 0.5F, 0.0F, ColorUtil.rgba(255, 255, 255, Math.round(17.0F * this.intro)), true);
         float titleSize = 6.8F;
         float titleY = y + (headerH - Msdf.height(bold, titleSize)) * 0.5F;
         Msdf.text(context, bold, "CHANGELOG", x + pad, titleY, titleSize, ColorUtil.multAlpha(TEXT, this.intro), true);
         float statsSize = 5.1F;
         String added = "Added: 6";
         String fixed = "Fixed: 3";
         String removed = "Removed: 1";
         float removedW = Msdf.width(bold, removed, statsSize);
         float fixedW = Msdf.width(bold, fixed, statsSize);
         float statsY = y + (headerH - Msdf.height(bold, statsSize)) * 0.5F;
         float removedX = x + panelW - pad - removedW;
         float fixedX = removedX - 9.0F - fixedW;
         float addedX = fixedX - 9.0F - Msdf.width(bold, added, statsSize);
         Msdf.text(context, bold, added, addedX, statsY, statsSize, ColorUtil.multAlpha(ADDED, this.intro), true);
         Msdf.text(context, bold, fixed, fixedX, statsY, statsSize, ColorUtil.multAlpha(FIXED, this.intro), true);
         Msdf.text(context, bold, removed, removedX, statsY, statsSize, ColorUtil.multAlpha(REMOVED, this.intro), true);
         float cursorY = y + headerH + 8.0F;

         for (MainMenuUpdateScreen.WrappedChange wrapped : wrappedChanges) {
            MainMenuUpdateScreen.Change change = wrapped.change;

            int statusColor = switch (change.type) {
               case ADDED -> ADDED;
               case FIXED -> FIXED;
               case REMOVED -> REMOVED;
            };

            String marker = switch (change.type) {
               case ADDED -> "+";
               case FIXED -> "~";
               case REMOVED -> "-";
            };
            float lineX = x + pad + 8.0F;
            Msdf.text(context, bold, marker, x + pad, cursorY, textSize, ColorUtil.multAlpha(statusColor, this.intro), true);
            Msdf.text(context, bold, wrapped.prefix, lineX, cursorY, textSize, ColorUtil.multAlpha(statusColor, this.intro), true);
            List<String> lines = wrapped.lines;
            if (!lines.isEmpty()) {
               Msdf.text(context, bold, lines.getFirst(), lineX + wrapped.prefixWidth, cursorY, textSize, ColorUtil.multAlpha(TEXT, this.intro * 0.86F), true);

               for (int line = 1; line < lines.size(); line++) {
                  cursorY += lineHeight;
                  Msdf.text(context, bold, lines.get(line), lineX, cursorY, textSize, ColorUtil.multAlpha(TEXT, this.intro * 0.86F), true);
               }
            }

            cursorY += lineHeight + rowGap;
         }

         String hint = "ESC - НАЗАД";
         float hintSize = 5.0F;
         Msdf.text(
            context,
            bold,
            hint,
            x + panelW - pad - Msdf.width(bold, hint, hintSize),
            y + panelH - 8.0F - Msdf.height(bold, hintSize),
            hintSize,
            ColorUtil.multAlpha(MUTED, this.intro * 0.75F),
            true
         );
      }
   }

   private static List<String> wrap(MsdfFont font, String text, float size, float maxWidth) {
      List<String> lines = new ArrayList<>();
      StringBuilder line = new StringBuilder();

      for (String word : text.split(" ")) {
         String candidate = line.isEmpty() ? word : line + " " + word;
         if (!line.isEmpty() && Msdf.width(font, candidate, size) > maxWidth) {
            lines.add(line.toString());
            line.setLength(0);
            line.append(word);
         } else {
            if (!line.isEmpty()) {
               line.append(' ');
            }

            line.append(word);
         }
      }

      if (!line.isEmpty()) {
         lines.add(line.toString());
      }

      return lines;
   }

   public void close() {
      if (this.client != null) {
         this.client.setScreen(this.parent);
      }
   }

   private record Change(MainMenuUpdateScreen.Type type, String module, String description) {
   }

   private enum Type {
      ADDED("Added"),
      FIXED("Fixed"),
      REMOVED("Removed");

      private final String label;

      Type(String label) {
         this.label = label;
      }
   }

   private record WrappedChange(MainMenuUpdateScreen.Change change, String prefix, float prefixWidth, List<String> lines) {
   }
}
