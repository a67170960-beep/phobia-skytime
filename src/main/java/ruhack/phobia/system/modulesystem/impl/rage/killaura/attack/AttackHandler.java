package ruhack.phobia.system.modulesystem.impl.rage.killaura.attack;

import lombok.Generated;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WeaponComponent;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.system.eventsystem.api.types.EventListener;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.eventsystem.impl.UsingItemEvent;
import ruhack.phobia.system.modulesystem.impl.rage.ElytraTarget;
import ruhack.phobia.system.modulesystem.impl.rage.KillAura;
import ruhack.phobia.util.math.MathUtils;
import ruhack.phobia.util.player.SimulatedPlayer;
import ruhack.phobia.util.player.swap.InventoryUtils;
import ruhack.phobia.util.rotation.RotationController;
import ruhack.phobia.util.rotation.raycast.RaytracingUtil;
import ruhack.phobia.util.string.PlayerInteractionHelper;
import ruhack.phobia.util.timer.StopWatch;

public class AttackHandler implements IMinecraft {
   private final StopWatch attackTimer = new StopWatch();
   private final StopWatch shieldWatch = new StopWatch();
   private final ClickScheduler clickScheduler = new ClickScheduler();
   private int count = 0;
   private static final int MACE_IDLE = 0;
   private static final int MACE_HOLDING = 1;
   private static final int MACE_ATTACKED = 2;
   private static final int MACE_HOLD_TIMEOUT = 30;
   private static final int SPHERE_PENDING_TIMEOUT = 3;
   private static final float SPHERE_EMERGENCY_HEALTH = 14.0F;
   private int maceStage = 0;
   private int maceStageTicks = 0;
   private int maceSlot = -1;
   private int previousMaceSlot = -1;
   private int maceTargetId = -1;
   private int pendingSphereSlot = -1;
   private int pendingSphereTicks = 0;

   void tick() {
   }

   void onPacket(PacketEvent e) {
      Packet<?> packet = e.getPacket();
      if (packet instanceof HandSwingC2SPacket || packet instanceof UpdateSelectedSlotC2SPacket) {
         this.clickScheduler.recalculate();
      }
   }

   void onUsingItem(UsingItemEvent e) {
      if (e.getType() == -1 && !this.shieldWatch.finished(50.0)) {
         e.cancel();
      }
   }

   void handleAttack(AttackPerpetrator.AttackPerpetratorConfigurable config) {
      if (!this.handleDamageSphereAttack(config)) {
         boolean elytraMode = this.checkElytraMode(config);
         if (!elytraMode || this.checkElytraRaycast(config)) {
            boolean raytrace = RaytracingUtil.rayTrace(config);
            if (this.maceStage == 1) {
               if (!this.isValidMaceSequence(config)) {
                  this.cancelMaceSequence();
               } else {
                  KillAura aura = KillAura.getInstance();
                  if (this.isMaceAttackReady(aura) && raytrace) {
                     this.attackEntity(config);
                     this.maceStage = 2;
                     this.maceStageTicks = 0;
                  }
               }
            } else if (!this.tryStartMaceSwap(config)) {
               if (raytrace && this.canAttack(config, 0)) {
                  if (this.tryShieldBreak(config)) {
                     return;
                  }

                  this.attackEntity(config);
               }
            }
         }
      }
   }

   private void applySprintReset() {
      KillAura aura = KillAura.getInstance();
      if (aura != null) {
         String mode = aura.sprintMode.getValue();
         if (!mode.equals("Не сбрасывать")) {
            if (mc.player.isSprinting() || EventListener.serverSprint) {
               if (!mc.player.isGliding() && !mc.player.isSwimming() && !mc.player.isTouchingWater()) {
                  if (mode.equals("Легит")) {
                     mc.player.setSprinting(false);
                  } else {
                     ClientPlayNetworkHandler nh = mc.player.networkHandler;
                     if (nh != null) {
                        nh.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
                        nh.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_SPRINTING));
                     }
                  }
               }
            }
         }
      }
   }

   private boolean tryShieldBreak(AttackPerpetrator.AttackPerpetratorConfigurable config) {
      if (!config.isShieldBreaker()) {
         return false;
      }

      if (!config.getTarget().isBlocking()) {
         return false;
      }

      if (mc.getNetworkHandler() == null) {
         return false;
      }

      if (this.disablesBlocking(mc.player.getMainHandStack())) {
         return false;
      }

      int axeSlot = this.findShieldBreakerSlot();
      if (axeSlot == -1) {
         return false;
      }

      int prevSlot = mc.player.getInventory().getSelectedSlot();
      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(axeSlot));
      this.attackEntity(config);
      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
      return true;
   }

   private int findShieldBreakerSlot() {
      for (int i = 0; i < 9; i++) {
         if (this.disablesBlocking(mc.player.getInventory().getStack(i))) {
            return i;
         }
      }

      return -1;
   }

   private boolean disablesBlocking(ItemStack stack) {
      WeaponComponent weapon = (WeaponComponent)stack.get(DataComponentTypes.WEAPON);
      return weapon != null && weapon.disableBlockingForSeconds() > 0.0F;
   }

   public void tickMace() {
      this.tickDamageSphere();
      if (this.maceStage != 0) {
         this.maceStageTicks++;
         KillAura aura = KillAura.getInstance();
         boolean active = aura != null && aura.isState() && aura.autoMace.isValue() && mc.player != null && mc.world != null;
         if (this.maceStage == 1) {
            boolean targetChanged = !active || aura.getTarget() == null || aura.getTarget().getId() != this.maceTargetId;
            boolean flightEnded = active && !this.isAirborneForMace();
            boolean slotChanged = active && mc.player.getInventory().getSelectedSlot() != this.maceSlot;
            if (slotChanged) {
               this.clearMaceState();
            } else if (targetChanged || flightEnded || this.maceStageTicks > 30) {
               this.restoreMaceSlot();
            }
         } else if (this.maceStage == 2 && this.maceStageTicks >= 1) {
            this.restoreMaceSlot();
         }
      }
   }

   public void resetMace() {
      if (this.maceStage != 0) {
         this.restoreMaceSlot();
      } else {
         this.clearMaceState();
      }
   }

   public void resetDamageSphere() {
      this.finishDamageSphereRestore();
   }

   public void prepareDamageSphere(AttackPerpetrator.AttackPerpetratorConfigurable config) {
      KillAura aura = KillAura.getInstance();
      if (aura != null && aura.maximumDamage.isValue() && mc.player != null) {
         if (this.pendingSphereSlot < 0 && !aura.isDamageSphereEquipped()) {
            if (RaytracingUtil.rayTrace(config)) {
               if (this.canAttack(config, 0)) {
                  int slot = aura.swapDamageSphereForAttack();
                  if (slot >= 0) {
                     this.pendingSphereSlot = slot;
                     this.pendingSphereTicks = 0;
                  }
               }
            }
         }
      }
   }

   private void tickDamageSphere() {
      if (this.pendingSphereSlot >= 0) {
         this.pendingSphereTicks++;
         KillAura aura = KillAura.getInstance();
         boolean abort = mc.player == null || aura == null || !aura.isState() || mc.player.getHealth() <= 14.0F || this.pendingSphereTicks > 3;
         if (abort) {
            this.finishDamageSphereRestore();
         }
      }
   }

   private boolean handleDamageSphereAttack(AttackPerpetrator.AttackPerpetratorConfigurable config) {
      KillAura aura = KillAura.getInstance();
      if (aura != null && aura.maximumDamage.isValue()) {
         boolean elytraMode = this.checkElytraMode(config);
         if (elytraMode && !this.checkElytraRaycast(config)) {
            if (this.pendingSphereSlot >= 0) {
               this.finishDamageSphereRestore();
            }

            return this.pendingSphereSlot >= 0;
         } else if (this.pendingSphereSlot >= 0) {
            if (!aura.isDamageSphereEquipped()) {
               return true;
            } else if (!RaytracingUtil.rayTrace(config) || !this.canAttack(config, 0)) {
               return true;
            } else if (this.tryShieldBreak(config)) {
               this.finishDamageSphereRestore();
               return true;
            } else {
               this.attackEntity(config);
               this.finishDamageSphereRestore();
               return true;
            }
         } else {
            if (aura.isDamageSphereEquipped()) {
               return false;
            }

            if (RaytracingUtil.rayTrace(config) && this.canAttack(config, 0)) {
               int slot = aura.swapDamageSphereForAttack();
               if (slot == -2) {
                  return false;
               } else if (slot == -1) {
                  return false;
               } else if (!aura.isDamageSphereEquipped()) {
                  this.pendingSphereSlot = slot;
                  this.pendingSphereTicks = 0;
                  return true;
               } else if (this.tryShieldBreak(config)) {
                  aura.restoreDamageSphereAfterAttack(slot);
                  return true;
               } else {
                  this.attackEntity(config);
                  aura.restoreDamageSphereAfterAttack(slot);
                  return true;
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private void finishDamageSphereRestore() {
      if (this.pendingSphereSlot >= 0) {
         KillAura aura = KillAura.getInstance();
         if (aura != null) {
            aura.restoreDamageSphereAfterAttack(this.pendingSphereSlot);
         }

         this.pendingSphereSlot = -1;
         this.pendingSphereTicks = 0;
      }
   }

   private void restoreMaceSlot() {
      if (mc.player != null && this.previousMaceSlot >= 0 && mc.player.getInventory().getSelectedSlot() == this.maceSlot) {
         InventoryUtils.selectSlot(this.previousMaceSlot);
      }

      this.clearMaceState();
   }

   private void cancelMaceSequence() {
      if (mc.player != null && mc.player.getInventory().getSelectedSlot() == this.maceSlot) {
         this.restoreMaceSlot();
      } else {
         this.clearMaceState();
      }
   }

   private void clearMaceState() {
      this.maceStage = 0;
      this.maceStageTicks = 0;
      this.maceSlot = -1;
      this.previousMaceSlot = -1;
      this.maceTargetId = -1;
   }

   private boolean tryStartMaceSwap(AttackPerpetrator.AttackPerpetratorConfigurable config) {
      if (this.maceStage != 0) {
         return false;
      }

      KillAura aura = KillAura.getInstance();
      if (aura == null || !aura.isState() || !aura.autoMace.isValue()) {
         return false;
      }

      if (!this.isNearMaceApex(aura, config)) {
         return false;
      }

      int selectedSlot = mc.player.getInventory().getSelectedSlot();
      int slot = mc.player.getMainHandStack().isOf(Items.MACE) ? selectedSlot : InventoryUtils.findHotbarItem(Items.MACE);
      if (slot == -1) {
         return false;
      }

      this.previousMaceSlot = aura.maceReturnSlot.isValue() && slot != selectedSlot ? selectedSlot : -1;
      this.maceSlot = slot;
      this.maceTargetId = config.getTarget().getId();
      if (slot != selectedSlot) {
         InventoryUtils.selectSlot(slot);
      }

      this.maceStage = 1;
      this.maceStageTicks = 0;
      return true;
   }

   private boolean isValidMaceSequence(AttackPerpetrator.AttackPerpetratorConfigurable config) {
      KillAura aura = KillAura.getInstance();
      return aura != null
         && aura.isState()
         && aura.autoMace.isValue()
         && this.isAirborneForMace()
         && config.getTarget().getId() == this.maceTargetId
         && mc.player.getInventory().getSelectedSlot() == this.maceSlot
         && mc.player.getMainHandStack().isOf(Items.MACE);
   }

   private boolean isNearMaceApex(KillAura aura, AttackPerpetrator.AttackPerpetratorConfigurable config) {
      if (!this.isAirborneForMace()) {
         return false;
      }

      double velocityY = mc.player.getVelocity().y;
      double availableFall = mc.player.getY() - config.getTarget().getY();
      return velocityY <= 0.2 && velocityY >= -0.3 && availableFall >= aura.maceMinFall.getValue() && mc.player.fallDistance < aura.maceMinFall.getValue();
   }

   private boolean isMaceAttackReady(KillAura aura) {
      return aura != null && this.isAirborneForMace() && mc.player.getVelocity().y < 0.0 && mc.player.fallDistance >= aura.maceMinFall.getValue();
   }

   private boolean isAirborneForMace() {
      return mc.player != null && !mc.player.isOnGround() && !mc.player.isGliding() && !mc.player.isTouchingWater() && !mc.player.isInLava();
   }

   void preAttackEntity(AttackPerpetrator.AttackPerpetratorConfigurable config) {
   }

   void attackEntity(AttackPerpetrator.AttackPerpetratorConfigurable config) {
      this.attack(config);
   }

   private void attack(AttackPerpetrator.AttackPerpetratorConfigurable config) {
      this.applySprintReset();
      mc.interactionManager.attackEntity(mc.player, config.getTarget());
      mc.player.swingHand(Hand.MAIN_HAND);
      this.attackTimer.reset();
      this.count++;
   }

   private boolean isSprinting() {
      return EventListener.serverSprint && !mc.player.isGliding() && !mc.player.isTouchingWater();
   }

   public boolean canAttack(AttackPerpetrator.AttackPerpetratorConfigurable config, int ticks) {
      for (int i = 0; i <= ticks; i++) {
         if (this.canCrit(config, i)) {
            return true;
         }
      }

      return false;
   }

   public boolean canCrit(AttackPerpetrator.AttackPerpetratorConfigurable config, int ticks) {
      if (mc.player.isUsingItem() && !mc.player.getActiveItem().getItem().equals(Items.SHIELD) && config.isEatAndAttack()) {
         return false;
      } else {
         int pidor = ticks;
         if (!this.clickScheduler.isCooldownComplete(false, pidor)) {
            return false;
         } else {
            SimulatedPlayer simulated = SimulatedPlayer.simulateLocalPlayer(ticks);
            boolean noRestrict = !this.hasMovementRestrictions(simulated);
            boolean critState = this.isPlayerInCriticalState(simulated, ticks);
            if (config.isOnlyCritical() && !mc.options.jumpKey.isPressed()) {
               return !noRestrict ? true : critState || simulated.onGround;
            } else {
               return !this.hasMovementRestrictions(simulated) ? this.isPlayerInCriticalState(simulated, ticks) : true;
            }
         }
      }
   }

   private boolean hasMovementRestrictions(SimulatedPlayer simulated) {
      return simulated.hasStatusEffect(StatusEffects.BLINDNESS)
         || simulated.hasStatusEffect(StatusEffects.LEVITATION)
         || PlayerInteractionHelper.isBoxInBlock(simulated.boundingBox.expand(-0.001), Blocks.COBWEB)
         || simulated.isSubmergedInWater()
         || simulated.isInLava()
         || simulated.isClimbing()
         || !PlayerInteractionHelper.canChangeIntoPose(EntityPose.STANDING, simulated.pos)
         || simulated.player.getAbilities().flying;
   }

   private boolean checkElytraMode(AttackPerpetrator.AttackPerpetratorConfigurable config) {
      return KillAura.getInstance().getTarget() != null
         && KillAura.getInstance().getTarget().isGliding()
         && mc.player.isGliding()
         && ElytraTarget.getInstance() != null
         && ElytraTarget.getInstance().isState();
   }

   private boolean checkElytraRaycast(AttackPerpetrator.AttackPerpetratorConfigurable config) {
      Vec3d targetVelocity = config.getTarget().getVelocity();
      float leadTicks = 0.0F;
      if (ElytraTarget.shouldElytraTarget) {
         leadTicks = ElytraTarget.getInstance().elytraForward.getValue();
      }

      Vec3d predictedPos = config.getTarget().getEntityPos().add(targetVelocity.multiply(leadTicks));
      Box predictedBox = new Box(
         predictedPos.x - config.getTarget().getWidth() / 2.0F,
         predictedPos.y,
         predictedPos.z - config.getTarget().getWidth() / 2.0F,
         predictedPos.x + config.getTarget().getWidth() / 2.0F,
         predictedPos.y + config.getTarget().getHeight(),
         predictedPos.z + config.getTarget().getWidth() / 2.0F
      );
      Vec3d eyePos = mc.player.getEyePos();
      Vec3d lookVec = RotationController.INSTANCE.getRotation().toVector();
      return predictedBox.raycast(eyePos, eyePos.add(lookVec.multiply(config.getMaximumRange()))).isPresent();
   }

   private boolean isPlayerInCriticalState(SimulatedPlayer simulated, int ticks) {
      float falldistance = 0.0F;
      if (KillAura.getInstance().getAimType().isSelected("d")) {
         falldistance = MathUtils.getRandom(0.03F, 0.07F);
      }

      boolean fall = simulated.fallDistance > falldistance && (simulated.fallDistance < 0.08 || !SimulatedPlayer.simulateLocalPlayer(ticks).onGround);
      return !simulated.onGround && fall;
   }

   @Generated
   public void setCount(int count) {
      this.count = count;
   }

   @Generated
   public void setMaceStage(int maceStage) {
      this.maceStage = maceStage;
   }

   @Generated
   public void setMaceStageTicks(int maceStageTicks) {
      this.maceStageTicks = maceStageTicks;
   }

   @Generated
   public void setMaceSlot(int maceSlot) {
      this.maceSlot = maceSlot;
   }

   @Generated
   public void setPreviousMaceSlot(int previousMaceSlot) {
      this.previousMaceSlot = previousMaceSlot;
   }

   @Generated
   public void setMaceTargetId(int maceTargetId) {
      this.maceTargetId = maceTargetId;
   }

   @Generated
   public void setPendingSphereSlot(int pendingSphereSlot) {
      this.pendingSphereSlot = pendingSphereSlot;
   }

   @Generated
   public void setPendingSphereTicks(int pendingSphereTicks) {
      this.pendingSphereTicks = pendingSphereTicks;
   }

   @Generated
   public StopWatch getAttackTimer() {
      return this.attackTimer;
   }

   @Generated
   public StopWatch getShieldWatch() {
      return this.shieldWatch;
   }

   @Generated
   public ClickScheduler getClickScheduler() {
      return this.clickScheduler;
   }

   @Generated
   public int getCount() {
      return this.count;
   }

   @Generated
   public int getMaceStage() {
      return this.maceStage;
   }

   @Generated
   public int getMaceStageTicks() {
      return this.maceStageTicks;
   }

   @Generated
   public int getMaceSlot() {
      return this.maceSlot;
   }

   @Generated
   public int getPreviousMaceSlot() {
      return this.previousMaceSlot;
   }

   @Generated
   public int getMaceTargetId() {
      return this.maceTargetId;
   }

   @Generated
   public int getPendingSphereSlot() {
      return this.pendingSphereSlot;
   }

   @Generated
   public int getPendingSphereTicks() {
      return this.pendingSphereTicks;
   }
}
