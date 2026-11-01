package ruhack.phobia.system.modulesystem.impl.display;

import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.istance.Instance;

public class Hud extends ModuleStructure {
   public final MultiSelectSetting interfaceSettings = new MultiSelectSetting("Элементы", "Настройка элементов интерфейса")
      .value("Logotype", "Info", "Potions", "Cooldowns", "TargetHud", "Notifications", "Hotkeys", "BindPreview", "ArrayList", "AutoWarden")
      .selected("Logotype", "Info", "Potions", "Cooldowns", "TargetHud", "Notifications", "Hotkeys", "BindPreview", "ArrayList", "AutoWarden");
   public final BooleanSetting showBps = new BooleanSetting("Show BPS", "Показывать блоки в секунду")
      .setValue(true)
      .visible(() -> this.interfaceSettings.isSelected("Info"));
   public final BooleanSetting showTps = new BooleanSetting("Show TPS", "Показывать TPS в Logotype")
      .setValue(true)
      .visible(() -> this.interfaceSettings.isSelected("Logotype"));
   public final SliderSettings interfaceScale = new SliderSettings("Размер интерфейса", "Масштаб всех элементов HUD", 100.0F).range(70, 130).suffix("%");
   public final SliderSettings backgroundAlpha = new SliderSettings("HUD: Background Alpha", "Background opacity for HUD elements", 190.0F).range(0, 255);
   public final BooleanSetting arrayListShowModes = new BooleanSetting("ArrayList: Show Modes", "Показывать текущий режим модуля")
      .setValue(true)
      .visible(() -> this.interfaceSettings.isSelected("ArrayList"));
   public final SliderSettings arrayListBackgroundAlpha = new SliderSettings("ArrayList: Background Alpha", "Прозрачность чёрного фона", 190.0F)
      .range(0, 255)
      .visible(() -> this.interfaceSettings.isSelected("ArrayList"));
   public final SliderSettings arrayListAnimation = new SliderSettings("ArrayList: Animation Speed", "Длительность анимации", 200.0F)
      .range(100, 400)
      .suffix(" ms")
      .visible(() -> this.interfaceSettings.isSelected("ArrayList"));
   public final SliderSettings arrayListFontSize = new SliderSettings("ArrayList: Font Size", "Размер текста модулей", 6.5F)
      .range(5.0F, 9.0F)
      .step(0.25F)
      .visible(() -> this.interfaceSettings.isSelected("ArrayList"));
   public final BooleanSetting arrayListSideBar = new BooleanSetting("ArrayList: Side Bar", "Показывать цветную полоску слева")
      .setValue(true)
      .visible(() -> this.interfaceSettings.isSelected("ArrayList"));
   public final SelectSetting arrayListColorMode = new SelectSetting("ArrayList: Color Mode", "Цвет названий и боковой полоски", "Тема", "Тема", "Градиент")
      .visible(() -> this.interfaceSettings.isSelected("ArrayList"));

   public static Hud getInstance() {
      return Instance.get(Hud.class);
   }

   public Hud() {
      super("Hud", "Настройка элементов интерфейса", ModuleCategory.DISPLAY);
      this.settings(
         this.interfaceSettings,
         this.interfaceScale,
         this.backgroundAlpha,
         this.showBps,
         this.showTps,
         this.arrayListShowModes,
         this.arrayListBackgroundAlpha,
         this.arrayListAnimation,
         this.arrayListFontSize,
         this.arrayListSideBar,
         this.arrayListColorMode
      );
   }

   public float getInterfaceScale() {
      return this.interfaceScale.getValue() / 100.0F;
   }
}
