package ruhack.phobia.system.modulesystem.impl.movement;

import net.minecraft.client.MinecraftClient;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.impl.rage.KillAura;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.istance.Instance;
import ruhack.phobia.util.math.TaskPriority;
import ruhack.phobia.util.player.MovingUtil;
import ruhack.phobia.util.rotation.RotationConfig;
import ruhack.phobia.util.rotation.RotationController;
import ruhack.phobia.util.rotation.angle.Angle;

public class Strafe extends ModuleStructure {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public SelectSetting mode = new SelectSetting("Режим", "Выберите тип стрейфов", "Matrix", "Matrix", "Grim");
   SliderSettings speed = new SliderSettings("Скорость", "Выберите скорость для стрейфа", 0.42F)
      .range(0.0F, 1.0F)
      .visible(() -> this.mode.isSelected("Matrix"));
   private float lastYaw;
   private float lastPitch;
   private final Angle rot = new Angle(0.0F, 0.0F);

   public Strafe() {
      super("Strafe", "Позволяет игроку стрейфить", ModuleCategory.MOVEMENT);
      this.settings(this.mode, this.speed);
   }

   public static Strafe getInstance() {
      return Instance.get(Strafe.class);
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         boolean moving = MovingUtil.hasPlayerMovement();
         float yaw = mc.player.getYaw();
         if (this.mode.isSelected("Matrix")) {
            this.handleMatrixMode(moving, yaw);
         } else if (this.mode.isSelected("Grim")) {
            this.handleGrimMode(moving, yaw);
         }

         this.lastYaw = yaw;
         this.lastPitch = 0.0F;
      }
   }

   private void handleMatrixMode(boolean moving, float yaw) {
      if (moving) {
         yaw = MovingUtil.moveYaw(mc.player.getYaw());
         double motion = this.speed.getValue() * 1.5F;
         MovingUtil.setVelocity(motion);
      } else {
         MovingUtil.setVelocity(0.0);
      }

      mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z);
   }

   private void handleGrimMode(boolean moving, float yaw) {
      if (moving) {
         RotationConfig.freeCorrection = true;
         yaw = MovingUtil.moveYaw(mc.player.getYaw());
         this.rot.setYaw(yaw);
         this.rot.setPitch(mc.player.getPitch());
         if (KillAura.getInstance().getTarget() == null) {
            RotationController.INSTANCE.rotateTo(this.rot, RotationConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
         }
      }
   }

   @Override
   public void activate() {
      super.activate();
      this.lastYaw = mc.player != null ? mc.player.getYaw() : 0.0F;
      this.lastPitch = mc.player != null ? mc.player.getPitch() : 0.0F;
   }
}
