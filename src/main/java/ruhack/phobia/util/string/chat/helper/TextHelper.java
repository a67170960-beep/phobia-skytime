package ruhack.phobia.util.string.chat.helper;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ruhack.phobia.system.modulesystem.impl.other.Theme;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.render.GradientUtil;

public class TextHelper {
   public static MutableText legacy(String text) {
      MutableText result = Text.empty();
      if (text != null && !text.isEmpty()) {
         Style style = Style.EMPTY;
         StringBuilder part = new StringBuilder();

         for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == 167 && i + 1 < text.length()) {
               if (!part.isEmpty()) {
                  result.append(Text.literal(part.toString()).setStyle(style));
                  part.setLength(0);
               }

               Formatting formatting = Formatting.byCode(text.charAt(++i));
               if (formatting == Formatting.RESET) {
                  style = Style.EMPTY;
               } else if (formatting != null) {
                  style = style.withFormatting(formatting);
               }
            } else {
               part.append(current);
            }
         }

         if (!part.isEmpty()) {
            result.append(Text.literal(part.toString()).setStyle(style));
         }

         return result;
      } else {
         return result;
      }
   }

   public static Text applyGradient(String text, TextHelper.GradientStyle style, int color1, int color2, boolean bold) {
      switch (style) {
         case HALF_SPLIT:
            return halfSplitGradient(text, color1, color2, bold);
         case FULL_GRADIENT:
            return fullGradient(text, color1, color2, bold);
         case ASTOLFO:
            return astolfoGradient(text, bold);
         case TWO_COLOR_FADE:
            return twoColorFade(text, color1, color2, bold);
         default:
            return Text.literal(text).styled(s -> s.withColor(color1).withBold(bold));
      }
   }

   public static Text applyThemeGradient(String text, boolean bold) {
      Theme theme = Theme.getInstance();
      int[] colors = theme == null ? new int[]{ColorUtil.getClientColor()} : theme.selectedPalette().colors();
      return GradientUtil.themeGradient(text, 45.0F, colors).styled(style -> style.withBold(bold));
   }

   private static Text halfSplitGradient(String text, int color1, int color2, boolean bold) {
      MutableText result = Text.literal("");
      int midPoint = text.length() / 2;

      for (int i = 0; i < text.length(); i++) {
         int color = i < midPoint ? color1 : color2;
         result.append(Text.literal(String.valueOf(text.charAt(i))).styled(s -> s.withColor(color).withBold(bold)));
      }

      return result;
   }

   private static Text fullGradient(String text, int color1, int color2, boolean bold) {
      MutableText result = Text.literal("");

      for (int i = 0; i < text.length(); i++) {
         float ratio = (float)i / (text.length() - 1);
         int color = ColorUtil.interpolate(color1, color2, ratio);
         result.append(Text.literal(String.valueOf(text.charAt(i))).styled(s -> s.withColor(color).withBold(bold)));
      }

      return result;
   }

   private static Text astolfoGradient(String text, boolean bold) {
      MutableText result = Text.literal("");

      for (int i = 0; i < text.length(); i++) {
         int color = ColorUtil.astolfo(10, i, 0.7F, 0.7F, 1.0F);
         result.append(Text.literal(String.valueOf(text.charAt(i))).styled(s -> s.withColor(color).withBold(bold)));
      }

      return result;
   }

   private static Text twoColorFade(String text, int color1, int color2, boolean bold) {
      MutableText result = Text.literal("");

      for (int i = 0; i < text.length(); i++) {
         float ratio = (float)i / (text.length() - 1);
         int color = ColorUtil.interpolateColor(color1, color2, ratio);
         result.append(Text.literal(String.valueOf(text.charAt(i))).styled(s -> s.withColor(color).withBold(bold)));
      }

      return result;
   }

   public static Text applyPredefinedGradient(String text, String gradientName, boolean bold) {
      switch (gradientName.toLowerCase()) {
         case "red_blue":
            return applyGradient(text, TextHelper.GradientStyle.HALF_SPLIT, ColorUtil.red, ColorUtil.toColor("#0000FF"), bold);
         case "green_purple":
            return applyGradient(text, TextHelper.GradientStyle.HALF_SPLIT, ColorUtil.green, ColorUtil.toColor("#800080"), bold);
         case "yellow_cyan":
            return applyGradient(text, TextHelper.GradientStyle.FULL_GRADIENT, ColorUtil.yellow, ColorUtil.toColor("#00FFFF"), bold);
         case "orange_magenta":
            return applyGradient(text, TextHelper.GradientStyle.FULL_GRADIENT, ColorUtil.orange, ColorUtil.toColor("#FF00FF"), bold);
         case "astolfo":
            return applyGradient(text, TextHelper.GradientStyle.ASTOLFO, 0, 0, bold);
         case "blue_green_fade":
            return applyGradient(text, TextHelper.GradientStyle.TWO_COLOR_FADE, ColorUtil.toColor("#0000FF"), ColorUtil.green, bold);
         case "purple_red_fade":
            return applyGradient(text, TextHelper.GradientStyle.TWO_COLOR_FADE, ColorUtil.toColor("#800080"), ColorUtil.red, bold);
         case "cyan_orange_fade":
            return applyGradient(text, TextHelper.GradientStyle.TWO_COLOR_FADE, ColorUtil.toColor("#00FFFF"), ColorUtil.orange, bold);
         case "white_black":
            return applyGradient(text, TextHelper.GradientStyle.FULL_GRADIENT, ColorUtil.colorForTextWhite$(), ColorUtil.colorForRectsBlack$(), bold);
         case "custom_purple":
            return applyGradient(text, TextHelper.GradientStyle.FULL_GRADIENT, ColorUtil.colorForTextCustom$(), ColorUtil.colorForRectsCustom$(), bold);
         case "black_light_purple":
            return applyGradient(text, TextHelper.GradientStyle.FULL_GRADIENT, ColorUtil.colorForRectsBlack$(), ColorUtil.toColor("#DA70D6"), bold);
         case "dark_red_bright_red":
            return applyGradient(text, TextHelper.GradientStyle.FULL_GRADIENT, ColorUtil.toColor("#8B0000"), ColorUtil.red, bold);
         case "dark_red":
            return applyGradient(text, TextHelper.GradientStyle.HALF_SPLIT, ColorUtil.toColor("#8B0000"), ColorUtil.toColor("#8B0000"), bold);
         case "red_white":
            return applyGradient(text, TextHelper.GradientStyle.HALF_SPLIT, ColorUtil.red, ColorUtil.colorForTextWhite$(), bold);
         case "purple_bright_pink":
            return applyGradient(text, TextHelper.GradientStyle.FULL_GRADIENT, ColorUtil.toColor("#800080"), ColorUtil.toColor("#FF69B4"), bold);
         case "pink_dark_pink":
            return applyGradient(text, TextHelper.GradientStyle.FULL_GRADIENT, ColorUtil.toColor("#FFC1CC"), ColorUtil.toColor("#C71585"), bold);
         case "bright_red":
            return applyGradient(text, TextHelper.GradientStyle.HALF_SPLIT, ColorUtil.red, ColorUtil.red, bold);
         case "dark_green_bright_green":
            return applyGradient(text, TextHelper.GradientStyle.FULL_GRADIENT, ColorUtil.toColor("#006400"), ColorUtil.green, bold);
         case "red_orange":
            return applyGradient(text, TextHelper.GradientStyle.FULL_GRADIENT, ColorUtil.red, ColorUtil.orange, bold);
         case "turquoise_blue":
            return applyGradient(text, TextHelper.GradientStyle.FULL_GRADIENT, ColorUtil.toColor("#40E0D0"), ColorUtil.toColor("#0000FF"), bold);
         default:
            return Text.literal(text).styled(s -> s.withColor(ColorUtil.colorForTextWhite$()).withBold(bold));
      }
   }

   public enum GradientStyle {
      HALF_SPLIT,
      FULL_GRADIENT,
      ASTOLFO,
      TWO_COLOR_FADE;
   }
}
