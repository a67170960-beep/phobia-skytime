package ruhack.phobia.system.modulesystem.impl.rage;

import com.adl.nativeprotect.Native;
import java.util.Locale;
import java.util.Objects;
import lombok.Generated;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.InputEvent;
import ruhack.phobia.system.eventsystem.impl.RotationUpdateEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.attack.AttackHandler;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.attack.AttackPerpetrator;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic.AngleSmoothMode;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic.LinearSmoothMode;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.packet.AdminSmooth;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.packet.MLSSmooth;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.packet.MatrixSmooth;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.packet.NeuroSmooth;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.packet.Snap;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.packet.SpookyNewSmooth;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.packet.SpookyTimeSmooth;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.packet.SkyTimeSmooth;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.target.TargetSelector;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.aim.neuro.NeuroController;
import ruhack.phobia.util.istance.Instance;
import ruhack.phobia.util.math.TaskPriority;
import ruhack.phobia.util.player.swap.InventoryUtils;
import ruhack.phobia.util.rotation.RotationConfig;
import ruhack.phobia.util.rotation.RotationController;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;
import ruhack.phobia.util.rotation.mod.PointFinder;

public class KillAura extends ModuleStructure {
   TargetSelector targetSelector = new TargetSelector();
   PointFinder pointFinder = new PointFinder();
   SpookyTimeSmooth spookyTimeSmooth = new SpookyTimeSmooth();
   SkyTimeSmooth skyTimeSmooth = new SkyTimeSmooth();
   SpookyNewSmooth spookyNewSmooth = new SpookyNewSmooth();
   MLSSmooth mlsSmooth = new MLSSmooth();
   NeuroSmooth neuroSmooth = new NeuroSmooth();
   LivingEntity target;
   LivingEntity lastTarget;
   public final SelectSetting aimType = new SelectSetting(
      "Режим", "Выбор типа ротации", "Matrix", "Matrix", "Snap", "Admin", "SpookyTime", "SpookyNew", "MLS", "Neuro", "SkyTime"
   );
   public final SliderSettings timeOnTick = new SliderSettings("Время снапов", "Время которое снапы будут смотреть на таргета после удара", 50.0F)
      .range(1.0F, 300.0F)
      .step(1.0F)
      .visible(() -> this.aimType.isSelected("Snap"));
   public final SliderSettings speedValue = new SliderSettings("Скорость снапов", "Скорость наводки и отводки у снапов", 50.0F)
      .range(15.0F, 180.0F)
      .step(5.0F)
      .visible(() -> this.aimType.isSelected("Snap"));
   public final SliderSettings attackRange = new SliderSettings("Дистанция ударов", "Максимальная дистанция для атаки", 3.0F).range(2.5F, 6.0F).step(0.1F);
   public final SelectSetting sprintMode = new SelectSetting(
      "Спринт",
      "Сброс спринта при ударе для нокбэка: Пакет — сброс пакетом (незаметно), Легит — настоящий W-tap, Не сбрасывать — бить не трогая спринт",
      "Пакет",
      "Пакет",
      "Легит",
      "Не сбрасывать"
   );
   public final SelectSetting clickType = new SelectSetting("Режим кликов", "Выбор типа кликов", "1.9+", "1.8", "1.9+");
   public final SliderSettings cpsValue = new SliderSettings("Значение кпс", "Скорость заклика", 30.0F)
      .range(5.0F, 60.0F)
      .step(5.0F)
      .visible(() -> this.clickType.isSelected("1.8"));
   private final SliderSettings lookrange = new SliderSettings("Дистанция поиска", "Дополнительная дистанция для поиска таргета", 1.5F).range(0.5F, 10.0F);
   public final MultiSelectSetting targetType = new MultiSelectSetting("Выбор таргетов", "Типы целей для атаки")
      .value("Игроки", "Голые", "Мобы", "Друзья")
      .selected("Игроки");
   public final MultiSelectSetting options = new MultiSelectSetting("Основные настройки", "Дополнительные параметры модуля")
      .value("Не бить при использовании", "Бить через стены", "Отжимать щит", "Ломать щит", "Альтернативная коррекция движения")
      .selected("Не бить при использовании");
   public final BooleanSetting onlyCriticals = new BooleanSetting("Криты только с пробелом", "Крит только при нажатии пробела").setValue(true);
   public final BooleanSetting autoMace = new BooleanSetting("Авто булава", "Автоматически бьёт булавой при падении").setValue(false);
   public final SliderSettings maceMinFall = new SliderSettings("Мин. падение", "Минимальная высота падения для удара булавой", 1.5F)
      .range(0.5F, 10.0F)
      .step(0.5F)
      .visible(() -> this.autoMace.isValue());
   public final BooleanSetting maceReturnSlot = new BooleanSetting("Возвращать слот", "Возвращаться на прошлый слот после удара")
      .setValue(true)
      .visible(() -> this.autoMace.isValue());
   public final BooleanSetting maximumDamage = new BooleanSetting("Максимальный урон", "На момент удара меняет обычную сферу в оффхенде на выбранную")
      .setValue(false);
   public final SelectSetting damageSphere = new SelectSetting(
         "Артефакт для удара",
         "Сфера или талисман, временно устанавливаемые в оффхенд на момент атаки",
         "Сфера Ареса",
         "Сфера Ареса",
         "Сфера Афины",
         "Сфера Титана",
         "Сфера Хаоса",
         "Сфера Сатира",
         "Сфера Бестии",
         "Сфера Гидры",
         "Сфера Икара",
         "Сфера Эрида",
         "Талисман Карателя",
         "Талисман Ярости"
      )
      .visible(() -> this.maximumDamage.isValue());

   public static KillAura getInstance() {
      return Instance.get(KillAura.class);
   }

   public KillAura() {
      super("KillAura", "Автоматически бьет сущностей вокруг", ModuleCategory.RAGE);
      this.settings(
         this.aimType,
         this.timeOnTick,
         this.speedValue,
         this.attackRange,
         this.lookrange,
         this.targetType,
         this.options,
         this.onlyCriticals,
         this.sprintMode,
         this.clickType,
         this.cpsValue,
         this.autoMace,
         this.maceMinFall,
         this.maceReturnSlot,
         this.maximumDamage,
         this.damageSphere
      );
   }

   public boolean isDamageSphereEquipped() {
      return this.maximumDamage.isValue() && mc.player != null ? this.matchesArtifact(mc.player.getOffHandStack(), this.damageSphere.getValue()) : false;
   }

   public int swapDamageSphereForAttack() {
      if (this.maximumDamage.isValue() && mc.player != null) {
         String wanted = this.damageSphere.getValue();
         if (this.matchesArtifact(mc.player.getOffHandStack(), wanted)) {
            return -1;
         }

         int artifactSlot = this.findArtifactScreenSlot(wanted);
         if (artifactSlot < 0) {
            return -2;
         }

         InventoryUtils.dropCursorStack();
         InventoryUtils.swapToOffhand(artifactSlot);
         return artifactSlot;
      } else {
         return -2;
      }
   }

   public void restoreDamageSphereAfterAttack(int sphereSlot) {
      if (sphereSlot >= 0 && mc.player != null) {
         InventoryUtils.dropCursorStack();
         InventoryUtils.swapToOffhand(sphereSlot);
         this.hideArtifactFromMainHand();
      }
   }

   private int findArtifactScreenSlot(String wanted) {
      int selectedGui = 36 + mc.player.getInventory().getSelectedSlot();

      for (int i = 9; i < 36; i++) {
         if (this.matchesArtifact(mc.player.getInventory().getStack(i), wanted)) {
            return i;
         }
      }

      for (int i = 0; i < 9; i++) {
         int gui = i + 36;
         if (gui != selectedGui && this.matchesArtifact(mc.player.getInventory().getStack(i), wanted)) {
            return gui;
         }
      }

      if (this.matchesArtifact(mc.player.getMainHandStack(), wanted)) {
         return selectedGui;
      }

      for (int i = 0; i < 9; i++) {
         if (this.matchesArtifact(mc.player.getInventory().getStack(i), wanted)) {
            return i + 36;
         }
      }

      return -1;
   }

   private void hideArtifactFromMainHand() {
      if (this.maximumDamage.isValue() && mc.player != null) {
         String wanted = this.damageSphere.getValue();
         if (this.matchesArtifact(mc.player.getMainHandStack(), wanted)) {
            int selected = mc.player.getInventory().getSelectedSlot();

            for (int i = 0; i < 9; i++) {
               if (i != selected) {
                  ItemStack stack = mc.player.getInventory().getStack(i);
                  if (stack.isEmpty() || !this.matchesArtifact(stack, wanted)) {
                     InventoryUtils.selectSlot(i);
                     return;
                  }
               }
            }
         }
      }
   }

   private boolean matchesArtifact(ItemStack stack, String name) {
      if (stack != null && !stack.isEmpty()) {
         String setting = name.toLowerCase(Locale.ROOT);
         String itemName = stack.getName().getString().toLowerCase(Locale.ROOT);
         String key = setting.replace("сфера ", "").replace("талисман ", "").trim();
         if (setting.contains("талисман")) {
            return stack.isOf(Items.TOTEM_OF_UNDYING)
               && itemName.contains("талисман")
               && (itemName.contains(key) || key.length() >= 4 && itemName.contains(key.substring(0, key.length() - 1)));
         } else if (!stack.isOf(Items.PLAYER_HEAD)) {
            return false;
         } else {
            return itemName.contains(key) ? true : key.length() >= 4 && itemName.contains(key.substring(0, key.length() - 1));
         }
      } else {
         return false;
      }
   }

   public float finalDistance() {
      return this.attackRange.getValue()
         + (
            mc.player.isGliding() && ElytraTarget.getInstance() != null && ElytraTarget.getInstance().isState()
               ? ElytraTarget.getInstance().elytraFindRange.getValue()
               : this.lookrange.getValue()
         );
   }

   public float attackDistance() {
      return this.attackRange.getValue();
   }

   @Override
   public void deactivate() {
      if (Initialization.getInstance() != null && Initialization.getInstance().getManager() != null) {
         AttackHandler handler = Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
         handler.resetMace();
         handler.resetDamageSphere();
      }

      this.hideArtifactFromMainHand();
      if (this.aimType.isSelected("Neuro") && mc.player != null) {
         Angle rot = RotationController.INSTANCE.getRotation();
         if (rot != null) {
            mc.player.setYaw(rot.getYaw());
            mc.player.setPitch(rot.getPitch());
         }

         RotationController.INSTANCE.forceStop();
      }

      this.targetSelector.releaseTarget();
      this.target = null;
      super.deactivate();
   }

   @EventHandler
   public void inputEvent(InputEvent e) {
      if (this.target != null
         && Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler().canAttack(this.getConfig(), 1)
         && mc.player.distanceTo(this.target) <= this.attackDistance()
         && !mc.player.isSubmergedInWater()
         && !RotationConfig.clickSpam) {
         e.setDirectionalLow(false, false, false, false);
      }

      if (this.options.isSelected("Отжимать щит")
         && this.target != null
         && Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler().canAttack(this.getConfig(), 2)
         && mc.player.isUsingItem()
         && mc.player.getActiveItem().getItem().equals(Items.SHIELD)) {
         mc.player.stopUsingItem();
      }
   }

   @EventHandler
   @Native
   public void onRotationUpdate(RotationUpdateEvent e) {
      switch (e.getType()) {
         case 0:
            Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler().tickMace();
            this.target = this.updateTarget();
            if (this.target != null) {
               AttackPerpetrator.AttackPerpetratorConfigurable config = this.getConfig();
               Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler().prepareDamageSphere(config);
               boolean neuroTrigger = this.aimType.isSelected("Neuro") && !NeuroController.get().isPlaying();
               if (!neuroTrigger) {
                  this.rotateToTarget(config);
               }

               this.lastTarget = this.target;
            }
            break;
         case 2:
            if (this.target != null) {
               Initialization.getInstance().getManager().getAttackPerpetrator().performAttack(this.getConfig());
            }
      }
   }

   private LivingEntity updateTarget() {
      TargetSelector.EntityFilter filter = new TargetSelector.EntityFilter(this.targetType.getSelected());
      float fov = 360.0F;
      this.targetSelector.searchTargets(mc.world.getEntities(), this.finalDistance(), fov, this.options.isSelected("Бить через стены"));
      this.targetSelector.validateTarget(entity -> filter.isValid(entity) && !NeuroController.get().isRecordingTarget(entity));
      return this.targetSelector.getCurrentTarget();
   }

   private void rotateToTarget(AttackPerpetrator.AttackPerpetratorConfigurable config) {
      AttackHandler attackHandler = Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
      RotationController controller = RotationController.INSTANCE;
      Angle.VecRotation rotation = new Angle.VecRotation(config.getAngle(), config.getAngle().toVector());
      RotationConfig rotationConfig = this.getRotationConfig();
      String var6 = this.aimType.getValue();
      byte var7 = -1;
      var6.hashCode();
      switch (var7) {
         default:
            controller.rotateTo(rotation, this.target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
      }
   }

   public AttackPerpetrator.AttackPerpetratorConfigurable getConfig() {
      Vec3d computedPoint;
      Box box;
      if (this.aimType.isSelected("Neuro")) {
         computedPoint = this.target.getEntityPos().add(0.0, this.target.getHeight() * 0.5, 0.0);
         box = this.target.getBoundingBox();
      } else {
         Pair<Vec3d, Box> point = this.pointFinder
            .computeVector(
               this.target,
               this.attackDistance(),
               RotationController.INSTANCE.getRotation(),
               this.getSmoothMode().randomValue(),
               this.options.isSelected("Бить через стены")
            );
         computedPoint = (Vec3d)point.getLeft();
         box = (Box)point.getRight();
      }

      if (mc.player.isGliding() && this.target.isGliding()) {
         Vec3d targetVelocity = this.target.getVelocity();
         double targetSpeed = targetVelocity.horizontalLength();
         float leadTicks = 0.0F;
         if (ElytraTarget.shouldElytraTarget && ElytraTarget.getInstance() != null && ElytraTarget.getInstance().isState()) {
            leadTicks = ElytraTarget.getInstance().elytraForward.getValue();
         }

         if (targetSpeed > 0.35) {
            Vec3d predictedPos = this.target.getEntityPos().add(targetVelocity.multiply(leadTicks));
            computedPoint = predictedPos.add(0.0, this.target.getHeight() / 2.0F, 0.0);
            box = new Box(
               predictedPos.x - this.target.getWidth() / 2.0F,
               predictedPos.y,
               predictedPos.z - this.target.getWidth() / 2.0F,
               predictedPos.x + this.target.getWidth() / 2.0F,
               predictedPos.y + this.target.getHeight(),
               predictedPos.z + this.target.getWidth() / 2.0F
            );
         }
      }

      Angle angle = AngleUtil.fromVec3d(computedPoint.subtract(Objects.requireNonNull(mc.player).getEyePos()));
      return new AttackPerpetrator.AttackPerpetratorConfigurable(
         this.target, angle, this.attackDistance(), this.options.getSelected(), box, this.onlyCriticals.isValue()
      );
   }

   public RotationConfig getRotationConfig() {
      boolean free = false;
      if (this.options.isSelected("Альтернативная коррекция движения")) {
         free = true;
      }

      return new RotationConfig(this.getSmoothMode(), true, free, this.clickType.isSelected("1.8"));
   }

   public AngleSmoothMode getSmoothMode() {
      if (mc.player.isGliding() && ElytraTarget.getInstance() != null && ElytraTarget.getInstance().isState()) {
         return new LinearSmoothMode();
      }

      return switch (this.aimType.getValue()) {
         case "Matrix" -> new MatrixSmooth();
         case "Snap" -> new Snap();
         case "Admin" -> new AdminSmooth();
         case "SpookyTime" -> this.spookyTimeSmooth;
         case "SpookyNew" -> this.spookyNewSmooth;
         case "MLS" -> this.mlsSmooth;
         case "Neuro" -> this.neuroSmooth;
         case "SkyTime" -> this.skyTimeSmooth;
         default -> new LinearSmoothMode();
      };
   }

   @Generated
   public TargetSelector getTargetSelector() {
      return this.targetSelector;
   }

   @Generated
   public PointFinder getPointFinder() {
      return this.pointFinder;
   }

   @Generated
   public SpookyTimeSmooth getSpookyTimeSmooth() {
      return this.spookyTimeSmooth;
   }

   @Generated
   public SpookyNewSmooth getSpookyNewSmooth() {
      return this.spookyNewSmooth;
   }

   @Generated
   public MLSSmooth getMlsSmooth() {
      return this.mlsSmooth;
   }

   @Generated
   public NeuroSmooth getNeuroSmooth() {
      return this.neuroSmooth;
   }

   @Generated
   public LivingEntity getTarget() {
      return this.target;
   }

   @Generated
   public LivingEntity getLastTarget() {
      return this.lastTarget;
   }

   @Generated
   public SelectSetting getAimType() {
      return this.aimType;
   }

   @Generated
   public SliderSettings getTimeOnTick() {
      return this.timeOnTick;
   }

   @Generated
   public SliderSettings getSpeedValue() {
      return this.speedValue;
   }

   @Generated
   public SliderSettings getAttackRange() {
      return this.attackRange;
   }

   @Generated
   public SelectSetting getSprintMode() {
      return this.sprintMode;
   }

   @Generated
   public SelectSetting getClickType() {
      return this.clickType;
   }

   @Generated
   public SliderSettings getCpsValue() {
      return this.cpsValue;
   }

   @Generated
   public SliderSettings getLookrange() {
      return this.lookrange;
   }

   @Generated
   public MultiSelectSetting getTargetType() {
      return this.targetType;
   }

   @Generated
   public MultiSelectSetting getOptions() {
      return this.options;
   }

   @Generated
   public BooleanSetting getOnlyCriticals() {
      return this.onlyCriticals;
   }

   @Generated
   public BooleanSetting getAutoMace() {
      return this.autoMace;
   }

   @Generated
   public SliderSettings getMaceMinFall() {
      return this.maceMinFall;
   }

   @Generated
   public BooleanSetting getMaceReturnSlot() {
      return this.maceReturnSlot;
   }

   @Generated
   public BooleanSetting getMaximumDamage() {
      return this.maximumDamage;
   }

   @Generated
   public SelectSetting getDamageSphere() {
      return this.damageSphere;
   }
}
