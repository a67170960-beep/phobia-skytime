package ruhack.phobia.util.rotation;

import lombok.Generated;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.eventsystem.impl.PlayerVelocityStrafeEvent;
import ruhack.phobia.system.eventsystem.impl.RotationUpdateEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.util.math.TaskPriority;
import ruhack.phobia.util.math.TaskProcessor;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;

public class RotationController implements IMinecraft {
   public static RotationController INSTANCE = new RotationController();
   private RotationPlan lastRotationPlan;
   private final TaskProcessor<RotationPlan> rotationPlanTaskProcessor = new TaskProcessor<>();
   private Angle currentAngle;
   private Angle previousAngle;
   private Angle serverAngle = Angle.DEFAULT;

   public void init() {
      Initialization.getInstance().getManager().getEventManager();
      EventManager.register(this);
   }

   public void setRotation(Angle value) {
      if (value == null) {
         this.previousAngle = this.currentAngle != null ? this.currentAngle : AngleUtil.cameraAngle();
      } else {
         this.previousAngle = this.currentAngle;
      }

      this.currentAngle = value;
   }

   public Angle getRotation() {
      return this.currentAngle != null ? this.currentAngle : AngleUtil.cameraAngle();
   }

   public Angle getPreviousRotation() {
      return this.currentAngle != null && this.previousAngle != null ? this.previousAngle : new Angle(mc.player.lastYaw, mc.player.lastPitch);
   }

   public Angle getMoveRotation() {
      RotationPlan rotationPlan = this.getCurrentRotationPlan();
      return this.currentAngle != null && rotationPlan != null && rotationPlan.isMoveCorrection() ? this.currentAngle : AngleUtil.cameraAngle();
   }

   public RotationPlan getCurrentRotationPlan() {
      return this.rotationPlanTaskProcessor.fetchActiveTaskValue() != null ? this.rotationPlanTaskProcessor.fetchActiveTaskValue() : this.lastRotationPlan;
   }

   public void rotateTo(
      Angle.VecRotation vecRotation, LivingEntity entity, int reset, RotationConfig configurable, TaskPriority taskPriority, ModuleStructure provider
   ) {
      this.rotateTo(configurable.createRotationPlan(vecRotation.getAngle(), vecRotation.getVec(), entity, reset), taskPriority, provider);
   }

   public void rotateTo(Angle angle, int reset, RotationConfig configurable, TaskPriority taskPriority, ModuleStructure provider) {
      this.rotateTo(configurable.createRotationPlan(angle, angle.toVector(), null, reset), taskPriority, provider);
   }

   public void rotateTo(Angle angle, RotationConfig configurable, TaskPriority taskPriority, ModuleStructure provider) {
      this.rotateTo(configurable.createRotationPlan(angle, angle.toVector(), null, 1), taskPriority, provider);
   }

   public void rotateTo(RotationPlan plan, TaskPriority taskPriority, ModuleStructure provider) {
      this.rotationPlanTaskProcessor.addTask(new TaskProcessor.Task<>(1, taskPriority.getPriority(), provider, plan));
   }

   public void update() {
      RotationPlan activePlan = this.getCurrentRotationPlan();
      if (activePlan != null) {
         Angle clientAngle = new Angle(mc.player.getYaw(), mc.player.getPitch());
         if (this.lastRotationPlan != null) {
            double differenceFromCurrentToPlayer = computeRotationDifference(this.serverAngle, clientAngle);
            if (activePlan.getTicksUntilReset() <= this.rotationPlanTaskProcessor.tickCounter && differenceFromCurrentToPlayer < 1.0) {
               mc.player.setYaw(this.getRotation().getYaw());
               mc.player.setPitch(this.getRotation().getPitch());
               this.setRotation(null);
               this.lastRotationPlan = null;
               this.rotationPlanTaskProcessor.tickCounter = 0;
               return;
            }
         }

         Angle newAngle = activePlan.nextRotation(
               this.currentAngle != null ? this.currentAngle : clientAngle, this.rotationPlanTaskProcessor.fetchActiveTaskValue() == null
            )
            .adjustSensitivity();
         this.setRotation(newAngle);
         this.lastRotationPlan = activePlan;
         this.rotationPlanTaskProcessor.tick(1);
      }
   }

   public static double computeRotationDifference(Angle a, Angle b) {
      return Math.hypot(Math.abs(computeAngleDifference(a.getYaw(), b.getYaw())), Math.abs(a.getPitch() - b.getPitch()));
   }

   public static float computeAngleDifference(float a, float b) {
      return MathHelper.wrapDegrees(a - b);
   }

   private Vec3d fixVelocity(Vec3d currVelocity, Vec3d movementInput, float speed) {
      if (this.currentAngle != null) {
         float yaw = this.currentAngle.getYaw();
         double d = movementInput.lengthSquared();
         if (d < 1.0E-7) {
            return Vec3d.ZERO;
         }

         Vec3d vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).multiply(speed);
         float f = MathHelper.sin(yaw * (float) (Math.PI / 180.0));
         float g = MathHelper.cos(yaw * (float) (Math.PI / 180.0));
         return new Vec3d(vec3d.getX() * g - vec3d.getZ() * f, vec3d.getY(), vec3d.getZ() * g + vec3d.getX() * f);
      } else {
         return currVelocity;
      }
   }

   public void clear() {
      this.rotationPlanTaskProcessor.activeTasks.clear();
   }

   public void forceStop() {
      this.rotationPlanTaskProcessor.activeTasks.clear();
      this.rotationPlanTaskProcessor.tickCounter = 0;
      this.lastRotationPlan = null;
      this.currentAngle = null;
      this.previousAngle = null;
   }

   @EventHandler
   public void onPlayerVelocityStrafe(PlayerVelocityStrafeEvent e) {
      RotationPlan currentRotationPlan = this.getCurrentRotationPlan();
      if (currentRotationPlan != null && currentRotationPlan.isMoveCorrection()) {
         e.setVelocity(this.fixVelocity(e.getVelocity(), e.getMovementInput(), e.getSpeed()));
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      EventManager.callEvent(new RotationUpdateEvent((byte)0));
      this.update();
      EventManager.callEvent(new RotationUpdateEvent((byte)2));
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (!event.isCancelled()) {
         Object var4 = event.getPacket();
         switch (var4) {
            case PlayerMoveC2SPacket player when player.changesLook():
               this.serverAngle = new Angle(player.getYaw(1.0F), player.getPitch(1.0F));
               break;
            case PlayerPositionLookS2CPacket var6:
               PlayerPositionLookS2CPacket player = (PlayerPositionLookS2CPacket)var4;
               this.serverAngle = new Angle(player.change().yaw(), player.change().pitch());
               break;
            default:
         }
      }
   }

   @Generated
   public RotationPlan getLastRotationPlan() {
      return this.lastRotationPlan;
   }

   @Generated
   public TaskProcessor<RotationPlan> getRotationPlanTaskProcessor() {
      return this.rotationPlanTaskProcessor;
   }

   @Generated
   public Angle getCurrentAngle() {
      return this.currentAngle;
   }

   @Generated
   public Angle getPreviousAngle() {
      return this.previousAngle;
   }

   @Generated
   public Angle getServerAngle() {
      return this.serverAngle;
   }
}
