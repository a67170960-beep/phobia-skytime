package ruhack.phobia.system.modulesystem.impl.render;

import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;

public class Boobs extends ModuleStructure {
   private static Boobs instance;
   public final SelectSetting display = new SelectSetting("Отображать", "На каких игроках отображать модель", "На себе", "На себе", "На всех");
   public final SliderSettings size = new SliderSettings("Размер", "Размер модели груди", 60.0F).range(0.0F, 100.0F).step(1.0F).suffix("%");
   public final SliderSettings xOffset = new SliderSettings("Положение X", "Горизонтальное смещение", 0.0F).range(-1.0F, 1.0F).step(0.05F);
   public final SliderSettings yOffset = new SliderSettings("Положение Y", "Вертикальное смещение", -0.2F).range(-1.0F, 1.0F).step(0.05F);
   public final SliderSettings zOffset = new SliderSettings("Положение Z", "Смещение модели вперёд", 0.0F).range(-1.0F, 1.0F).step(0.05F);
   public final SliderSettings cleavage = new SliderSettings("Расстояние", "Расстояние между сторонами", 0.05F).range(0.0F, 1.0F).step(0.05F);
   public final BooleanSetting uniboob = new BooleanSetting("Объединённая форма", "Соединяет обе стороны в одну форму").setValue(false);
   public final BooleanSetting physics = new BooleanSetting("Физика", "Добавляет реакцию на движение и прыжки").setValue(true);
   public final SliderSettings bounce = new SliderSettings("Интенсивность", "Сила физической анимации", 0.33F)
      .range(0.0F, 1.0F)
      .step(0.01F)
      .visible(this.physics::isValue);
   public final SliderSettings floppy = new SliderSettings("Инерция", "Плавность и инерция движения", 0.75F)
      .range(0.0F, 1.0F)
      .step(0.01F)
      .visible(this.physics::isValue);
   public final BooleanSetting breathing = new BooleanSetting("Дыхание", "Добавляет спокойную анимацию дыхания").setValue(true);
   public final BooleanSetting showInArmor = new BooleanSetting("Показывать в броне", "Не скрывать модель под нагрудником").setValue(true);

   public Boobs() {
      super("Boobs", "Добавляет настраиваемую модель груди игроку", ModuleCategory.RENDER);
      instance = this;
      this.settings(
         this.display,
         this.size,
         this.xOffset,
         this.yOffset,
         this.zOffset,
         this.cleavage,
         this.uniboob,
         this.physics,
         this.bounce,
         this.floppy,
         this.breathing,
         this.showInArmor
      );
   }

   public static Boobs getInstance() {
      return instance;
   }
}
