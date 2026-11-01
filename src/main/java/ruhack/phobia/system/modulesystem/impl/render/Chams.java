package ruhack.phobia.system.modulesystem.impl.render;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.ColorSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.colorschem.ColorUtil;

public class Chams extends ModuleStructure {
   private static Chams instance;
   public static boolean renderContext;
   public static boolean wallContext;
   public static int visibleLight;
   public static int visibleOverlay;
   public static int wallLight;
   public static int wallOverlay;
   public static boolean renderingSelf;
   public final SelectSetting target = new SelectSetting("Цель", "На кого накладывать эффект", "Игроки", "Игроки", "Мобы", "Все");
   public final SelectSetting colorMode = new SelectSetting("Цвет", "Источник цветов эффекта", "От темы", "От темы", "Кастомный");
   public final ColorSetting color1 = new ColorSetting("Цвет 1", "Первый цвет градиента")
      .value(ColorUtil.getColor(150, 60, 255, 255))
      .visible(() -> this.colorMode.isSelected("Кастомный"));
   public final ColorSetting color2 = new ColorSetting("Цвет 2", "Второй цвет градиента")
      .value(ColorUtil.getColor(60, 220, 255, 255))
      .visible(() -> this.colorMode.isSelected("Кастомный"));
   public final SliderSettings speed = new SliderSettings("Скорость", "Скорость анимации", 1.0F).range(0.1F, 3.0F).step(0.1F);
   public final SliderSettings fill = new SliderSettings("Заливка", "Насколько эффект перекрывает текстуру", 100.0F).range(0, 100).suffix("%");
   public final BooleanSetting throughWalls = new BooleanSetting("Через стены", "Показывать сущностей за стенами").setValue(true);
   public final BooleanSetting onSelf = new BooleanSetting("На себе", "Накладывать эффект на себя (вид от третьего лица)");

   public static Chams getInstance() {
      return instance;
   }

   public Chams() {
      super("Chams", "Шейдерная подсветка моделей сущностей", ModuleCategory.RENDER);
      instance = this;
      this.settings(this.target, this.colorMode, this.color1, this.color2, this.speed, this.fill, this.throughWalls, this.onSelf);
   }

   public float fillFraction() {
      return this.fill.getValue() / 100.0F;
   }

   public int getPrimaryColor() {
      return this.colorMode.isSelected("От темы") ? ColorUtil.getClientColorAt(0.0F) : this.color1.getColor();
   }

   public int getSecondaryColor() {
      return this.colorMode.isSelected("От темы") ? ColorUtil.getClientColorAt(0.5F) : this.color2.getColorNoAlpha();
   }

   public boolean shouldApply(LivingEntityRenderState state) {
      boolean isPlayer = state instanceof PlayerEntityRenderState;
      if (isPlayer && !this.onSelf.isValue() && mc.player != null && ((PlayerEntityRenderState)state).id == mc.player.getId()) {
         return false;
      } else if (this.target.isSelected("Игроки")) {
         return isPlayer;
      } else {
         return this.target.isSelected("Мобы") ? !isPlayer : true;
      }
   }
}
