package ruhack.phobia.system.modulesystem.impl.legit;

import java.util.Objects;
import lombok.Generated;
import net.minecraft.entity.LivingEntity;
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
import ruhack.phobia.system.modulesystem.impl.legit.legitaura.angle.LegitSmooth;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.attack.AttackHandler;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.attack.AttackPerpetrator;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic.AngleSmoothMode;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.target.TargetSelector;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.istance.Instance;
import ruhack.phobia.util.math.TaskPriority;
import ruhack.phobia.util.rotation.RotationConfig;
import ruhack.phobia.util.rotation.RotationController;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;
import ruhack.phobia.util.rotation.mod.PointFinder;

public class LegitAura extends ModuleStructure {
   TargetSelector targetSelector = new TargetSelector();
   PointFinder pointFinder = new PointFinder();
   LivingEntity target;
   LivingEntity lastTarget;
   public final SliderSettings speed = new SliderSettings("Скорость наводки", "Скорость наводки на сущность", 15.0F).range(5.0F, 100.0F).step(5.0F);
   public final SliderSettings attackRange = new SliderSettings("Дистанция ударов", "Максимальная дистанция ударов", 3.0F).range(2.5F, 6.0F).step(0.1F);
   public final MultiSelectSetting targetType = new MultiSelectSetting("Выбор таргетов", "Выбор сущностей для таргета")
      .value("Игроки", "Мобы")
      .selected("Игроки");
   public final MultiSelectSetting options = new MultiSelectSetting("Основные настройки", "Основные настройки модуля")
      .value("Не бить при использовании", "Бить через стены", "Отжимать щит")
      .selected("Не бить при использовании");
   public final BooleanSetting onlyCriticals = new BooleanSetting("Криты только с пробелом", "Крит только при зажатии пробела").setValue(true);
   public final BooleanSetting pitchCorrecting = new BooleanSetting("Наводить питч", "Включает наводку питча (по высоте)").setValue(false);

   public static LegitAura getInstance() {
      return Instance.get(LegitAura.class);
   }

   public LegitAura() {
      super("LegitAura", "Точно такая-же killaura но с закосом на легит", ModuleCategory.LEGIT);
      this.settings(this.attackRange, this.speed, this.targetType, this.options, this.onlyCriticals, this.pitchCorrecting);
   }

   public float finalDistance() {
      return this.attackRange.getValue() + 0.4F;
   }

   public float attackDistance() {
      return this.attackRange.getValue();
   }

   @Override
   public void deactivate() {
      this.targetSelector.releaseTarget();
      this.target = null;
      mc.player.setYaw(RotationController.INSTANCE.getRotation().getYaw());
      mc.player.setPitch(RotationController.INSTANCE.getRotation().getPitch());
      super.deactivate();
   }

   @EventHandler
   public void onRotationUpdate(RotationUpdateEvent e) {
      if (this.target == null) {
         mc.player.setYaw(RotationController.INSTANCE.getRotation().getYaw());
         mc.player.setPitch(RotationController.INSTANCE.getRotation().getPitch());
      }

      switch (e.getType()) {
         case 0:
            this.target = this.updateTarget();
            if (this.target != null) {
               this.rotateToTarget(this.getConfig());
               this.lastTarget = this.target;
            }
            break;
         case 2:
            if (this.target != null) {
               Initialization.getInstance().getManager().getAttackPerpetrator().performAttack(this.getConfig());
            }
      }
   }

   private void rotateToTarget(AttackPerpetrator.AttackPerpetratorConfigurable config) {
      AttackHandler attackHandler = Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
      RotationController controller = RotationController.INSTANCE;
      Angle.VecRotation rotation = new Angle.VecRotation(config.getAngle(), config.getAngle().toVector());
      RotationConfig rotationConfig = this.getRotationConfig();
      controller.rotateTo(rotation, this.target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
   }

   public RotationConfig getRotationConfig() {
      boolean free = false;
      return new RotationConfig(this.getSmoothMode(), true, free, false);
   }

   public AngleSmoothMode getSmoothMode() {
      return new LegitSmooth();
   }

   @EventHandler
   public void inputEvent(InputEvent e) {
      if (this.target != null
         && Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler().canAttack(this.getConfig(), 1)
         && !mc.player.isSubmergedInWater()) {
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

   private LivingEntity updateTarget() {
      TargetSelector.EntityFilter filter = new TargetSelector.EntityFilter(this.targetType.getSelected());
      float fov = 360.0F;
      this.targetSelector.searchTargets(mc.world.getEntities(), this.finalDistance(), fov, this.options.isSelected("Бить через стены"));
      this.targetSelector.validateTarget(filter::isValid);
      return this.targetSelector.getCurrentTarget();
   }

   public AttackPerpetrator.AttackPerpetratorConfigurable getConfig() {
      Pair<Vec3d, Box> point = this.pointFinder
         .computeVector(
            this.target,
            this.attackDistance(),
            RotationController.INSTANCE.getRotation(),
            new Vec3d(0.0, 0.0, 0.0),
            this.options.isSelected("Бить через стены")
         );
      Angle angle = AngleUtil.fromVec3d(((Vec3d)point.getLeft()).subtract(Objects.requireNonNull(mc.player).getEyePos()));
      Box box = (Box)point.getRight();
      return new AttackPerpetrator.AttackPerpetratorConfigurable(
         this.target, angle, this.attackDistance(), this.options.getSelected(), box, this.onlyCriticals.isValue()
      );
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
   public LivingEntity getTarget() {
      return this.target;
   }

   @Generated
   public LivingEntity getLastTarget() {
      return this.lastTarget;
   }

   @Generated
   public SliderSettings getSpeed() {
      return this.speed;
   }

   @Generated
   public SliderSettings getAttackRange() {
      return this.attackRange;
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
   public BooleanSetting getPitchCorrecting() {
      return this.pitchCorrecting;
   }
}
