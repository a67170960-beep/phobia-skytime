package ruhack.phobia.system.modulesystem.impl.render;

import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.istance.Instance;

public class AspectRatio extends ModuleStructure {
   public final SelectSetting mode = new SelectSetting("Соотношение", "Формат изображения", "Кастомное", "4:3", "16:9", "1:1", "16:10", "Кастомное");
   public final SliderSettings custom = new SliderSettings("Значение", "Собственное соотношение сторон", 1.0F)
      .range(0.1F, 5.0F)
      .step(0.1F)
      .visible(() -> this.mode.isSelected("Кастомное"));

   public AspectRatio() {
      super("AspectRatio", "Изменяет соотношение сторон перспективы", ModuleCategory.RENDER);
      this.settings(this.mode, this.custom);
   }

   public static AspectRatio getInstance() {
      return Instance.get(AspectRatio.class);
   }

   public float getRatio() {
      return switch (this.mode.getValue()) {
         case "4:3" -> 1.3333334F;
         case "16:9" -> 1.7777778F;
         case "1:1" -> 1.0F;
         case "16:10" -> 1.6F;
         default -> this.custom.getValue();
      };
   }
}
