package ruhack.phobia.system.screensystem.clickgui;

import com.adl.nativeprotect.User;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.util.render.scissor.Scissor;

public final class SidebarComponent {
   private static final Identifier LOGO = Identifier.of("phobia", "phobia.png");
   private static final Identifier AVATAR = Identifier.of("phobia", "images/main.png");
   private static final DateTimeFormatter[] DATE_TIME_FORMATS = new DateTimeFormatter[]{
      DateTimeFormatter.ofPattern("yyyy-dd-MM HH:mm:ss"),
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
   };
   private static final DateTimeFormatter[] DATE_FORMATS = new DateTimeFormatter[]{
      DateTimeFormatter.ofPattern("yyyy-dd-MM"), DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("dd.MM.yyyy")
   };
   private static String cachedSubscriptionLabel;
   private final Map<ClickGuiCategory, Float> animations = new EnumMap<>(ClickGuiCategory.class);
   private float x;
   private float y;
   private float width;
   private float height;
   private float rowsTop;
   private float rowsBottom;
   private float scroll;
   private float targetScroll;
   private float contentHeight;
   private long lastFrame = System.nanoTime();

   public void render(DrawContext context, float x, float y, float width, float height, ClickGuiCategory selected, float alpha, double mouseX, double mouseY) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      float dt = Math.min((float)(System.nanoTime() - this.lastFrame) / 1.0E9F, 0.1F);
      this.lastFrame = System.nanoTime();
      this.scroll = this.scroll + (this.targetScroll - this.scroll) * Math.min(1.0F, dt * 12.0F);
      float windowRadius = ClickGuiLayout.physicalToRender(15.0F);
      Render2D.blur(context, x, y, width, height, windowRadius, 0.0F, 0.0F, windowRadius, 9.0F, true);
      Render2D.rect(context, x, y, width, height, windowRadius, 0.0F, 0.0F, windowRadius, true, ClickGuiTheme.alpha(ClickGuiTheme.SIDEBAR, alpha));
      float onePx = ClickGuiLayout.physicalToRender(1.0F);
      Render2D.rect(
         context,
         x + width - onePx,
         y + ClickGuiLayout.physicalToRender(1.0F),
         onePx,
         height - ClickGuiLayout.physicalToRender(2.0F),
         0.0F,
         ClickGuiTheme.alpha(ClickGuiTheme.BORDER, alpha),
         true
      );
      float logoSize = ClickGuiLayout.physicalToRender(36.0F);
      float logoX = x + ClickGuiLayout.physicalToRender(12.0F);
      float logoY = y + ClickGuiLayout.physicalToRender(11.0F);
      ClickGuiEffects.logoGlow(context, logoX, logoY, logoSize, LOGO, alpha);
      Msdf.text(
         context,
         MsdfManager.BOLD,
         "PHOBIA",
         x + ClickGuiLayout.physicalToRender(55.0F),
         y + ClickGuiLayout.physicalToRender(17.0F),
         ClickGuiLayout.physicalToRender(13.0F),
         ClickGuiTheme.alpha(ClickGuiTheme.TEXT, alpha),
         true
      );
      Msdf.text(
         context,
         MsdfManager.BOLD,
         "client",
         x + ClickGuiLayout.physicalToRender(55.0F),
         y + ClickGuiLayout.physicalToRender(34.0F),
         ClickGuiLayout.physicalToRender(9.0F),
         ClickGuiTheme.alpha(ClickGuiTheme.TEXT_FAINT, alpha),
         true
      );
      this.rowsTop = y + ClickGuiLayout.physicalToRender(61.0F);
      this.rowsBottom = y + height - ClickGuiLayout.physicalToRender(69.0F);
      Scissor.push(x, this.rowsTop, width, this.rowsBottom - this.rowsTop, true);
      float cursor = this.rowsTop + ClickGuiLayout.physicalToRender(3.0F) + this.scroll;
      Msdf.text(
         context,
         MsdfManager.BOLD,
         "Main",
         x + ClickGuiLayout.physicalToRender(13.0F),
         cursor,
         ClickGuiLayout.physicalToRender(9.0F),
         ClickGuiTheme.alpha(ClickGuiTheme.TEXT_FAINT, alpha),
         true
      );
      cursor += ClickGuiLayout.physicalToRender(18.0F);

      for (ClickGuiCategory category : ClickGuiCategory.values()) {
         if (category != ClickGuiCategory.THEME && category != ClickGuiCategory.IRC) {
            cursor = this.drawCategory(context, category, selected, cursor, alpha, mouseX, mouseY, dt);
         }
      }

      cursor += ClickGuiLayout.physicalToRender(5.0F);
      Msdf.text(
         context,
         MsdfManager.BOLD,
         "Other",
         x + ClickGuiLayout.physicalToRender(13.0F),
         cursor,
         ClickGuiLayout.physicalToRender(9.0F),
         ClickGuiTheme.alpha(ClickGuiTheme.TEXT_FAINT, alpha),
         true
      );
      cursor += ClickGuiLayout.physicalToRender(18.0F);
      cursor = this.drawCategory(context, ClickGuiCategory.THEME, selected, cursor, alpha, mouseX, mouseY, dt);
      cursor = this.drawCategory(context, ClickGuiCategory.IRC, selected, cursor, alpha, mouseX, mouseY, dt);
      this.contentHeight = cursor - (this.rowsTop + this.scroll);
      Scissor.pop(true);
      this.renderProfile(context, alpha);
   }

   private float drawCategory(
      DrawContext context, ClickGuiCategory category, ClickGuiCategory selected, float rowY, float alpha, double mouseX, double mouseY, float dt
   ) {
      boolean active = selected == category;
      float sidePadding = ClickGuiLayout.physicalToRender(8.0F);
      float rowHeight = ClickGuiLayout.physicalToRender(30.0F);
      boolean hovered = inside(mouseX, mouseY, this.x + sidePadding, rowY, this.width - sidePadding * 2.0F, rowHeight);
      float target = active ? 1.0F : (hovered ? 0.48F : 0.0F);
      float anim = this.animations.getOrDefault(category, 0.0F);
      anim += (target - anim) * Math.min(1.0F, dt * 11.0F);
      this.animations.put(category, anim);
      if (anim > 0.01F) {
         int base = active ? ClickGuiTheme.SURFACE_ACTIVE : ClickGuiTheme.SURFACE_HOVER;
         Render2D.rect(
            context,
            this.x + sidePadding,
            rowY,
            this.width - sidePadding * 2.0F,
            rowHeight,
            ClickGuiLayout.physicalToRender(7.0F),
            ClickGuiTheme.alpha(base, alpha * anim),
            true
         );
      }

      int color = active ? ClickGuiTheme.TEXT : ClickGuiTheme.TEXT_MUTED;
      if (active) {
         Msdf.textGradient(
            context,
            MsdfManager.ICONS,
            category.getIcon(),
            this.x + ClickGuiLayout.physicalToRender(16.0F),
            rowY + ClickGuiLayout.physicalToRender(9.0F),
            ClickGuiLayout.physicalToRender(18.0F),
            ClickGuiTheme.accentAt(this.x, Math.round(alpha * 255.0F)),
            ClickGuiTheme.accentAt(this.x + this.width, Math.round(alpha * 255.0F)),
            true
         );
      } else {
         Msdf.text(
            context,
            MsdfManager.ICONS,
            category.getIcon(),
            this.x + ClickGuiLayout.physicalToRender(16.0F),
            rowY + ClickGuiLayout.physicalToRender(9.0F),
            ClickGuiLayout.physicalToRender(18.0F),
            ClickGuiTheme.alpha(color, alpha),
            true
         );
      }

      Msdf.text(
         context,
         MsdfManager.BOLD,
         category.getReadableName(),
         this.x + ClickGuiLayout.physicalToRender(36.0F),
         rowY + ClickGuiLayout.physicalToRender(9.0F),
         ClickGuiLayout.physicalToRender(12.0F),
         ClickGuiTheme.alpha(color, alpha),
         true
      );
      return rowY + ClickGuiLayout.physicalToRender(32.0F);
   }

   private void renderProfile(DrawContext context, float alpha) {
      float profileHeight = ClickGuiLayout.physicalToRender(61.0F);
      float profileY = this.y + this.height - profileHeight - ClickGuiLayout.physicalToRender(7.0F);
      float inset = ClickGuiLayout.physicalToRender(8.0F);
      Render2D.rect(
         context,
         this.x + inset,
         profileY,
         this.width - inset * 2.0F,
         ClickGuiLayout.physicalToRender(1.0F),
         0.0F,
         ClickGuiTheme.alpha(ClickGuiTheme.BORDER, alpha),
         true
      );
      String username = profile("username", "leaked by angelw1ngsov");
      String subTime = subscriptionLabel();
      float avatar = ClickGuiLayout.physicalToRender(36.0F);
      Render2D.image(
         context,
         this.x + ClickGuiLayout.physicalToRender(15.0F),
         profileY + (profileHeight - avatar) / 2.0F,
         avatar,
         AVATAR,
         ClickGuiTheme.alpha(-1, alpha),
         avatar / 2.0F,
         true
      );
      Msdf.text(
         context,
         MsdfManager.BOLD,
         username,
         this.x + ClickGuiLayout.physicalToRender(58.0F),
         profileY + ClickGuiLayout.physicalToRender(17.0F),
         ClickGuiLayout.physicalToRender(12.0F),
         ClickGuiTheme.alpha(ClickGuiTheme.TEXT, alpha),
         true
      );
      Msdf.text(
         context,
         MsdfManager.BOLD,
         subTime,
         this.x + ClickGuiLayout.physicalToRender(58.0F),
         profileY + ClickGuiLayout.physicalToRender(35.0F),
         ClickGuiLayout.physicalToRender(9.0F),
         ClickGuiTheme.alpha(ClickGuiTheme.TEXT_MUTED, alpha),
         true
      );
      Msdf.text(
         context,
         MsdfManager.ICONS,
         ">",
         this.x + this.width - ClickGuiLayout.physicalToRender(18.0F),
         profileY + ClickGuiLayout.physicalToRender(25.0F),
         ClickGuiLayout.physicalToRender(10.0F),
         ClickGuiTheme.alpha(ClickGuiTheme.TEXT_MUTED, alpha),
         true
      );
   }

   public ClickGuiCategory categoryAt(double mouseX, double mouseY) {
      if (!inside(mouseX, mouseY, this.x, this.rowsTop, this.width, this.rowsBottom - this.rowsTop)) {
         return null;
      }

      float cursor = this.rowsTop + ClickGuiLayout.physicalToRender(3.0F) + this.scroll;
      cursor += ClickGuiLayout.physicalToRender(18.0F);

      for (ClickGuiCategory category : ClickGuiCategory.values()) {
         if (category != ClickGuiCategory.THEME && category != ClickGuiCategory.IRC) {
            float sidePadding = ClickGuiLayout.physicalToRender(8.0F);
            if (inside(mouseX, mouseY, this.x + sidePadding, cursor, this.width - sidePadding * 2.0F, ClickGuiLayout.physicalToRender(30.0F))) {
               return category;
            }

            cursor += ClickGuiLayout.physicalToRender(32.0F);
         }
      }

      cursor += ClickGuiLayout.physicalToRender(23.0F);
      float sidePadding = ClickGuiLayout.physicalToRender(8.0F);
      if (inside(mouseX, mouseY, this.x + sidePadding, cursor, this.width - sidePadding * 2.0F, ClickGuiLayout.physicalToRender(30.0F))) {
         return ClickGuiCategory.THEME;
      }

      cursor += ClickGuiLayout.physicalToRender(32.0F);
      return inside(mouseX, mouseY, this.x + sidePadding, cursor, this.width - sidePadding * 2.0F, ClickGuiLayout.physicalToRender(30.0F))
         ? ClickGuiCategory.IRC
         : null;
   }

   public boolean scroll(double amount, double mouseX, double mouseY) {
      if (!inside(mouseX, mouseY, this.x, this.rowsTop, this.width, this.rowsBottom - this.rowsTop)) {
         return false;
      }

      this.targetScroll = this.targetScroll + (float)amount * ClickGuiLayout.physicalToRender(24.0F);
      float maxScroll = Math.min(0.0F, this.rowsBottom - this.rowsTop - this.contentHeight);
      this.targetScroll = Math.max(maxScroll, Math.min(0.0F, this.targetScroll));
      return true;
   }

   private static String profile(String key, String fallback) {
      try {
         String value = User.getInstance().profile(key);
         return value != null && !value.isBlank() ? value : fallback;
      } catch (Throwable ignored) {
         return fallback;
      }
   }

   private static String subscriptionLabel() {
      if (cachedSubscriptionLabel != null) {
         return cachedSubscriptionLabel;
      }

      ZonedDateTime expiry = parseExpiry(profile("subTime", ""));
      cachedSubscriptionLabel = expiry == null ? "No expiry date" : remainingLabel(Duration.between(ZonedDateTime.now(), expiry).getSeconds());
      return cachedSubscriptionLabel;
   }

   private static ZonedDateTime parseExpiry(String value) {
      if (value != null && !value.isBlank()) {
         String normalized = value.trim().replace('T', ' ');
         ZoneId zone = ZoneId.systemDefault();

         for (DateTimeFormatter format : DATE_TIME_FORMATS) {
            try {
               return LocalDateTime.parse(normalized, format).atZone(zone);
            } catch (DateTimeParseException var9) {
            }
         }

         for (DateTimeFormatter format : DATE_FORMATS) {
            try {
               return LocalDate.parse(normalized, format).atTime(LocalTime.MAX).atZone(zone);
            } catch (DateTimeParseException var8) {
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static String remainingLabel(long seconds) {
      if (seconds <= 0L) {
         return "expired";
      }

      long[] durations = new long[]{31536000L, 2592000L, 86400L, 3600L, 60L};
      String[] units = new String[]{"year", "month", "day", "hour", "minute"};

      for (int i = 0; i < durations.length; i++) {
         long amount = seconds / durations[i];
         if (amount > 0L) {
            return "expires in " + amount + " " + units[i] + (amount == 1L ? "" : "s");
         }
      }

      return "expires in less than a minute";
   }

   private static boolean inside(double mx, double my, float x, float y, float w, float h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }
}
