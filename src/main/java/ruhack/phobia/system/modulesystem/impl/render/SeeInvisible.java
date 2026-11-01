package ruhack.phobia.system.modulesystem.impl.render;

import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.EntityColorEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.colorschem.ColorUtil;

public class SeeInvisible extends ModuleStructure {
   private final SliderSettings alphaSetting = new SliderSettings("Прозрачность", "Прозрачность игрока", 0.5F).range(0.1F, 1.0F);

   public SeeInvisible() {
      super("SeeInvisible", "Показывает неведимых игроков", ModuleCategory.RENDER);
      this.settings(this.alphaSetting);
   }

   @EventHandler
   public void onEntityColor(EntityColorEvent e) {
      e.setColor(ColorUtil.multAlpha(e.getColor(), this.alphaSetting.getValue()));
      e.cancel();
   }
}
