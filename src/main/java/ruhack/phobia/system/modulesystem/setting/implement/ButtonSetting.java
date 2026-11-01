package ruhack.phobia.system.modulesystem.setting.implement;

import java.util.function.Supplier;
import lombok.Generated;
import ruhack.phobia.system.modulesystem.setting.Setting;

public class ButtonSetting extends Setting {
   private Runnable runnable;
   private String buttonName;

   public ButtonSetting(String name, String description) {
      super(name, description);
   }

   public ButtonSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   @Generated
   public Runnable getRunnable() {
      return this.runnable;
   }

   @Generated
   public String getButtonName() {
      return this.buttonName;
   }

   @Generated
   public ButtonSetting setRunnable(Runnable runnable) {
      this.runnable = runnable;
      return this;
   }

   @Generated
   public ButtonSetting setButtonName(String buttonName) {
      this.buttonName = buttonName;
      return this;
   }
}
