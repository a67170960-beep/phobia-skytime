package ruhack.phobia.system.modulesystem.impl.render;

import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.ColorSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.colorschem.ColorUtil;

public class ShaderSky extends ModuleStructure {
   public static final String MODE_AURORA = "Северное сияние";
   public static final String MODE_BLACK_HOLE = "Чёрная дыра";
   public static final String MODE_GALAXY = "Галактика";
   private static ShaderSky instance;
   public final SelectSetting mode = new SelectSetting("Режим", "Тип неба", "Северное сияние", "Северное сияние", "Чёрная дыра", "Галактика");
   public final SelectSetting colorMode = new SelectSetting("Цвет", "Источник цветов шейдера", "От темы", "От темы", "Кастомный");
   public final ColorSetting color1 = new ColorSetting("Цвет 1", "Первый цвет эффекта")
      .value(ColorUtil.getColor(40, 255, 140, 255))
      .visible(() -> this.colorMode.isSelected("Кастомный"));
   public final ColorSetting color2 = new ColorSetting("Цвет 2", "Второй цвет эффекта")
      .value(ColorUtil.getColor(150, 80, 255, 255))
      .visible(() -> this.colorMode.isSelected("Кастомный"));
   public final SliderSettings speed = new SliderSettings("Скорость", "Скорость анимации", 1.0F).range(0.1F, 3.0F).step(0.1F);
   public final SliderSettings brightness = new SliderSettings("Яркость", "Яркость неба", 1.0F).range(0.2F, 2.0F).step(0.1F);

   public static ShaderSky getInstance() {
      return instance;
   }

   public ShaderSky() {
      super("ShaderSky", "Заменяет небо на шейдерное", ModuleCategory.RENDER);
      instance = this;
      this.settings(this.mode, this.colorMode, this.color1, this.color2, this.speed, this.brightness);
   }

   public int getPrimaryColor() {
      return this.colorMode.isSelected("От темы") ? ColorUtil.getClientColorAt(0.0F) : this.color1.getColor();
   }

   public int getSecondaryColor() {
      return this.colorMode.isSelected("От темы") ? ColorUtil.getClientColorAt(0.5F) : this.color2.getColor();
   }
}
