package ruhack.phobia.system.screensystem.clickgui;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import ruhack.phobia.system.modulesystem.ModuleStructure;

public final class ModulePresentation {
   private static final Map<String, ModulePresentation.Entry> ENTRIES = new HashMap<>();

   private ModulePresentation() {
   }

   public static String section(ModuleStructure module) {
      ModulePresentation.Entry entry = ENTRIES.get(module.getName());
      return entry == null ? module.getCategory().getReadableName() : entry.section;
   }

   public static String description(ModuleStructure module) {
      ModulePresentation.Entry entry = ENTRIES.get(module.getName());
      if (entry != null) {
         return entry.description;
      }

      String description = module.getDescription();
      return description != null && !description.isBlank() ? description : "Описание отсутствует.";
   }

   public static Comparator<ModuleComponent> comparator() {
      return Comparator.<ModuleComponent>comparingInt(component -> entry(component).sectionOrder)
         .thenComparingInt(component -> entry(component).itemOrder)
         .thenComparing(component -> component.getModule().getName(), String.CASE_INSENSITIVE_ORDER);
   }

   private static ModulePresentation.Entry entry(ModuleComponent component) {
      return ENTRIES.getOrDefault(
         component.getModule().getName(),
         new ModulePresentation.Entry(component.getModule().getCategory().getReadableName(), 100, 100, description(component.getModule()))
      );
   }

   private static ModulePresentation.Item item(String name, String description) {
      return new ModulePresentation.Item(name, description);
   }

   private static void section(String section, int sectionOrder, ModulePresentation.Item... items) {
      for (int i = 0; i < items.length; i++) {
         ModulePresentation.Item item = items[i];
         ENTRIES.put(item.name, new ModulePresentation.Entry(section, sectionOrder, i, item.description));
      }
   }

   static {
      section(
         "Attack",
         0,
         item("KillAura", "Автоматически атакует подходящие цели в заданной дистанции."),
         item("TriggerBot", "Атакует сущность, находящуюся прямо под прицелом."),
         item("LegitAura", "Атакует ближайшую цель с плавными, похожими на обычную игру поворотами."),
         item("ElytraTarget", "Удерживает целью ауры противника, который летит на элитрах."),
         item("TargetPearl", "Прогнозирует приземление чужого жемчуга и бросает свой к той же точке."),
         item("AimingBalls", "Наводит камеру на выбранные типы ценного дропа поблизости."),
         item("AutoExplosion", "Автоматически ставит и взрывает кристалл на обсидиане или бедроке."),
         item("MaceTarget", "Автоматизирует полет, наведение и удар булавой по выбранной цели.")
      );
      section(
         "Assistance",
         1,
         item("ShiftTap", "Автоматически кратко нажимает приседание после удара."),
         item("HitBox", "Увеличивает область попадания выбранных сущностей."),
         item("AntiBot", "Исключает серверных ботов и ложные цели из выбора ауры."),
         item("Velocity", "Уменьшает или полностью убирает отбрасывание при получении урона."),
         item("NoFriendDamage", "Блокирует атаки по игрокам, добавленным в список друзей.")
      );
      section(
         "Automation",
         2,
         item("AutoPotion", "Автоматически бросает выбранные зелья под игрока при необходимости."),
         item("AutoGApple", "Автоматически начинает есть золотое яблоко при заданных условиях."),
         item("AutoTotem", "Перемещает тотем в левую руку, когда игроку угрожает опасность."),
         item("AutoSwap", "Переключает два выбранных предмета в левой руке по клавише."),
         item("AutoCart", "Устанавливает и подрывает вагонетку с динамитом на ближайших рельсах."),
         item("WebTrap", "Ставит паутину в позицию цели по назначенной клавише.")
      );
      section(
         "Speed",
         10,
         item("AutoSprint", "Автоматически включает бег в обычном или легитном режиме."),
         item("Speed", "Повышает скорость передвижения выбранным режимом."),
         item("Strafe", "Улучшает управление скоростью и направлением движения в воздухе."),
         item("TargetStrafe", "Автоматически движется по окружности вокруг цели KillAura."),
         item("WaterSpeed", "Ускоряет горизонтальное передвижение в воде."),
         item("NoSlowDown", "Убирает замедление во время еды, блокирования и использования предметов.")
      );
      section(
         "Traversal",
         11,
         item("Fly", "Позволяет перемещаться по воздуху в выбранном режиме полета."),
         item("HighJump", "Увеличивает высоту прыжка игрока."),
         item("Jesus", "Позволяет стоять и двигаться по поверхности воды."),
         item("Spider", "Позволяет подниматься вверх вдоль вертикальных стен."),
         item("Scaffold", "Автоматически ставит блоки под игроком во время движения.")
      );
      section(
         "Assistance",
         12,
         item("FireworkBooster", "Изменяет ускорение от фейерверка во время полета на элитрах."),
         item("AntiWeb", "Убирает сильное замедление при движении внутри паутины."),
         item("NoFallDamage", "Предотвращает получение урона от падения.")
      );
      section(
         "Interaction",
         20,
         item("NoDelay", "Убирает выбранные задержки прыжка, правого клика и ломания блоков."),
         item("AntiPush", "Отключает толчки и столкновения от выбранных блоков и сущностей."),
         item("NoEntityTrace", "Позволяет взаимодействовать с блоками сквозь сущности; отдельно учитывает меч."),
         item("NoInteract", "Блокирует случайное взаимодействие с блоками.")
      );
      section(
         "Automation",
         21,
         item("TapeMouse", "Автоматически удерживает или нажимает выбранную клавишу управления."),
         item("RegionExploit", "Передает координаты между клиентами и автоматизирует создание региона вокруг точки.")
      );
      section("Camera", 22, item("FreeCam", "Отделяет камеру от игрока и позволяет свободно осматривать мир."));
      section("Interface", 30, item("Hud", "Настраивает вид, положение и масштаб элементов игрового интерфейса."));
      section(
         "World",
         31,
         item("Ambience", "Локально изменяет отображаемое время суток и освещение мира."),
         item("ShaderSky", "Заменяет обычное небо на анимированный шейдер: северное сияние, чёрную дыру или галактику."),
         item("Particles", "Создаёт glow-частицы в бездействии, за летящей жемчужиной и при критическом ударе."),
         item("TargetESP", "Отображает текущую цель KillAura кольцом, ромбом, душами или эффектом Ghosts."),
         item("BlockOverlay", "Подсвечивает блок, на который наведен прицел."),
         item("HitPoint", "Отмечает в мире точку последнего попадания по сущности."),
         item("SeeInvisible", "Делает невидимых игроков и сущностей видимыми для клиента."),
         item("NoRender", "Скрывает выбранные экранные эффекты, оверлеи, BossBar и Scoreboard.")
      );
      section(
         "View",
         32,
         item("AspectRatio", "Изменяет соотношение сторон игровой перспективы."),
         item("ViewModel", "Меняет позицию, поворот и масштаб предметов в руках."),
         item("ShaderHands", "Накладывает на руки и предметы от первого лица настраиваемое свечение или градиент."),
         item("Custom Models", "Применяет к вашему игроку одну из встроенных анимированных моделей."),
         item("Chams", "Подсвечивает модели игроков или мобов цветным шейдером, в том числе сквозь стены."),
         item("SwingAnimation", "Заменяет анимацию взмаха и использования предмета от первого лица."),
         item("ChinaHat", "Рисует декоративную настраиваемую шляпу над моделью игрока."),
         item("Wings", "Рисует анимированные крылья цвета темы за спиной игроков."),
         item("Boobs", "Добавляет модели игрока настраиваемую грудь с физикой движения.")
      );
      section(
         "Overlay",
         33,
         item("Tags", "Показывает имена и здоровье выбранных сущностей и названия предметов."),
         item("Arrows", "Показывает направление на игроков, друзей и предметы."),
         item("Prediction", "Показывает траекторию и время до падения жемчуга, стрел и трезубцев."),
         item("ShulkerPreview", "Показывает содержимое шалкера при зажатом Ctrl.")
      );
      section(
         "Inventory",
         40,
         item("ClickAction", "Бросает эндер-жемчуг или заряд ветра и добавляет или удаляет игрока из друзей по отдельным клавишам."),
         item("NoSlotChange", "Защищает активный слот и важные предметы от случайной смены или выброса."),
         item("ItemScroller", "Быстро переносит, выбрасывает или собирает одинаковые предметы в контейнере."),
         item("ElytraHelper", "Быстро меняет нагрудник на элитры и использует фейерверк по назначенным клавишам.")
      );
      section(
         "Automation",
         41,
         item("AutoRespawn", "Автоматически возрождает игрока после смерти."),
         item("AutoTpAccept", "Автоматически принимает входящие запросы на телепортацию."),
         item("AutoLeave", "Автоматически покидает сервер или режим при появлении опасного игрока рядом."),
         item("AutoDuel", "Автоматически отправляет выбранным игрокам запросы на дуэль."),
         item("BowSpammer", "Быстро натягивает и отпускает лук с заданной задержкой.")
      );
      section(
         "Network",
         42,
         item("NameProtect", "Скрывает ваш ник и при необходимости ники друзей в отображаемом тексте."),
         item("ServerRPSpoof", "Подтверждает серверу загрузку обязательного ресурспака, не применяя его локально.")
      );
      section("Appearance", 50, item("Theme", "Меняет основной цвет ClickGUI, HUD и остальных акцентных элементов клиента."));
   }

   private record Entry(String section, int sectionOrder, int itemOrder, String description) {
   }

   private record Item(String name, String description) {
   }
}
