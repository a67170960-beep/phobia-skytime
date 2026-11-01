package ruhack.phobia.system.modulesystem.setting.implement;

import java.awt.Color;
import java.util.function.Supplier;
import lombok.Generated;
import ruhack.phobia.system.modulesystem.setting.Setting;

public class ColorSetting extends Setting {
   private float hue = 0.0F;
   private float saturation = 1.0F;
   private float brightness = 1.0F;
   private float alpha = 1.0F;
   private int[] presets = new int[0];
   private int[] previousColors = new int[8];
   private int previousColorsCount = 0;

   public ColorSetting(String name, String description) {
      super(name, description);
   }

   public ColorSetting value(int value) {
      this.setColor(value);
      return this;
   }

   public ColorSetting presets(int... presets) {
      this.presets = presets;
      return this;
   }

   public ColorSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public int getColor() {
      int rgb = Color.HSBtoRGB(this.hue, this.saturation, this.brightness);
      int alphaInt = Math.round(this.alpha * 255.0F);
      return alphaInt << 24 | rgb & 16777215;
   }

   public int getColorWithAlpha() {
      return this.getColor();
   }

   public int getColorNoAlpha() {
      return Color.HSBtoRGB(this.hue, this.saturation, this.brightness) | 0xFF000000;
   }

   public ColorSetting setColor(int color) {
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      int a = color >> 24 & 0xFF;
      float[] hsb = Color.RGBtoHSB(r, g, b, null);
      this.hue = hsb[0];
      this.saturation = hsb[1];
      this.brightness = hsb[2];
      this.alpha = a / 255.0F;
      return this;
   }

   public Color getAwtColor() {
      int color = this.getColor();
      return new Color(color, true);
   }

   public ColorSetting setHue(float hue) {
      this.hue = Math.max(0.0F, Math.min(1.0F, hue));
      return this;
   }

   public ColorSetting setSaturation(float saturation) {
      this.saturation = Math.max(0.0F, Math.min(1.0F, saturation));
      return this;
   }

   public ColorSetting setBrightness(float brightness) {
      this.brightness = Math.max(0.0F, Math.min(1.0F, brightness));
      return this;
   }

   public ColorSetting setAlpha(float alpha) {
      this.alpha = Math.max(0.0F, Math.min(1.0F, alpha));
      return this;
   }

   public void addToPreviousColors() {
      int currentColor = this.getColorNoAlpha();

      for (int i = 0; i < this.previousColorsCount; i++) {
         if (this.previousColors[i] == currentColor) {
            return;
         }
      }

      if (this.previousColorsCount < 8) {
         System.arraycopy(this.previousColors, 0, this.previousColors, 1, this.previousColorsCount);
         this.previousColorsCount++;
      } else {
         System.arraycopy(this.previousColors, 0, this.previousColors, 1, 7);
      }

      this.previousColors[0] = currentColor;
   }

   public String getHexString() {
      int color = this.getColorNoAlpha();
      return String.format("#%06X", color & 16777215);
   }

   @Generated
   public float getHue() {
      return this.hue;
   }

   @Generated
   public float getSaturation() {
      return this.saturation;
   }

   @Generated
   public float getBrightness() {
      return this.brightness;
   }

   @Generated
   public float getAlpha() {
      return this.alpha;
   }

   @Generated
   public int[] getPresets() {
      return this.presets;
   }

   @Generated
   public int[] getPreviousColors() {
      return this.previousColors;
   }

   @Generated
   public int getPreviousColorsCount() {
      return this.previousColorsCount;
   }

   @Generated
   public void setPresets(int[] presets) {
      this.presets = presets;
   }

   @Generated
   public void setPreviousColors(int[] previousColors) {
      this.previousColors = previousColors;
   }

   @Generated
   public void setPreviousColorsCount(int previousColorsCount) {
      this.previousColorsCount = previousColorsCount;
   }
}
