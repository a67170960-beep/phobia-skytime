package ruhack.phobia.system.modulesystem.impl.render;

import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.util.istance.Instance;

public class NoRender extends ModuleStructure {
   public final MultiSelectSetting modeSetting = new MultiSelectSetting("Элементы", "Выберите элементы для игнорирования")
      .value("Fire", "Bad Effects", "Block Overlay", "Darkness", "Damage", "Nausea", "Scoreboard", "BossBar")
      .selected("Fire", "Bad Effects", "Block Overlay", "Darkness", "Damage", "Nausea");

   public static NoRender getInstance() {
      return Instance.get(NoRender.class);
   }

   public NoRender() {
      super("NoRender", "No Render", ModuleCategory.RENDER);
      this.settings(this.modeSetting);
   }
}
