package ruhack.phobia.system.modulesystem.impl.other;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.util.math.MathHelper;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.ColorSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.istance.Instance;
import ruhack.phobia.util.render.GradientUtil;

public final class Theme extends ModuleStructure {
   public static final String CUSTOM_NAME = "Custom";
   private static final Theme.Palette CUSTOM = palette("Custom", "#78BCFF", "#B892FF", "#FF73A8");
   private static final List<Theme.Palette> PALETTES = List.of(
      CUSTOM,
      palette("Красная", "#FF4055"),
      palette("Белая", "#F2F3F7"),
      palette("Жёлтая", "#FFD447"),
      palette("Синяя", "#7387FF", "#858CFF"),
      palette("Зелёная", "#45D483"),
      palette("Фиолетовая", "#AD73FF", "#C685FF"),
      palette("Розовая", "#FF72B6"),
      palette("Оранжевая", "#FF8A45"),
      palette("Бирюзовая", "#3EDBC5"),
      palette("Прозрачная", "#5F71BF", "#858CFF"),
      palette("JShine", "#78BCFF", "#FF73A8"),
      palette("Ледяная сирень", "#5F71BF", "#A0B0EF"),
      palette("Северное сияние", "#42E8B4", "#55B8FF", "#987BFF"),
      palette("Лиловый сумрак", "#7362B0", "#9C8ADF"),
      palette("Чистое небо", "#6FA4C8", "#7FBAF4"),
      palette("Песчаное тепло", "#B9B079", "#D6D4A8"),
      palette("Пудровая роза", "#C78097", "#DD95B1"),
      palette("Закатный коралл", "#C97576", "#DD7F81"),
      palette("Рубиновый неон", "#FF315C", "#FF587D", "#BE3BFF"),
      palette("Мятный океан", "#2DE2B3", "#25BCEB", "#477BFF"),
      palette("Золотой час", "#FFB347", "#FFD56A", "#FF7B62"),
      palette("Cyber Lime", "#B8FF5A", "#39E6A2", "#31A8FF"),
      palette("Electric Night", "#335CFF", "#8B5CFF", "#FF4FD8"),
      palette("Cherry Ice", "#FF477E", "#FF85A1", "#86C5FF"),
      palette("Deep Space", "#3548C8", "#805AD5", "#D946EF"),
      palette("Toxic Sunset", "#C9FF45", "#FFCF4A", "#FF5F57"),
      palette("Ocean Flame", "#00D4FF", "#4169E1", "#FF4D8D"),
      palette("Aurora Glass", "#67F5C8", "#7BB8FF", "#E2A8FF"),
      palette("Crimson Frost", "#FF365D", "#B75CFF", "#70C7FF")
   );
   private static final Map<String, Theme.Palette> PALETTES_BY_NAME = PALETTES.stream()
      .collect(Collectors.toUnmodifiableMap(Theme.Palette::name, palette -> (Theme.Palette)palette));
   private static final Map<String, String> LEGACY_THEMES = Map.of(
      "Красная",
      "Красная",
      "Белая",
      "Белая",
      "Жёлтая",
      "Жёлтая",
      "Синяя",
      "Синяя",
      "Зелёная",
      "Зелёная",
      "Фиолетовая",
      "Фиолетовая",
      "Розовая",
      "Розовая",
      "Оранжевая",
      "Оранжевая",
      "Бирюзовая",
      "Бирюзовая"
   );
   public final SelectSetting color = new SelectSetting(
         "Тема", "Цвет ClickGUI, HUD и визуальных эффектов", "JShine", PALETTES.stream().map(Theme.Palette::name).toArray(String[]::new)
      )
      .visible(() -> false);
   public final SelectSetting customColorCount = new SelectSetting("Количество цветов", "Число цветов в собственном градиенте", "3", "1", "2", "3")
      .visible(this::isCustomSelected);
   public final ColorSetting customColor1 = new ColorSetting("Цвет 1", "Начало градиента").value(ColorUtil.toColor("#78BCFF")).visible(this::isCustomSelected);
   public final ColorSetting customColor2 = new ColorSetting("Цвет 2", "Середина градиента")
      .value(ColorUtil.toColor("#B892FF"))
      .visible(() -> this.isCustomSelected() && this.customColorCount() >= 2);
   public final ColorSetting customColor3 = new ColorSetting("Цвет 3", "Конец градиента")
      .value(ColorUtil.toColor("#FF73A8"))
      .visible(() -> this.isCustomSelected() && this.customColorCount() >= 3);

   public Theme() {
      super("Theme", "Выбор готовой или собственной темы клиента", ModuleCategory.OTHER);
      this.settings(this.color, this.customColorCount, this.customColor1, this.customColor2, this.customColor3);
   }

   public static Theme getInstance() {
      return Instance.get(Theme.class);
   }

   public int getAccentColor() {
      return this.getColorAt(0.0F, 255);
   }

   public int getColorAt(float position, int alpha) {
      Theme.Palette palette = this.selectedPalette();
      if (this.isCustom(palette)) {
         this.syncCustomPalette();
      }

      int color = GradientUtil.color(18, Math.round(position * 360.0F), palette.colors());
      return ColorUtil.replAlpha(color, MathHelper.clamp(alpha, 0, 255));
   }

   public Theme.Palette selectedPalette() {
      String selected = LEGACY_THEMES.getOrDefault(this.color.getValue(), this.color.getValue());
      if (!selected.equals(this.color.getValue())) {
         this.color.setValue(selected);
      }

      return PALETTES_BY_NAME.getOrDefault(selected, PALETTES.getFirst());
   }

   public List<Theme.Palette> palettes() {
      this.syncCustomPalette();
      return PALETTES;
   }

   public boolean isSelected(Theme.Palette palette) {
      return palette.name().equals(this.color.getValue());
   }

   public void select(Theme.Palette palette) {
      this.color.setValue(palette.name());
   }

   public boolean isCustom(Theme.Palette palette) {
      return palette == CUSTOM || "Custom".equals(palette.name());
   }

   public boolean isCustomSelected() {
      return "Custom".equals(this.color.getValue());
   }

   private int customColorCount() {
      try {
         return MathHelper.clamp(Integer.parseInt(this.customColorCount.getValue()), 1, 3);
      } catch (NumberFormatException ignored) {
         return 3;
      }
   }

   private void syncCustomPalette() {
      int count = this.customColorCount();
      int[] target = CUSTOM.colors();
      target[0] = this.customColor1.getColorNoAlpha();
      target[1] = count >= 2 ? this.customColor2.getColorNoAlpha() : target[0];
      target[2] = count >= 3 ? this.customColor3.getColorNoAlpha() : target[count >= 2 ? 1 : 0];
   }

   private static Theme.Palette palette(String name, String... colors) {
      int[] parsed = new int[colors.length];

      for (int i = 0; i < colors.length; i++) {
         parsed[i] = ColorUtil.toColor(colors[i]);
      }

      return new Theme.Palette(name, parsed);
   }

   public record Palette(String name, int... colors) {
      public int color() {
         return this.colors.length == 0 ? -1 : this.colors[0];
      }

      public int colorAt(float position, int alpha) {
         if (this.colors.length == 0) {
            return ColorUtil.rgba(255, 255, 255, alpha);
         }

         if (this.colors.length == 1) {
            return ColorUtil.replAlpha(this.colors[0], alpha);
         }

         float wrapped = position - (float)Math.floor(position);
         float scaled = wrapped * this.colors.length;
         int from = Math.min(this.colors.length - 1, (int)scaled);
         int to = (from + 1) % this.colors.length;
         return ColorUtil.replAlpha(ColorUtil.interpolateColor(this.colors[from], this.colors[to], scaled - from), alpha);
      }
   }
}
