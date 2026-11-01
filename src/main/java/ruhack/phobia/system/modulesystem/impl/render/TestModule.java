package ruhack.phobia.system.modulesystem.impl.render;

import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BindSetting;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.ColorSetting;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.system.modulesystem.setting.implement.TextSetting;
import ruhack.phobia.util.istance.Instance;

public class TestModule extends ModuleStructure {
   public static TestModule getInstance() {
      return Instance.get(TestModule.class);
   }

   public TestModule() {
      super("TestModule", "Модуль для тестов", ModuleCategory.RENDER);
      this.settings(
         new ColorSetting("Цвет", "Выбери цвет модуля").value(-16714241),
         new MultiSelectSetting("Multi Select", "Выбери несколько").value("Option 1", "Option 2", "Option 3", "Option 4", "Option 5"),
         new BooleanSetting("Tint", "Включить эффект тинта"),
         new SliderSettings("Tint Period", "Частота мигания", 800.0F).range(500, 1200).suffix(" мс"),
         new SliderSettings("Speed Two Color", "Скорость переливания цветов", 25.0F).range(15, 50),
         new BooleanSetting(
               "Show HUD",
               "Эта настройка позволяет отображать графический интерфейс поверх игры, предоставляя важную информацию в реальном времени для лучшего контроля ситуации."
            )
            .setValue(true),
         new SliderSettings(
               "Animation Speed",
               "Скорость анимации интерфейса определяет насколько плавно и быстро будут открываться окна и переключаться элементы управления в чите.",
               4.0F
            )
            .range(1, 10),
         new SelectSetting(
            "Select",
            "Выбор режима работы модуля позволяет адаптировать функционал под разные игровые ситуации и сервера с различной защитой.",
            "Totem",
            "Use",
            "Drink",
            "Totem",
            "Function",
            "Pickup"
         ),
         new MultiSelectSetting(
               "Multi Select", "Выберите несколько функций одновременно для создания уникальной комбинации способностей, которая будет работать синхронно."
            )
            .value("KillAura", "Fly", "Speed", "KillAura", "WallHack")
            .selected("KillAura", "Fly"),
         new TextSetting(
            "Text",
            "Введите произвольный текст для кастомизации визуальных элементов или указания специфических параметров работы данного модуля.",
            "Default text"
         ),
         new BindSetting("Key", "Клавиша для включения")
      );
   }
}
