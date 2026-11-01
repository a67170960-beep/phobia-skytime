package ruhack.phobia.system.modulesystem.setting.implement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import lombok.Generated;
import ruhack.phobia.system.modulesystem.setting.Setting;

public class SelectSetting extends Setting {
   private String value;
   private List<String> list;

   public SelectSetting(String name, String description, String defaultValue, String... values) {
      super(name, description);
      this.list = new ArrayList<>(Arrays.asList(values));
      this.value = defaultValue;
      if (!this.list.contains(defaultValue)) {
         this.list.add(0, defaultValue);
      }
   }

   public SelectSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public SelectSetting selected(String string) {
      if (this.list.contains(string)) {
         this.value = string;
      }

      return this;
   }

   public boolean isSelected(String name) {
      return this.value != null && this.value.equals(name);
   }

   @Generated
   public String getValue() {
      return this.value;
   }

   @Generated
   public List<String> getList() {
      return this.list;
   }

   @Generated
   public void setValue(String value) {
      this.value = value;
   }
}
