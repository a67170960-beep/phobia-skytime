package ruhack.phobia.system.screensystem.clickgui;

import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.util.colorschem.ColorUtil;

public final class ClickGuiTheme {
   public static final int BACKDROP = ColorUtil.rgba(7, 8, 13, 82);
   public static final int WINDOW = ColorUtil.rgba(12, 12, 16, 216);
   public static final int SIDEBAR = ColorUtil.rgba(14, 14, 19, 208);
   public static final int HEADER = ColorUtil.rgba(12, 12, 17, 198);
   public static final int FLOATING = ColorUtil.rgba(15, 15, 21, 230);
   public static final int SURFACE = ColorUtil.rgba(22, 22, 29, 142);
   public static final int SURFACE_HOVER = ColorUtil.rgba(32, 31, 41, 186);
   public static int SURFACE_ACTIVE = ColorUtil.rgba(48, 46, 61, 194);
   public static final int GLASS_INSET = ColorUtil.rgba(22, 22, 30, 124);
   public static final int GLASS_PROFILE = ColorUtil.rgba(22, 22, 29, 158);
   public static final int BORDER = ColorUtil.rgba(158, 151, 181, 16);
   public static int BORDER_STRONG = ColorUtil.rgba(178, 166, 218, 25);
   public static int HIGHLIGHT = ColorUtil.rgba(228, 222, 255, 22);
   public static final int TEXT = ColorUtil.rgba(241, 239, 248, 255);
   public static final int TEXT_MUTED = ColorUtil.rgba(150, 147, 163, 255);
   public static final int TEXT_FAINT = ColorUtil.rgba(116, 113, 126, 255);
   public static int ACCENT = ColorUtil.rgba(178, 154, 255, 255);
   public static int ACCENT_BRIGHT = ColorUtil.rgba(190, 169, 255, 255);
   public static int ACCENT_SOFT = ColorUtil.rgba(178, 154, 255, 58);
   public static int ACCENT_GLOW = ColorUtil.rgba(139, 111, 255, 42);
   public static final int TOGGLE_OFF = ColorUtil.rgba(43, 43, 52, 255);
   public static final int TOGGLE_KNOB = ColorUtil.rgba(199, 197, 208, 255);
   public static final int SHADOW_WINDOW = ColorUtil.rgba(2, 2, 7, 84);
   public static final int SHADOW_FLOATING = ColorUtil.rgba(1, 1, 6, 92);
   public static final int SHADOW_CARD = ColorUtil.rgba(2, 2, 7, 38);

   private ClickGuiTheme() {
   }

   public static void syncAccent() {
      int accent = ColorUtil.getClientColorAt(0.0F);
      int r = ColorUtil.getRed(accent);
      int g = ColorUtil.getGreen(accent);
      int b = ColorUtil.getBlue(accent);
      ACCENT = ColorUtil.rgba(r, g, b, 255);
      ACCENT_BRIGHT = ColorUtil.rgba(mix(r, 255, 0.16F), mix(g, 255, 0.16F), mix(b, 255, 0.16F), 255);
      ACCENT_SOFT = ColorUtil.rgba(r, g, b, 58);
      ACCENT_GLOW = ColorUtil.rgba(r, g, b, 42);
      SURFACE_ACTIVE = ColorUtil.rgba(mix(34, r, 0.12F), mix(33, g, 0.12F), mix(43, b, 0.12F), 194);
      BORDER_STRONG = ColorUtil.rgba(r, g, b, 25);
      HIGHLIGHT = ColorUtil.rgba(mix(r, 255, 0.68F), mix(g, 255, 0.68F), mix(b, 255, 0.68F), 22);
   }

   private static int mix(int from, int to, float amount) {
      return Math.round(from + (to - from) * amount);
   }

   public static int alpha(int color, float alpha) {
      return ColorUtil.multiplyAlpha(color, Math.max(0.0F, Math.min(1.0F, alpha)));
   }

   public static int accent(int alpha) {
      return ColorUtil.withAlpha(ACCENT, Math.max(0, Math.min(255, alpha)));
   }

   public static int accentAt(float x, int alpha) {
      float position = x / Math.max(1.0F, Render2D.getFixedScaledWidth());
      return ColorUtil.getClientColorAt(position, Math.max(0, Math.min(255, alpha)));
   }

   public static int[] accentGradient(float x, float width, float alpha) {
      int[] colors = new int[9];
      int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
      float screen = Math.max(1.0F, Render2D.getFixedScaledWidth());
      float left = x / screen;
      float right = (x + width) / screen;

      for (int i = 0; i < colors.length; i++) {
         float column = i % 3 / 2.0F;
         colors[i] = ColorUtil.getClientColorAt(left + (right - left) * column, a);
      }

      return colors;
   }

   public static int[] mixedAccentGradient(int from, float x, float width, float amount, float alpha) {
      int[] colors = accentGradient(x, width, alpha);
      int fromWithAlpha = ColorUtil.replAlpha(from, Math.round(alpha * 255.0F));

      for (int i = 0; i < colors.length; i++) {
         colors[i] = ColorUtil.interpolateColor(fromWithAlpha, colors[i], amount);
      }

      return colors;
   }
}
