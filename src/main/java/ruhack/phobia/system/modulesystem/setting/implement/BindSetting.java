package ruhack.phobia.system.modulesystem.setting.implement;

import java.util.function.Supplier;
import lombok.Generated;
import net.minecraft.client.util.InputUtil.Type;
import ruhack.phobia.system.modulesystem.setting.Setting;
import ruhack.phobia.util.string.KeyHelper;

public class BindSetting extends Setting {
   private int key = -1;
   private int type = 1;
   private boolean mouse = false;

   public BindSetting(String name, String description) {
      super(name, description);
   }

   public void setKeyBind(int key, boolean isMouse) {
      this.key = key;
      this.mouse = isMouse;
   }

   public void reset() {
      this.key = -1;
      this.mouse = false;
   }

   public boolean isPressed(int eventKey, Type eventType) {
      if (this.key == -1) {
         return false;
      } else {
         return this.mouse ? eventType == Type.MOUSE && eventKey == this.key : eventType == Type.KEYSYM && eventKey == this.key;
      }
   }

   public String getKeyDisplayName() {
      if (this.key == -1) {
         return "None";
      }

      if (this.mouse) {
         return switch (this.key) {
            case 2 -> "MMB";
            case 3 -> "Mouse 4";
            case 4 -> "Mouse 5";
            case 5 -> "Mouse 6";
            case 6 -> "Mouse 7";
            case 7 -> "Mouse 8";
            default -> "Mouse " + (this.key + 1);
         };
      } else {
         return KeyHelper.getKeyName(this.key);
      }
   }

   public BindSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
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
   public boolean isMouse() {
      return this.mouse;
   }

   @Generated
   public BindSetting setKey(int key) {
      this.key = key;
      return this;
   }

   @Generated
   public BindSetting setType(int type) {
      this.type = type;
      return this;
   }

   @Generated
   public BindSetting setMouse(boolean mouse) {
      this.mouse = mouse;
      return this;
   }
}
