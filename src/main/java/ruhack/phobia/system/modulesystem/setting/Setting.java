package ruhack.phobia.system.modulesystem.setting;

import java.util.function.Supplier;
import lombok.Generated;

public class Setting {
   private final String name;
   private String description;
   private Supplier<Boolean> visible;

   public Setting(String name) {
      this.name = name;
   }

   public Setting(String name, String description) {
      this.name = name;
      this.description = description;
   }

   public boolean isVisible() {
      return this.visible == null || this.visible.get();
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public String getDescription() {
      return this.description;
   }

   @Generated
   public Supplier<Boolean> getVisible() {
      return this.visible;
   }

   @Generated
   public void setVisible(Supplier<Boolean> visible) {
      this.visible = visible;
   }
}
