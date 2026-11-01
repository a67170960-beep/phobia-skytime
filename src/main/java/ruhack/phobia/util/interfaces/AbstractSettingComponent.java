package ruhack.phobia.util.interfaces;

import java.awt.Color;
import lombok.Generated;
import ruhack.phobia.system.modulesystem.setting.Setting;

public abstract class AbstractSettingComponent extends AbstractComponent {
   private final Setting setting;
   protected float alphaMultiplier = 1.0F;

   public void setAlphaMultiplier(float alpha) {
      this.alphaMultiplier = alpha;
   }

   protected int applyAlpha(int color, float extraAlpha) {
      int a = color >> 24 & 0xFF;
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      int newAlpha = Math.max(0, Math.min(255, (int)(a * this.alphaMultiplier * extraAlpha)));
      return newAlpha << 24 | r << 16 | g << 8 | b;
   }

   protected int applyAlpha(int color) {
      return this.applyAlpha(color, 1.0F);
   }

   protected Color applyAlpha(Color color) {
      int newAlpha = Math.max(0, Math.min(255, (int)(color.getAlpha() * this.alphaMultiplier)));
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), newAlpha);
   }

   protected Color applyAlpha(Color color, float extraAlpha) {
      int newAlpha = Math.max(0, Math.min(255, (int)(color.getAlpha() * this.alphaMultiplier * extraAlpha)));
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), newAlpha);
   }

   @Generated
   public Setting getSetting() {
      return this.setting;
   }

   @Generated
   public float getAlphaMultiplier() {
      return this.alphaMultiplier;
   }

   @Generated
   public AbstractSettingComponent(Setting setting) {
      this.setting = setting;
   }
}
