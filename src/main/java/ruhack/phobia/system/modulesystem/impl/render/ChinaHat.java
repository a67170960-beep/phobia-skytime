package ruhack.phobia.system.modulesystem.impl.render;

import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.ColorSetting;
import ruhack.phobia.util.colorschem.ColorUtil;

public class ChinaHat extends ModuleStructure {
   private static ChinaHat instance;
   public final ColorSetting color1 = new ColorSetting("Color 1", "First gradient color").value(ColorUtil.getColor(255, 50, 100, 255));
   public final ColorSetting color2 = new ColorSetting("Color 2", "Second gradient color").value(ColorUtil.getColor(100, 50, 255, 255));

   public static ChinaHat getInstance() {
      return instance;
   }

   public ChinaHat() {
      super("ChinaHat", "Надевает на игрока китайскую шляпу", ModuleCategory.RENDER);
      instance = this;
      this.settings(this.color1, this.color2);
   }
}
