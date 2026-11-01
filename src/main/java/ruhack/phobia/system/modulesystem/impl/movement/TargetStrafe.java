package ruhack.phobia.system.modulesystem.impl.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.InputEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.impl.legit.LegitAura;
import ruhack.phobia.system.modulesystem.impl.rage.KillAura;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.istance.Instance;
import ruhack.phobia.util.rotation.RotationController;

public class TargetStrafe extends ModuleStructure {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public SelectSetting mode = new SelectSetting("Режим", "Тип стрейфа", "Matrix", "Matrix", "Grim");
   SelectSetting type = new SelectSetting("Точка ходьбы", "Выбирете точку куда будет идти стрейф", "Cube", "Cube", "Center", "Circle")
      .visible(() -> this.mode.isSelected("Grim"));
   SelectSetting typeMatrix = new SelectSetting("Точка для обхода", "Выберите точку обхода в режиме Matrix", "Cube", "Cube", "Circle")
      .visible(() -> this.mode.isSelected("Matrix"));
   SliderSettings grimRadius = new SliderSettings("Радиус обхода", "Радиус обхода вокруг цели", 0.87F)
      .range(0.1F, 1.5F)
      .visible(() -> this.mode.isSelected("Grim") && (this.type.isSelected("Cube") || this.type.isSelected("Circle")));
   MultiSelectSetting setting = new MultiSelectSetting("Настройки", "Позволяет настроить работу стрейфов")
      .value("Auto Jump", "Only Key Pressed", "In front of the target", "Direction Mode")
      .selected("Auto Jump");
   SelectSetting directionMode = new SelectSetting("Направление", "Выберите направление обхода", "Clockwise", "Clockwise", "Counterclockwise", "Random")
      .visible(() -> this.setting.isSelected("Direction Mode"));
   SliderSettings radius = new SliderSettings("Радиус", "Радиус обхода вокруг цели", 2.5F).range(0.1F, 7.0F).visible(() -> this.mode.isSelected("Matrix"));
   SliderSettings speed = new SliderSettings("Скорость", "Скорость стрейфа", 0.3F).range(0.1F, 1.0F).visible(() -> this.mode.isSelected("Matrix"));
   private int grimPointIndex = 0;

   public TargetStrafe() {
      super("TargetStrafe", "Стрейфится вокруг таргета киллауры", ModuleCategory.MOVEMENT);
      this.settings(this.mode, this.type, this.typeMatrix, this.grimRadius, this.radius, this.speed, this.setting, this.directionMode);
   }

   public static TargetStrafe getInstance() {
      return Instance.get(TargetStrafe.class);
   }

   @EventHandler
   public void onInput(InputEvent event) {
      if (mc.player != null && mc.world != null) {
         LivingEntity target = KillAura.getInstance().getTarget();
         if (!KillAura.getInstance().isState()) {
            target = LegitAura.getInstance().getTarget();
         }

         if (target != null && target.isAlive()) {
            if (this.mode.isSelected("Grim")) {
               if (KillAura.getInstance().getTarget() == null
                  || !Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler().canAttack(KillAura.getInstance().getConfig(), 1)) {
                  if (LegitAura.getInstance().getTarget() == null
                     || !Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler().canAttack(LegitAura.getInstance().getConfig(), 1)) {
                     if (!this.setting.isSelected("Only Key Pressed")
                        || mc.options.forwardKey.isPressed()
                        || mc.options.backKey.isPressed()
                        || mc.options.leftKey.isPressed()
                        || mc.options.rightKey.isPressed()) {
                        Vec3d nextPoint = this.calculateGrimNextPoint(target);
                        this.applyGrimMovement(event, nextPoint);
                     }
                  }
               }
            }
         }
      }
   }

   private Vec3d calculateGrimNextPoint(LivingEntity target) {
      Vec3d playerPos = mc.player.getEntityPos();
      Vec3d targetPos = target.getEntityPos();
      double r = this.grimRadius.getValue();
      int directionMultiplier = this.getDirectionMultiplier();
      return this.setting.isSelected("In front of the target")
         ? this.calculateFrontPoint(target, targetPos, r, directionMultiplier)
         : this.calculateNormalPoint(playerPos, targetPos, r, directionMultiplier);
   }

   private Vec3d calculateFrontPoint(LivingEntity target, Vec3d targetPos, double r, int directionMultiplier) {
      float targetYaw = target.getYaw();
      if (this.type.isSelected("Center")) {
         return targetPos.add(
            -Math.sin(Math.toRadians(targetYaw)) * r * directionMultiplier, 0.0, Math.cos(Math.toRadians(targetYaw)) * r * directionMultiplier
         );
      }

      double offset = Math.cos(System.currentTimeMillis() / 500.0) * r * directionMultiplier;
      return targetPos.add(
         -Math.sin(Math.toRadians(targetYaw)) * r + Math.cos(Math.toRadians(targetYaw)) * offset,
         0.0,
         Math.cos(Math.toRadians(targetYaw)) * r + Math.sin(Math.toRadians(targetYaw)) * offset
      );
   }

   private Vec3d calculateNormalPoint(Vec3d playerPos, Vec3d targetPos, double r, int directionMultiplier) {
      if (this.type.isSelected("Cube")) {
         Vec3d[] points = new Vec3d[]{
            new Vec3d(targetPos.x - r, playerPos.y, targetPos.z - r),
            new Vec3d(targetPos.x - r, playerPos.y, targetPos.z + r),
            new Vec3d(targetPos.x + r, playerPos.y, targetPos.z + r),
            new Vec3d(targetPos.x + r, playerPos.y, targetPos.z - r)
         };
         if (playerPos.distanceTo(points[this.grimPointIndex]) < 0.5) {
            this.grimPointIndex = (this.grimPointIndex + directionMultiplier + points.length) % points.length;
         }

         return points[this.grimPointIndex];
      } else if (this.type.isSelected("Circle")) {
         double baseAngle = System.currentTimeMillis() % 3600L / 3600.0 * 4.0 * Math.PI;
         double angle = directionMultiplier > 0 ? baseAngle : (Math.PI * 2) - baseAngle;
         return new Vec3d(targetPos.x + Math.cos(angle) * r, playerPos.y, targetPos.z + Math.sin(angle) * r);
      } else {
         return new Vec3d(targetPos.x, playerPos.y, targetPos.z);
      }
   }

   private void applyGrimMovement(InputEvent event, Vec3d nextPoint) {
      Vec3d playerPos = mc.player.getEntityPos();
      Vec3d direction = nextPoint.subtract(playerPos).normalize();
      float yaw = RotationController.INSTANCE.getRotation().getYaw();
      float movementAngle = (float)Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
      float angleDiff = MathHelper.wrapDegrees(movementAngle - yaw);
      boolean forward = false;
      boolean back = false;
      boolean left = false;
      boolean right = false;
      if (angleDiff >= -22.5 && angleDiff < 22.5) {
         forward = true;
      } else if (angleDiff >= 22.5 && angleDiff < 67.5) {
         forward = true;
         right = true;
      } else if (angleDiff >= 67.5 && angleDiff < 112.5) {
         right = true;
      } else if (angleDiff >= 112.5 && angleDiff < 157.5) {
         back = true;
         right = true;
      } else if (angleDiff >= -67.5 && angleDiff < -22.5) {
         forward = true;
         left = true;
      } else if (angleDiff >= -112.5 && angleDiff < -67.5) {
         left = true;
      } else if (angleDiff >= -157.5 && angleDiff < -112.5) {
         back = true;
         left = true;
      } else {
         back = true;
      }

      event.setDirectionalLow(forward, back, left, right);
      if (this.setting.isSelected("Auto Jump") && mc.player.isOnGround()) {
         event.setJumping(true);
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         LivingEntity target = KillAura.getInstance().getTarget();
         if (target != null && target.isAlive()) {
            if (this.mode.isSelected("Matrix")) {
               if (!this.setting.isSelected("Only Key Pressed")
                  || mc.options.forwardKey.isPressed()
                  || mc.options.backKey.isPressed()
                  || mc.options.leftKey.isPressed()
                  || mc.options.rightKey.isPressed()) {
                  if (this.setting.isSelected("Auto Jump") && mc.player.isOnGround()) {
                     mc.player.jump();
                  }

                  this.processMatrixStrafe(target);
               }
            }
         }
      }
   }

   private void processMatrixStrafe(LivingEntity target) {
      Vec3d playerPos = mc.player.getEntityPos();
      Vec3d targetPos = target.getEntityPos();
      double r = this.radius.getValue();
      int directionMultiplier = this.getDirectionMultiplier();
      if (this.setting.isSelected("In front of the target")) {
         this.processMatrixFrontStrafe(target, playerPos, targetPos, r, directionMultiplier);
      } else {
         if (this.typeMatrix.isSelected("Cube")) {
            this.processMatrixCubeStrafe(playerPos, targetPos, r, directionMultiplier);
         } else if (this.typeMatrix.isSelected("Circle")) {
            this.processMatrixCircleStrafe(playerPos, targetPos, r, directionMultiplier);
         }
      }
   }

   private int getDirectionMultiplier() {
      int directionMultiplier = 1;
      if (this.setting.isSelected("Direction Mode")) {
         if (this.directionMode.isSelected("Counterclockwise")) {
            directionMultiplier = -1;
         } else if (this.directionMode.isSelected("Random")) {
            long time = System.currentTimeMillis() / 3000L;
            directionMultiplier = time % 2L == 0L ? 1 : -1;
         }
      }

      return directionMultiplier;
   }

   private void processMatrixFrontStrafe(LivingEntity target, Vec3d playerPos, Vec3d targetPos, double r, int directionMultiplier) {
      float targetYaw = target.getYaw();
      double x = targetPos.x - Math.sin(Math.toRadians(targetYaw)) * r * directionMultiplier;
      double z = targetPos.z + Math.cos(Math.toRadians(targetYaw)) * r * directionMultiplier;
      float yaw = (float)Math.toDegrees(Math.atan2(z - playerPos.z, x - playerPos.x)) - 90.0F;
      double motionSpeed = this.speed.getValue();
      mc.player.setVelocity(-Math.sin(Math.toRadians(yaw)) * motionSpeed, mc.player.getVelocity().y, Math.cos(Math.toRadians(yaw)) * motionSpeed);
   }

   private void processMatrixCubeStrafe(Vec3d playerPos, Vec3d targetPos, double r, int directionMultiplier) {
      Vec3d[] points = new Vec3d[]{
         new Vec3d(targetPos.x - r, playerPos.y, targetPos.z - r),
         new Vec3d(targetPos.x - r, playerPos.y, targetPos.z + r),
         new Vec3d(targetPos.x + r, playerPos.y, targetPos.z + r),
         new Vec3d(targetPos.x + r, playerPos.y, targetPos.z - r)
      };
      if (playerPos.distanceTo(points[this.grimPointIndex]) < 0.5) {
         this.grimPointIndex = (this.grimPointIndex + directionMultiplier + points.length) % points.length;
      }

      Vec3d nextPoint = points[this.grimPointIndex];
      Vec3d dirVec = nextPoint.subtract(playerPos).normalize();
      float yaw = (float)Math.toDegrees(Math.atan2(dirVec.z, dirVec.x)) - 90.0F;
      double motionSpeed = this.speed.getValue();
      mc.player.setVelocity(-Math.sin(Math.toRadians(yaw)) * motionSpeed, mc.player.getVelocity().y, Math.cos(Math.toRadians(yaw)) * motionSpeed);
   }

   private void processMatrixCircleStrafe(Vec3d playerPos, Vec3d targetPos, double r, int directionMultiplier) {
      double angle = Math.atan2(playerPos.z - targetPos.z, playerPos.x - targetPos.x);
      angle += directionMultiplier * this.speed.getValue() / Math.max(playerPos.distanceTo(targetPos), r);
      double x = targetPos.x + r * Math.cos(angle);
      double z = targetPos.z + r * Math.sin(angle);
      float yaw = (float)Math.toDegrees(Math.atan2(z - playerPos.z, x - playerPos.x)) - 90.0F;
      double motionSpeed = this.speed.getValue();
      mc.player.setVelocity(-Math.sin(Math.toRadians(yaw)) * motionSpeed, mc.player.getVelocity().y, Math.cos(Math.toRadians(yaw)) * motionSpeed);
   }

   @Override
   public void activate() {
      super.activate();
      this.grimPointIndex = 0;
   }
}
