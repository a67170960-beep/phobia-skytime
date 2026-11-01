package ruhack.phobia.system.modulesystem.impl.render;

import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.util.istance.Instance;

public class Wings extends ModuleStructure {
   public final BooleanSetting animation = new BooleanSetting("Анимация крыльев", "Плавное движение крыльев").setValue(true);
   public final MultiSelectSetting targets = new MultiSelectSetting("Отображать", "На ком рисовать крылья")
      .value("Игроки", "Друзья", "Себя")
      .selected("Игроки", "Друзья");

   public Wings() {
      super("Wings", "Рисует тематические крылья за спиной игроков", ModuleCategory.RENDER);
      this.settings(this.animation, this.targets);
   }

   public static Wings getInstance() {
      return Instance.get(Wings.class);
   }
}
