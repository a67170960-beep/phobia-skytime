package ruhack.phobia.system.modulesystem.impl.rage;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.BoundingBoxControlEvent;
import ruhack.phobia.system.friendsystem.FriendUtils;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;

public class HitBox extends ModuleStructure {
   private final SliderSettings xzExpandSetting = new SliderSettings("Расширение XZ", "Позволяет расширить хитбокс по осям XZ", 0.2F).range(0.0F, 3.0F);
   private final SliderSettings yExpandSetting = new SliderSettings("Расширение Y", "Позволяет расширить хитбокс по оси Y", 0.0F).range(0.0F, 3.0F);

   public HitBox() {
      super("HitBox", "Расширяет хитбоксы сущностей", ModuleCategory.RAGE);
      this.settings(this.xzExpandSetting, this.yExpandSetting);
   }

   @EventHandler
   public void onBoundingBoxControl(BoundingBoxControlEvent event) {
      if (event.getEntity() instanceof LivingEntity living) {
         Box box = event.getBox();
         float xzExpand = this.xzExpandSetting.getValue();
         float yExpand = this.yExpandSetting.getValue();
         Box changedBox = new Box(
            box.minX - xzExpand / 2.0F,
            box.minY - yExpand / 2.0F,
            box.minZ - xzExpand / 2.0F,
            box.maxX + xzExpand / 2.0F,
            box.maxY + yExpand / 2.0F,
            box.maxZ + xzExpand / 2.0F
         );
         if (living != mc.player && !FriendUtils.isFriend(living)) {
            event.setBox(changedBox);
         }
      }
   }
}
