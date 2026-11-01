package ruhack.phobia.util.colorschem;

import java.awt.Color;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4i;
import ruhack.phobia.system.modulesystem.impl.other.Theme;
import ruhack.phobia.util.math.MathUtils;

public class ColorUtil {
   private static int clientColorR = 255;
   private static int clientColorG = 0;
   private static int clientColorB = 0;
   private static int[] clientColors = new int[3];
   private static String clientColorMode = "Статичный";
   public static final int green = new Color(64, 255, 64).getRGB();
   public static final int yellow = new Color(255, 255, 64).getRGB();
   public static final int orange = new Color(255, 128, 32).getRGB();
   public static final int red = new Color(255, 64, 64).getRGB();
   private static long colorTransitionStart = System.currentTimeMillis();
   public static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("(?i)§[0-9a-f-or]");

   private static int computeColor(int red, int green, int blue, int alpha) {
      return MathHelper.clamp(alpha, 0, 255) << 24
         | MathHelper.clamp(red, 0, 255) << 16
         | MathHelper.clamp(green, 0, 255) << 8
         | MathHelper.clamp(blue, 0, 255);
   }

   public static int replAlpha(int color, int alpha) {
      return getColor(red(color), green(color), blue(color), alpha);
   }

   public static Vector4i multRedAndAlpha(Vector4i color, float red, float alpha) {
      return new Vector4i(
         multRedAndAlpha(color.x, red, alpha), multRedAndAlpha(color.y, red, alpha), multRedAndAlpha(color.w, red, alpha), multRedAndAlpha(color.z, red, alpha)
      );
   }

   public static int red(int c) {
      return c >> 16 & 0xFF;
   }

   public static int green(int c) {
      return c >> 8 & 0xFF;
   }

   public static int blue(int c) {
      return c & 0xFF;
   }

   public static int alpha(int c) {
      return c >> 24 & 0xFF;
   }

   public static int multRedAndAlpha(int color, float red, float alpha) {
      return getColor(red(color), Math.min(255, Math.round(green(color) / red)), Math.min(255, Math.round(blue(color) / red)), Math.round(alpha(color) * alpha));
   }

   public static int getColor(int red, int green, int blue, int alpha) {
      return computeColor(red, green, blue, alpha);
   }

   public static void setClientColors(int[] colors) {
      if (colors != null && colors.length > 0) {
         clientColors = new int[3];

         for (int i = 0; i < Math.min(colors.length, 3); i++) {
            clientColors[i] = colors[i];
         }

         colorTransitionStart = System.currentTimeMillis();
      }
   }

   public static void setClientColorMode(String mode) {
      clientColorMode = mode;
      colorTransitionStart = System.currentTimeMillis();
   }

   public static int getClientColor() {
      return getClientColor(255);
   }

   public static int getClientColor(int alpha) {
      try {
         Theme theme = Theme.getInstance();
         if (theme != null) {
            return theme.getColorAt(0.0F, alpha);
         }
      } catch (Throwable var3) {
      }

      int colorCount = getActiveColorCount();
      if (colorCount == 0) {
         return rgba(clientColorR, clientColorG, clientColorB, alpha);
      } else if (colorCount == 1) {
         int color = clientColors[0];
         return rgba(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, alpha);
      } else {
         return clientColorMode.equals("Переливание") ? getGradientColor(alpha) : getStaticGradientColor(alpha);
      }
   }

   private static int getActiveColorCount() {
      int count = 0;

      for (int i = 0; i < clientColors.length; i++) {
         if (clientColors[i] != 0) {
            count = i + 1;
         }
      }

      return count;
   }

   private static int getGradientColor(int alpha) {
      int colorCount = getActiveColorCount();
      if (colorCount < 2) {
         int color = clientColors[0];
         return rgba(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, alpha);
      } else {
         return getClientColorAt(0.5F, alpha);
      }
   }

   public static int getClientColorAt(float x, int alpha) {
      try {
         Theme theme = Theme.getInstance();
         if (theme != null) {
            return theme.getColorAt(x, alpha);
         }
      } catch (Throwable var16) {
      }

      int colorCount = getActiveColorCount();
      if (colorCount == 0) {
         return getClientColor(alpha);
      }

      if (colorCount == 1) {
         int color = clientColors[0];
         return rgba(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, alpha);
      }

      if (clientColorMode.equals("Переливание")) {
         long currentTime = System.currentTimeMillis();
         float cycleDuration = 5000.0F;
         float progress = (float)((currentTime - colorTransitionStart) % (long)cycleDuration) / cycleDuration;
         float animatedX = x + progress;
         float wrappedX = animatedX - (int)animatedX;
         if (colorCount == 2) {
            int color1 = clientColors[0];
            int color2 = clientColors[1];
            return wrappedX < 0.5F
               ? interpolateColor(color1, color2, wrappedX * 2.0F, alpha)
               : interpolateColor(color2, color1, (wrappedX - 0.5F) * 2.0F, alpha);
         } else {
            float totalLength = colorCount;
            float normalizedX = wrappedX * totalLength;
            int segment = (int)normalizedX;
            float localX = normalizedX - segment;
            if (segment >= colorCount - 1) {
               int color1 = clientColors[colorCount - 1];
               int color2 = clientColors[0];
               float transitionX = normalizedX - (colorCount - 1);
               return interpolateColor(color1, color2, transitionX, alpha);
            } else {
               segment = Math.min(segment, colorCount - 2);
               int color1 = clientColors[segment];
               int color2 = clientColors[segment + 1];
               return interpolateColor(color1, color2, localX, alpha);
            }
         }
      } else {
         x = Math.max(0.0F, Math.min(1.0F, x));
         if (colorCount == 2) {
            int color1 = clientColors[0];
            int color2 = clientColors[1];
            return interpolateColor(color1, color2, x, alpha);
         } else {
            float segmentSize = 1.0F / (colorCount - 1);
            int segment = (int)(x / segmentSize);
            segment = Math.min(segment, colorCount - 2);
            float localX = (x - segment * segmentSize) / segmentSize;
            localX = Math.max(0.0F, Math.min(1.0F, localX));
            int color1 = clientColors[segment];
            int color2 = clientColors[segment + 1];
            return interpolateColor(color1, color2, localX, alpha);
         }
      }
   }

   private static int getStaticGradientColor(int alpha) {
      int colorCount = getActiveColorCount();
      if (colorCount < 2) {
         int color = clientColors[0];
         return rgba(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, alpha);
      } else {
         float x = 0.5F;
         if (colorCount == 2) {
            int color1 = clientColors[0];
            int color2 = clientColors[1];
            return interpolateColor(color1, color2, x, alpha);
         } else {
            float segmentSize = 1.0F / (colorCount - 1);
            int segment = (int)(x / segmentSize);
            segment = Math.min(segment, colorCount - 2);
            float localX = (x - segment * segmentSize) / segmentSize;
            localX = Math.max(0.0F, Math.min(1.0F, localX));
            int color1 = clientColors[segment];
            int color2 = clientColors[segment + 1];
            return interpolateColor(color1, color2, localX, alpha);
         }
      }
   }

   public static int multAlpha(int color, float percent01) {
      return rgba(getRed(color), getGreen(color), getBlue(color), Math.round(getAlpha(color) * percent01));
   }

   public static int getClientColorAt(float x) {
      return getClientColorAt(x, 255);
   }

   private static int interpolateColor(int color1, int color2, float amount, int alpha) {
      amount = Math.min(1.0F, Math.max(0.0F, amount));
      int r1 = getRed(color1);
      int g1 = getGreen(color1);
      int b1 = getBlue(color1);
      int r2 = getRed(color2);
      int g2 = getGreen(color2);
      int b2 = getBlue(color2);
      int r = (int)(r1 + (r2 - r1) * amount);
      int g = (int)(g1 + (g2 - g1) * amount);
      int b = (int)(b1 + (b2 - b1) * amount);
      return rgba(r, g, b, alpha);
   }

   public static void setClientColor(int r, int g, int b) {
      clientColorR = r;
      clientColorG = g;
      clientColorB = b;
   }

   public static void setClientColor(int color) {
      clientColorR = color >> 16 & 0xFF;
      clientColorG = color >> 8 & 0xFF;
      clientColorB = color & 0xFF;
   }

   public static int getClientColorDark() {
      int color = getClientColor();
      int r = (int)(getRed(color) * 0.4F);
      int g = (int)(getGreen(color) * 0.4F);
      int b = (int)(getBlue(color) * 0.4F);
      return rgba(r, g, b, getAlpha(color));
   }

   public static int getClientColorSoft(int alpha) {
      int color = getClientColor();
      float softness = 0.2F;
      int r = (int)(getRed(color) * softness);
      int g = (int)(getGreen(color) * softness);
      int b = (int)(getBlue(color) * softness);
      return rgba(r, g, b, alpha);
   }

   public static int[] getClientColorHorizontalGradient() {
      int colorCount = getActiveColorCount();
      if (colorCount == 0) {
         return horizontal(getClientColorDark(), getClientColor());
      }

      if (colorCount == 1) {
         int color = clientColors[0];
         int darkColor = rgba((int)((color >> 16 & 0xFF) * 0.4F), (int)((color >> 8 & 0xFF) * 0.4F), (int)((color & 0xFF) * 0.4F), 255);
         return horizontal(darkColor, color);
      }

      if (clientColorMode.equals("Переливание")) {
         long currentTime = System.currentTimeMillis();
         float cycleDuration = 5000.0F;
         float progress = (float)((currentTime - colorTransitionStart) % (long)cycleDuration) / cycleDuration;
         int[] gradient = new int[9];

         for (int i = 0; i < 9; i++) {
            float x = i / 8.0F;
            float animatedX = x + progress;
            float wrappedX = animatedX - (int)animatedX;
            if (colorCount == 2) {
               int color1 = clientColors[0];
               int color2 = clientColors[1];
               if (wrappedX < 0.5F) {
                  gradient[i] = interpolateColor(color1, color2, wrappedX * 2.0F, 255);
               } else {
                  gradient[i] = interpolateColor(color2, color1, (wrappedX - 0.5F) * 2.0F, 255);
               }
            } else {
               float totalLength = colorCount;
               float normalizedX = wrappedX * totalLength;
               int segment = (int)normalizedX;
               float localX = normalizedX - segment;
               if (segment >= colorCount - 1) {
                  int color1 = clientColors[colorCount - 1];
                  int color2 = clientColors[0];
                  float transitionX = normalizedX - (colorCount - 1);
                  gradient[i] = interpolateColor(color1, color2, transitionX, 255);
               } else {
                  segment = Math.min(segment, colorCount - 2);
                  int color1 = clientColors[segment];
                  int color2 = clientColors[segment + 1];
                  gradient[i] = interpolateColor(color1, color2, localX, 255);
               }
            }
         }

         return gradient;
      } else {
         if (colorCount == 2) {
            return horizontal(clientColors[0], clientColors[1]);
         }

         int color1 = clientColors[0];
         int color2 = clientColors[1];
         int color3 = clientColors[2];
         int mid1 = blend(color1, color2, 0.5F);
         int mid2 = blend(color2, color3, 0.5F);
         return new int[]{color1, mid1, color2, mid2, color3, mid2, color2, mid1};
      }
   }

   public static int rgba(int r, int g, int b, int a) {
      return a << 24 | r << 16 | g << 8 | b;
   }

   public static int rgba(int r, int g, int b, float a) {
      return rgba(r, g, b, (int)(a * 255.0F));
   }

   public static int rgb(int r, int g, int b) {
      return rgba(r, g, b, 255);
   }

   public static int[] solid(int color) {
      return new int[]{color, color, color, color, color, color, color, color, color};
   }

   public static int getRed(int hex) {
      return hex >> 16 & 0xFF;
   }

   public static int toColor(String hexColor) {
      int argb = Integer.parseInt(hexColor.substring(1), 16);
      return setAlpha(argb, 255);
   }

   public static int setAlpha(int color, int alpha) {
      return color & 16777215 | alpha << 24;
   }

   public static int colorForRectsCustom$() {
      return new Color(91, 63, 212, 255).getRGB();
   }

   public static int colorForRectsBlack$() {
      return new Color(26, 26, 26, 255).getRGB();
   }

   public static int colorForTextWhite$() {
      return new Color(255, 255, 255, 255).getRGB();
   }

   public static int colorForTextCustom$() {
      return new Color(130, 100, 210, 255).getRGB();
   }

   public static int getGreen(int hex) {
      return hex >> 8 & 0xFF;
   }

   public static int getBlue(int hex) {
      return hex & 0xFF;
   }

   public static int getAlpha(int hex) {
      return hex >> 24 & 0xFF;
   }

   public static float[] rgba(int color) {
      return new float[]{(color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, (color >> 24 & 0xFF) / 255.0F};
   }

   public static int interpolateColor(int color1, int color2, float amount) {
      amount = Math.min(1.0F, Math.max(0.0F, amount));
      int red1 = getRed(color1);
      int green1 = getGreen(color1);
      int blue1 = getBlue(color1);
      int alpha1 = getAlpha(color1);
      int red2 = getRed(color2);
      int green2 = getGreen(color2);
      int blue2 = getBlue(color2);
      int alpha2 = getAlpha(color2);
      int interpolatedRed = interpolateInt(red1, red2, amount);
      int interpolatedGreen = interpolateInt(green1, green2, amount);
      int interpolatedBlue = interpolateInt(blue1, blue2, amount);
      int interpolatedAlpha = interpolateInt(alpha1, alpha2, amount);
      return interpolatedAlpha << 24 | interpolatedRed << 16 | interpolatedGreen << 8 | interpolatedBlue;
   }

   public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
      return interpolateD(oldValue, newValue, (float)interpolationValue).intValue();
   }

   public static Double interpolateD(double oldValue, double newValue, double interpolationValue) {
      return oldValue + (newValue - oldValue) * interpolationValue;
   }

   public static int[] solid8(int color) {
      return new int[]{color, color, color, color, color, color, color, color};
   }

   public static int[] horizontal(int left, int right) {
      return new int[]{left, blend(left, right, 0.5F), right, left, blend(left, right, 0.5F), right, left, blend(left, right, 0.5F), right};
   }

   public static int astolfo(int speed, int index, float saturation, float brightness, float alpha) {
      float hueStep = 90.0F;
      float basaHuy = calculateHuyDegrees(speed, index);
      float huy = (basaHuy + index * hueStep) % 360.0F;
      huy /= 360.0F;
      saturation = Math.clamp(saturation, 0.0F, 1.0F);
      brightness = Math.clamp(brightness, 0.0F, 1.0F);
      int rgb = Color.HSBtoRGB(huy, saturation, brightness);
      int Ialpha = Math.max(0, Math.min(255, (int)(alpha * 255.0F)));
      return reAlphaInt(rgb, Ialpha);
   }

   public static int reAlphaInt(int color, int alpha) {
      return Math.clamp(alpha, 0, 255) << 24 | color & 16777215;
   }

   public static int calculateHuyDegrees(int divisor, int offset) {
      long currentTime = System.currentTimeMillis();
      long calculatedValue = (currentTime / divisor + offset) % 360L;
      return (int)calculatedValue;
   }

   public static int[] vertical(int top, int bottom) {
      return new int[]{top, top, top, blend(top, bottom, 0.5F), blend(top, bottom, 0.5F), blend(top, bottom, 0.5F), bottom, bottom, bottom};
   }

   public static int[] diagonal(int topLeft, int bottomRight) {
      int topRight = blend(topLeft, bottomRight, 0.5F);
      int bottomLeft = blend(topLeft, bottomRight, 0.5F);
      int center = blend(topLeft, bottomRight, 0.5F);
      return new int[]{
         topLeft,
         blend(topLeft, topRight, 0.5F),
         topRight,
         blend(topLeft, bottomLeft, 0.5F),
         center,
         blend(topRight, bottomRight, 0.5F),
         bottomLeft,
         blend(bottomLeft, bottomRight, 0.5F),
         bottomRight
      };
   }

   public static int[] corners(int topLeft, int topRight, int bottomLeft, int bottomRight) {
      int top = blend(topLeft, topRight, 0.5F);
      int bottom = blend(bottomLeft, bottomRight, 0.5F);
      int left = blend(topLeft, bottomLeft, 0.5F);
      int right = blend(topRight, bottomRight, 0.5F);
      int center = blend(blend(topLeft, bottomRight, 0.5F), blend(topRight, bottomLeft, 0.5F), 0.5F);
      return new int[]{topLeft, top, topRight, left, center, right, bottomLeft, bottom, bottomRight};
   }

   public static int[] full(int tl, int tc, int tr, int ml, int c, int mr, int bl, int bc, int br) {
      return new int[]{tl, tc, tr, ml, c, mr, bl, bc, br};
   }

   public static int[] horizontal8(int left, int right) {
      int mid = blend(left, right, 0.5F);
      return new int[]{left, mid, right, right, right, mid, left, left};
   }

   public static int[] vertical8(int top, int bottom) {
      int mid = blend(top, bottom, 0.5F);
      return new int[]{top, top, top, mid, bottom, bottom, bottom, mid};
   }

   public static int[] corners8(int topLeft, int topRight, int bottomRight, int bottomLeft) {
      int top = blend(topLeft, topRight, 0.5F);
      int right = blend(topRight, bottomRight, 0.5F);
      int bottom = blend(bottomRight, bottomLeft, 0.5F);
      int left = blend(bottomLeft, topLeft, 0.5F);
      return new int[]{topLeft, top, topRight, right, bottomRight, bottom, bottomLeft, left};
   }

   public static int[] rainbow8(int alpha) {
      return new int[]{
         rgba(255, 0, 0, alpha),
         rgba(255, 127, 0, alpha),
         rgba(255, 255, 0, alpha),
         rgba(0, 255, 0, alpha),
         rgba(0, 255, 255, alpha),
         rgba(0, 127, 255, alpha),
         rgba(0, 0, 255, alpha),
         rgba(127, 0, 255, alpha)
      };
   }

   public static float[] uniform8(float value) {
      return new float[]{value, value, value, value, value, value, value, value};
   }

   public static float[] varying8(float v1, float v2, float v3, float v4, float v5, float v6, float v7, float v8) {
      return new float[]{v1, v2, v3, v4, v5, v6, v7, v8};
   }

   public static int blend(int color1, int color2, float factor) {
      int a1 = color1 >> 24 & 0xFF;
      int r1 = color1 >> 16 & 0xFF;
      int g1 = color1 >> 8 & 0xFF;
      int b1 = color1 & 0xFF;
      int a2 = color2 >> 24 & 0xFF;
      int r2 = color2 >> 16 & 0xFF;
      int g2 = color2 >> 8 & 0xFF;
      int b2 = color2 & 0xFF;
      int a = (int)(a1 + (a2 - a1) * factor);
      int r = (int)(r1 + (r2 - r1) * factor);
      int g = (int)(g1 + (g2 - g1) * factor);
      int b = (int)(b1 + (b2 - b1) * factor);
      return a << 24 | r << 16 | g << 8 | b;
   }

   public static int withAlpha(int color, int alpha) {
      return alpha << 24 | color & 16777215;
   }

   public static int withAlpha(int color, float alpha) {
      return withAlpha(color, (int)(alpha * 255.0F));
   }

   public static int multiplyAlpha(int color, float factor) {
      int a = color >> 24 & 0xFF;
      int newAlpha = (int)(a * factor);
      newAlpha = Math.max(0, Math.min(255, newAlpha));
      return newAlpha << 24 | color & 16777215;
   }

   public static int multDark(int color, float percent01) {
      return getColor(Math.round(red(color) * percent01), Math.round(green(color) * percent01), Math.round(blue(color) * percent01), alpha(color));
   }

   public static String removeFormatting(String text) {
      return text != null && !text.isEmpty() ? FORMATTING_CODE_PATTERN.matcher(text).replaceAll("") : null;
   }

   public static String normalize(String text) {
      if (text != null && !text.isEmpty()) {
         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
               case 'ǫ':
                  c = 'Q';
                  break;
               case 'ɢ':
                  c = 'G';
                  break;
               case 'ɪ':
                  c = 'I';
                  break;
               case 'ɴ':
                  c = 'N';
                  break;
               case 'ʀ':
                  c = 'R';
                  break;
               case 'ʏ':
                  c = 'Y';
                  break;
               case 'ʙ':
                  c = 'B';
                  break;
               case 'ʜ':
                  c = 'H';
                  break;
               case 'ʟ':
                  c = 'L';
                  break;
               case 'ᴀ':
                  c = 'A';
                  break;
               case 'ᴄ':
                  c = 'C';
                  break;
               case 'ᴅ':
                  c = 'D';
                  break;
               case 'ᴇ':
                  c = 'E';
                  break;
               case 'ᴊ':
                  c = 'J';
                  break;
               case 'ᴋ':
                  c = 'K';
                  break;
               case 'ᴍ':
                  c = 'M';
                  break;
               case 'ᴏ':
                  c = 'O';
                  break;
               case 'ᴘ':
                  c = 'P';
                  break;
               case 'ᴛ':
                  c = 'T';
                  break;
               case 'ᴜ':
                  c = 'U';
                  break;
               case 'ᴠ':
                  c = 'V';
                  break;
               case 'ᴡ':
                  c = 'W';
                  break;
               case 'ᴢ':
                  c = 'Z';
                  break;
               case 'ꜰ':
                  c = 'F';
                  break;
               case 'ꜱ':
                  c = 'S';
            }

            sb.append(c);
         }

         return sb.toString();
      } else {
         return text;
      }
   }

   public static String toLegacyText(Text text) {
      if (text == null) {
         return null;
      }

      StringBuilder sb = new StringBuilder();
      text.visit((style, string) -> {
         if (style != null && style.getColor() != null) {
            sb.append("§#").append(String.format("%06x", style.getColor().getRgb() & 16777215));
         }

         if (style != null) {
            if (style.isBold()) {
               sb.append("§l");
            }

            if (style.isItalic()) {
               sb.append("§o");
            }

            if (style.isUnderlined()) {
               sb.append("§n");
            }

            if (style.isStrikethrough()) {
               sb.append("§m");
            }

            if (style.isObfuscated()) {
               sb.append("§k");
            }
         }

         sb.append(string);
         return Optional.empty();
      }, Style.EMPTY);
      return sb.toString();
   }

   public static int interpolate(int start, int end, float value) {
      float[] startColor = rgba(start);
      float[] endColor = rgba(end);
      return rgba(
         (int)MathUtils.interpolate(startColor[0] * 255.0F, endColor[0] * 255.0F, value),
         (int)MathUtils.interpolate(startColor[1] * 255.0F, endColor[1] * 255.0F, value),
         (int)MathUtils.interpolate(startColor[2] * 255.0F, endColor[2] * 255.0F, value),
         (int)MathUtils.interpolate(startColor[3] * 255.0F, endColor[3] * 255.0F, value)
      );
   }
}
