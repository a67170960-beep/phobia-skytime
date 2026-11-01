package ruhack.phobia.util.render;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import ruhack.phobia.util.colorschem.ColorUtil;

public final class GradientUtil {
   public static final int THEME_SPEED = 18;

   private GradientUtil() {
   }

   public static MutableText gradient(String text, int startColor, int endColor) {
      MutableText component = Text.empty();
      if (text != null && !text.isEmpty()) {
         int length = text.length();

         for (int i = 0; i < length; i++) {
            float ratio = length == 1 ? 0.0F : (float)i / (length - 1);
            int color = ColorUtil.interpolate(startColor, endColor, ratio);
            component.append(Text.literal(String.valueOf(text.charAt(i))).styled(style -> style.withColor(color)));
         }

         return component;
      } else {
         return component;
      }
   }

   public static MutableText gradient(String text, int startColor, int endColor, int speed) {
      return gradient(text, speed, 1.0F, startColor, endColor);
   }

   public static MutableText themeGradient(String text, float ratio, int... colors) {
      return gradient(text, 18, ratio, colors);
   }

   public static MutableText gradient(String text, int startColor, int endColor, int speed, float ratio) {
      return gradient(text, speed, ratio, startColor, endColor);
   }

   public static MutableText gradient(String text, int speed, float ratio, int... colors) {
      MutableText component = Text.empty();
      if (text != null && !text.isEmpty()) {
         for (int i = 0; i < text.length(); i++) {
            int index = (int)(i * ratio);
            int color = color(speed, index, colors);
            component.append(Text.literal(String.valueOf(text.charAt(i))).styled(style -> style.withColor(color)));
         }

         return component;
      } else {
         return component;
      }
   }

   public static int color(int speed, int index, int... colors) {
      if (colors != null && colors.length != 0) {
         if (colors.length == 1) {
            return colors[0];
         }

         int safeSpeed = Math.max(1, speed);
         int angle = (int)((System.currentTimeMillis() / safeSpeed + index) % 360L);
         if (angle < 0) {
            angle += 360;
         }

         angle = (angle > 180 ? 360 - angle : angle) + 180;
         float scaled = angle / 360.0F * colors.length;
         int colorIndex = Math.min(colors.length - 1, (int)scaled);
         int nextIndex = colorIndex == colors.length - 1 ? 0 : colorIndex + 1;
         return ColorUtil.interpolate(colors[colorIndex], colors[nextIndex], scaled - colorIndex);
      } else {
         return -1;
      }
   }
}
