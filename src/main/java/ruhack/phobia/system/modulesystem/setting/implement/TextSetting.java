package ruhack.phobia.system.modulesystem.setting.implement;

import java.util.function.Supplier;
import lombok.Generated;
import ruhack.phobia.system.modulesystem.setting.Setting;

public class TextSetting extends Setting {
   private String value;
   private int min;
   private int max;

   public TextSetting(String name, String description, String defaultValue) {
      super(name, description);
      this.value = defaultValue;
   }

   public String getValue() {
      return this.value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public TextSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   @Generated
   public int getMin() {
      return this.min;
   }

   @Generated
   public int getMax() {
      return this.max;
   }

   @Generated
   public void setMin(int min) {
      this.min = min;
   }

   @Generated
   public void setMax(int max) {
      this.max = max;
   }
}
