package ruhack.phobia.system.modulesystem.setting.implement;

import java.util.function.Supplier;
import lombok.Generated;
import ruhack.phobia.system.modulesystem.setting.Setting;

public class SliderSettings extends Setting {
   private float value;
   private float min;
   private float max;
   private float step = 0.1F;
   private String suffix = "";
   private boolean integer;

   public SliderSettings(String name, String description, float value) {
      super(name, description);
      this.value = value;
      this.min = value;
      this.max = value;
   }

   public void setValue(float value) {
      this.value = Math.max(this.min, Math.min(this.max, value));
   }

   public SliderSettings range(float min, float max) {
      this.min = min;
      this.max = max;
      this.value = Math.max(min, Math.min(max, this.value));
      return this;
   }

   public SliderSettings range(int min, int max) {
      this.min = min;
      this.max = max;
      this.value = Math.max(min, Math.min(max, this.value));
      this.integer = true;
      this.step = 1.0F;
      return this;
   }

   public SliderSettings suffix(String suffix) {
      this.suffix = suffix;
      return this;
   }

   public SliderSettings step(float step) {
      this.step = step;
      return this;
   }

   public int getInt() {
      return (int)this.value;
   }

   public SliderSettings visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   @Generated
   public float getValue() {
      return this.value;
   }

   @Generated
   public float getMin() {
      return this.min;
   }

   @Generated
   public float getMax() {
      return this.max;
   }

   @Generated
   public float getStep() {
      return this.step;
   }

   @Generated
   public String getSuffix() {
      return this.suffix;
   }

   @Generated
   public boolean isInteger() {
      return this.integer;
   }

   @Generated
   public SliderSettings setMin(float min) {
      this.min = min;
      return this;
   }

   @Generated
   public SliderSettings setMax(float max) {
      this.max = max;
      return this;
   }

   @Generated
   public SliderSettings setStep(float step) {
      this.step = step;
      return this;
   }

   @Generated
   public SliderSettings setSuffix(String suffix) {
      this.suffix = suffix;
      return this;
   }

   @Generated
   public SliderSettings setInteger(boolean integer) {
      this.integer = integer;
      return this;
   }
}
