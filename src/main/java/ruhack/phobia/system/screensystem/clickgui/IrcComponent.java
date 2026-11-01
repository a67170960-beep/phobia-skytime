package ruhack.phobia.system.screensystem.clickgui;

import com.adl.nativeprotect.User;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.util.render.scissor.Scissor;

public final class IrcComponent {
   private static final int MAX_MESSAGES = 200;
   private static final int MAX_INPUT_LENGTH = 200;
   private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
   private static final int[] USER_COLORS = new int[]{-10303528, -1673305, -7439128, -9975411, -1464738, -10574359, -2852120, -9059416};
   private final List<IrcComponent.Message> messages = new ArrayList<>();
   private final IrcClient client = IrcClient.INSTANCE;
   private boolean clientStarted;
   private String input = "";
   private boolean inputFocused;
   private boolean selectAll;
   private float x;
   private float y;
   private float width;
   private float height;
   private float inputY;
   private float messageTop;
   private float messageBottom;
   private float sendX;
   private float sendY;
   private float sendSize;
   private float scroll;
   private float targetScroll;
   private float contentHeight;

   public void render(DrawContext context, float x, float y, float width, float height, float alpha, double mouseX, double mouseY, float dt) {
      this.ensureClientStarted();
      this.drainMessages();
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.scroll = this.scroll + (this.targetScroll - this.scroll) * Math.min(1.0F, dt * 14.0F);
      float titleSize = px(15.0F);
      Msdf.text(context, MsdfManager.BOLD, "IRC", x, y + px(2.0F), titleSize, ClickGuiTheme.alpha(ClickGuiTheme.TEXT, alpha), true);
      float titleWidth = Msdf.width(MsdfManager.BOLD, "IRC", titleSize);

      int statusColor = switch (this.client.state()) {
         case ONLINE -> -11150966;
         case CONNECTING -> -1656484;
         case OFFLINE -> -2071452;
      };

      String statusText = switch (this.client.state()) {
         case ONLINE -> "В сети";
         case CONNECTING -> "Подключение...";
         case OFFLINE -> "Нет соединения";
      };
      float statusX = x + titleWidth + px(11.0F);
      float statusY = y + px(6.0F);
      Render2D.rect(context, statusX, statusY + px(3.0F), px(6.0F), px(6.0F), px(3.0F), ClickGuiTheme.alpha(statusColor, alpha), true);
      Msdf.text(context, MsdfManager.BOLD, statusText, statusX + px(10.0F), statusY, px(10.0F), ClickGuiTheme.alpha(statusColor, alpha), true);
      String onlineText = onlineText(this.client.onlineCount());
      float onlineWidth = Msdf.width(MsdfManager.BOLD, onlineText, px(9.0F));
      Msdf.text(context, MsdfManager.BOLD, onlineText, x + width - onlineWidth, statusY, px(9.0F), ClickGuiTheme.alpha(ClickGuiTheme.TEXT_MUTED, alpha), true);
      float inputHeight = px(30.0F);
      this.inputY = y + height - inputHeight;
      boolean inputHovered = inside(mouseX, mouseY, x, this.inputY, width, inputHeight);
      this.drawInputBox(context, x, this.inputY, width, inputHeight, inputHovered, alpha);
      this.sendSize = px(22.0F);
      this.sendX = x + width - this.sendSize - px(4.0F);
      this.sendY = this.inputY + (inputHeight - this.sendSize) / 2.0F;
      boolean sendHovered = inside(mouseX, mouseY, this.sendX, this.sendY, this.sendSize, this.sendSize);
      int sendColor = this.input.isBlank()
         ? ClickGuiTheme.SURFACE_HOVER
         : ClickGuiTheme.accentAt(this.sendX, Math.round(alpha * (sendHovered ? 255.0F : 210.0F)));
      Render2D.rect(context, this.sendX, this.sendY, this.sendSize, this.sendSize, this.sendSize / 2.0F, ClickGuiTheme.alpha(sendColor, alpha), true);
      float arrowSize = px(11.0F);
      float arrowWidth = Msdf.width(MsdfManager.BOLD, "↑", arrowSize);
      Msdf.text(
         context,
         MsdfManager.BOLD,
         "↑",
         this.sendX + (this.sendSize - arrowWidth) / 2.0F,
         this.sendY + px(5.0F),
         arrowSize,
         ClickGuiTheme.alpha(ClickGuiTheme.TEXT, alpha),
         true
      );
      String inputText = this.input.isEmpty() ? "Написать в IRC..." : this.input;
      Msdf.text(
         context,
         MsdfManager.BOLD,
         inputText,
         x + px(10.0F),
         this.inputY + px(9.0F),
         px(10.0F),
         ClickGuiTheme.alpha(this.input.isEmpty() ? ClickGuiTheme.TEXT_FAINT : ClickGuiTheme.TEXT, alpha),
         true
      );
      this.drawCaret(context, this.input, x + px(10.0F), this.inputY + px(8.0F), this.inputFocused, alpha);
      this.messageTop = y + px(31.0F);
      this.messageBottom = this.inputY - px(9.0F);
      this.renderMessages(context, alpha);
   }

   private void renderMessages(DrawContext context, float alpha) {
      float fontSize = px(10.0F);
      float textLineHeight = px(13.0F);
      float avatarSize = px(21.0F);
      float textX = this.x + avatarSize + px(8.0F);
      float textWidth = this.width - avatarSize - px(12.0F);
      List<IrcComponent.MessageLayout> layouts = this.buildLayouts(fontSize, textWidth, avatarSize, textLineHeight);
      this.contentHeight = 0.0F;

      for (IrcComponent.MessageLayout layout : layouts) {
         this.contentHeight = this.contentHeight + layout.height();
      }

      this.targetScroll = clamp(this.targetScroll, Math.min(0.0F, this.messageBottom - this.messageTop - this.contentHeight), 0.0F);
      this.scroll = clamp(this.scroll, Math.min(0.0F, this.messageBottom - this.messageTop - this.contentHeight), 0.0F);
      Scissor.push(this.x, this.messageTop, this.width, Math.max(0.0F, this.messageBottom - this.messageTop), true);
      float messageY = this.messageTop + this.scroll;

      for (IrcComponent.MessageLayout layout : layouts) {
         if (messageY + layout.height() >= this.messageTop && messageY <= this.messageBottom) {
            IrcComponent.Message message = layout.message();
            int userColor = userColor(message.author());
            Render2D.rect(
               context, this.x, messageY + px(1.0F), avatarSize, avatarSize, avatarSize / 2.0F, ClickGuiTheme.alpha(withAlpha(userColor, 58), alpha), true
            );
            String initial = firstLetter(message.author());
            float initialSize = px(9.0F);
            float initialWidth = Msdf.width(MsdfManager.BOLD, initial, initialSize);
            Msdf.text(
               context,
               MsdfManager.BOLD,
               initial,
               this.x + (avatarSize - initialWidth) / 2.0F,
               messageY + px(6.0F),
               initialSize,
               ClickGuiTheme.alpha(userColor, alpha),
               true
            );
            int roleColor = roleColor(message.role());
            Msdf.text(context, MsdfManager.BOLD, message.role(), textX, messageY, fontSize, ClickGuiTheme.alpha(roleColor, alpha), true);
            float roleWidth = Msdf.width(MsdfManager.BOLD, message.role(), fontSize);
            Msdf.text(
               context, MsdfManager.BOLD, message.author(), textX + roleWidth + px(4.0F), messageY, fontSize, ClickGuiTheme.alpha(userColor, alpha), true
            );
            float authorWidth = Msdf.width(MsdfManager.BOLD, message.author(), fontSize);
            Msdf.text(
               context,
               MsdfManager.BOLD,
               formatTime(message.timestamp()),
               textX + roleWidth + authorWidth + px(10.0F),
               messageY + px(1.0F),
               px(8.0F),
               ClickGuiTheme.alpha(ClickGuiTheme.TEXT_FAINT, alpha),
               true
            );
            float lineY = messageY + px(13.0F);

            for (String line : layout.lines()) {
               Msdf.text(context, MsdfManager.BOLD, line, textX, lineY, fontSize, ClickGuiTheme.alpha(ClickGuiTheme.TEXT, alpha), true);
               lineY += textLineHeight;
            }
         }

         messageY += layout.height();
      }

      Scissor.pop(true);
   }

   private List<IrcComponent.MessageLayout> buildLayouts(float fontSize, float textWidth, float avatarSize, float lineHeight) {
      List<IrcComponent.MessageLayout> layouts = new ArrayList<>();

      for (IrcComponent.Message message : this.messages) {
         List<String> wrapped = this.wrap(message.text(), textWidth, fontSize);
         float textHeight = px(13.0F) + wrapped.size() * lineHeight;
         layouts.add(new IrcComponent.MessageLayout(message, wrapped, Math.max(avatarSize, textHeight) + px(7.0F)));
      }

      return layouts;
   }

   private List<String> wrap(String text, float width, float fontSize) {
      List<String> result = new ArrayList<>();
      StringBuilder line = new StringBuilder();

      for (String word : text.split("\\s+")) {
         String candidate = line.isEmpty() ? word : line + " " + word;
         if (!line.isEmpty() && Msdf.width(MsdfManager.BOLD, candidate, fontSize) > width) {
            result.add(line.toString());
            line.setLength(0);
            line.append(word);
         } else {
            line.setLength(0);
            line.append(candidate);
         }
      }

      if (!line.isEmpty()) {
         result.add(line.toString());
      }

      if (result.isEmpty()) {
         result.add("");
      }

      return result;
   }

   private void drawInputBox(DrawContext context, float x, float y, float width, float height, boolean hovered, float alpha) {
      ClickGuiEffects.glass(
         context,
         x,
         y,
         width,
         height,
         px(7.0F),
         hovered ? ClickGuiTheme.SURFACE_HOVER : ClickGuiTheme.GLASS_INSET,
         5.0F,
         ClickGuiEffects.ShadowLevel.CARD,
         alpha,
         false
      );
   }

   private void drawCaret(DrawContext context, String value, float x, float y, boolean focused, float alpha) {
      if (focused && !this.selectAll && System.currentTimeMillis() / 500L % 2L == 0L) {
         float caretX = x + Msdf.width(MsdfManager.BOLD, value, px(10.0F));
         Render2D.rect(context, caretX, y, px(1.0F), px(13.0F), 0.0F, ClickGuiTheme.accentAt(caretX, Math.round(alpha * 255.0F)), true);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return false;
      } else if (inside(mouseX, mouseY, this.sendX, this.sendY, this.sendSize, this.sendSize)) {
         this.send();
         return true;
      } else {
         boolean inInput = inside(mouseX, mouseY, this.x, this.inputY, this.width, px(30.0F));
         this.inputFocused = inInput;
         this.selectAll = false;
         return inInput;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      if (!inside(mouseX, mouseY, this.x, this.messageTop, this.width, this.messageBottom - this.messageTop)) {
         return false;
      }

      this.targetScroll = this.targetScroll + (float)amount * px(24.0F);
      this.targetScroll = clamp(this.targetScroll, Math.min(0.0F, this.messageBottom - this.messageTop - this.contentHeight), 0.0F);
      return true;
   }

   public boolean keyPressed(KeyInput key) {
      if (!this.inputFocused) {
         return false;
      }

      if (key.key() == 256) {
         this.inputFocused = false;
         this.selectAll = false;
         return true;
      }

      if (!this.inputFocused || key.key() != 257 && key.key() != 335) {
         if (key.key() == 65 && (key.modifiers() & 2) != 0) {
            this.selectAll = true;
            return true;
         } else if (key.key() == 259) {
            this.replaceFocused(this.selectAll ? "" : removeLast(this.focusedValue()));
            this.selectAll = false;
            return true;
         } else if (key.key() == 261) {
            this.replaceFocused("");
            this.selectAll = false;
            return true;
         } else {
            return false;
         }
      } else {
         this.send();
         return true;
      }
   }

   public boolean charTyped(CharInput input) {
      if (this.inputFocused && !Character.isISOControl(input.codepoint())) {
         String value = this.selectAll ? "" : this.focusedValue();
         String addition = new String(Character.toChars(input.codepoint()));
         if (value.codePointCount(0, value.length()) < 200) {
            this.replaceFocused(value + addition);
         }

         this.selectAll = false;
         return true;
      } else {
         return false;
      }
   }

   public void resetFocus() {
      this.inputFocused = false;
      this.selectAll = false;
   }

   private void send() {
      String text = this.input.trim();
      if (!text.isEmpty()) {
         if (this.client.sendMessage(text)) {
            this.input = "";
            this.selectAll = false;
            this.targetScroll = Math.min(0.0F, this.messageBottom - this.messageTop - this.contentHeight - px(15.0F));
         }
      }
   }

   private void ensureClientStarted() {
      if (!this.clientStarted) {
         this.clientStarted = true;
         this.client.startFromProfile();
      }
   }

   private void drainMessages() {
      IrcClient.Event event;
      boolean added;
      for (added = false; (event = this.client.pollEvent()) != null; added = true) {
         this.messages.add(new IrcComponent.Message(event.role(), event.author(), event.text(), event.timestamp()));
         if (this.messages.size() > 200) {
            this.messages.removeFirst();
         }
      }

      if (added) {
         this.targetScroll = Math.min(0.0F, this.messageBottom - this.messageTop - this.contentHeight - px(15.0F));
      }
   }

   private String focusedValue() {
      return this.input;
   }

   private void replaceFocused(String value) {
      this.input = value;
   }

   private static String removeLast(String value) {
      return value.isEmpty() ? value : value.substring(0, value.offsetByCodePoints(value.length(), -1));
   }

   private static String profileName() {
      try {
         String value = User.getInstance().profile("username");
         return value != null && !value.isBlank() ? value : "leaked by angelw1ngsov";
      } catch (Throwable ignored) {
         return "leaked by angelw1ngsov";
      }
   }

   private static String profileRole() {
      try {
         String value = User.getInstance().profile("role");
         return value != null && !value.isBlank() ? normalizeRole(value) : "User";
      } catch (Throwable ignored) {
         return "User";
      }
   }

   private static String normalizeRole(String role) {
      return IrcClient.normalizeRole(role);
   }

   private static int roleColor(String role) {
      return switch (role.toLowerCase(Locale.ROOT)) {
         case "admin" -> -43691;
         case "media" -> -35403;
         case "beta" -> -11150966;
         default -> ClickGuiTheme.TEXT;
      };
   }

   private static int userColor(String author) {
      return USER_COLORS[Math.floorMod(author.toLowerCase(Locale.ROOT).hashCode(), USER_COLORS.length)];
   }

   private static int withAlpha(int color, int alpha) {
      return Math.max(0, Math.min(255, alpha)) << 24 | color & 16777215;
   }

   private static String firstLetter(String value) {
      if (value != null && !value.isBlank()) {
         int codePoint = value.codePointAt(0);
         return new String(Character.toChars(Character.toUpperCase(codePoint)));
      } else {
         return "?";
      }
   }

   private static String formatTime(long timestamp) {
      return TIME_FORMAT.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()));
   }

   private static String onlineText(int count) {
      int absolute = Math.abs(count) % 100;
      int last = absolute % 10;
      String noun = absolute > 10 && absolute < 20
         ? "пользователей"
         : (last == 1 ? "пользователь" : (last >= 2 && last <= 4 ? "пользователя" : "пользователей"));
      return count + " " + noun + " в сети";
   }

   private static float px(float value) {
      return ClickGuiLayout.physicalToRender(value);
   }

   private static boolean inside(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private record Message(String role, String author, String text, long timestamp) {
   }

   private record MessageLayout(IrcComponent.Message message, List<String> lines, float height) {
   }
}
