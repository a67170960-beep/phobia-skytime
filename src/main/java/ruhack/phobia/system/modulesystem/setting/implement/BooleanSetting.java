package ruhack.phobia.system.modulesystem.setting.implement;

import java.util.function.Supplier;
import lombok.Generated;
import ruhack.phobia.system.modulesystem.setting.Setting;

public class BooleanSetting extends Setting {
   private boolean value;
   private int key = -1;
   private int type = 1;

   public BooleanSetting(String name, String description) {
      super(name, description);
   }

   public BooleanSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   @Generated
   public boolean isValue() {
      return this.value;
   }

   @Generated
   public int getKey() {
      return this.key;
   }

   @Generated
   public int getType() {
      return this.type;
   }

   @Generated
   public BooleanSetting setValue(boolean value) {
      this.value = value;
      return this;
   }

   @Generated
   public BooleanSetting setKey(int key) {
      this.key = key;
      return this;
   }

   @Generated
   public BooleanSetting setType(int type) {
      this.type = type;
      return this;
   }
}
