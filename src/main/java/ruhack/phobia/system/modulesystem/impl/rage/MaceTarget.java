package ruhack.phobia.system.modulesystem.impl.rage;

import lombok.Generated;
import net.minecraft.entity.LivingEntity;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.InputEvent;
import ruhack.phobia.system.eventsystem.impl.RotationUpdateEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic.LinearSmoothMode;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.target.TargetSelector;
import ruhack.phobia.system.modulesystem.impl.rage.macetarget.armor.ArmorSwapHandler;
import ruhack.phobia.system.modulesystem.impl.rage.macetarget.armor.FireworkHandler;
import ruhack.phobia.system.modulesystem.impl.rage.macetarget.attack.AttackHandler;
import ruhack.phobia.system.modulesystem.impl.rage.macetarget.flight.FlightController;
import ruhack.phobia.system.modulesystem.impl.rage.macetarget.prediction.TargetPredictor;
import ruhack.phobia.system.modulesystem.impl.rage.macetarget.stage.StageHandler;
import ruhack.phobia.system.modulesystem.impl.rage.macetarget.state.MaceState;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.istance.Instance;
import ruhack.phobia.util.math.TaskPriority;
import ruhack.phobia.util.player.swap.InventoryUtils;
import ruhack.phobia.util.player.swap.SwapSettings;
import ruhack.phobia.util.rotation.RotationConfig;
import ruhack.phobia.util.rotation.RotationController;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.timer.StopWatch;

public class MaceTarget extends ModuleStructure {
   private final SelectSetting serverMode = new SelectSetting("Сервер", "Режим работы под сервер", "Default", "ReallyWorld").selected("Default");
   private final SelectSetting modeSetting = new SelectSetting("Режим", "Способ свапа", "Silent", "Silent", "Legit").selected("Silent");
   private final SliderSettings height = new SliderSettings("Высота", "Высота полёта над целью", 20.0F).range(20.0F, 60.0F);
   private final MultiSelectSetting targetType = new MultiSelectSetting("Цели", "Типы целей").value("Игроки", "Мобы", "Животные").selected("Игроки");
   private final BooleanSetting autoEquipChest = new BooleanSetting("Авто-нагрудник", "Одевать нагрудник при выключении").setValue(true);
   private final BooleanSetting predictMovement = new BooleanSetting("Предугадывание", "Предугадывать позицию убегающей цели").setValue(true);
   private final TargetPredictor predictor = new TargetPredictor();
   private final FlightController flightController;
   private final ArmorSwapHandler armorSwapHandler;
   private final FireworkHandler fireworkHandler;
   private final AttackHandler attackHandler = new AttackHandler();
   private final StageHandler stageHandler;
   private final TargetSelector targetFinder = new TargetSelector();
   private final StopWatch fireworkTimer = new StopWatch();
   private LivingEntity target;

   public static MaceTarget getInstance() {
      return Instance.get(MaceTarget.class);
   }

   public MaceTarget() {
      super("MaceTarget", "Mace Target", ModuleCategory.RAGE);
      this.settings(this.serverMode, this.modeSetting, this.height, this.targetType, this.autoEquipChest, this.predictMovement);
      this.flightController = new FlightController(this.predictor);
      this.armorSwapHandler = new ArmorSwapHandler(this::buildSettings);
      this.fireworkHandler = new FireworkHandler(this::buildSettings);
      this.stageHandler = new StageHandler(this.armorSwapHandler, this.fireworkHandler, this.attackHandler, this.fireworkTimer);
   }

   private boolean isSilentMode() {
      return this.modeSetting.getValue().equals("Silent");
   }

   private boolean isReallyWorldMode() {
      return this.serverMode.getValue().equals("ReallyWorld");
   }

   private SwapSettings buildSettings() {
      return this.isSilentMode() ? SwapSettings.instant() : SwapSettings.legit();
   }

   private void updateHandlers() {
      this.stageHandler.setSilentMode(this.isSilentMode());
      this.stageHandler.setReallyWorldMode(this.isReallyWorldMode());
      this.stageHandler.setHeight(this.height.getValue());
      this.flightController.setPredictionEnabled(this.predictMovement.isValue());
      this.flightController.setHeight(this.height.getValue());
   }

   @Override
   public void activate() {
      this.stageHandler.reset();
      this.target = null;
      this.attackHandler.reset();
      this.armorSwapHandler.reset();
      this.fireworkHandler.reset();
      this.predictor.reset();
      this.fireworkTimer.reset();
   }

   @Override
   public void deactivate() {
      if (this.autoEquipChest.isValue() && mc.player != null) {
         this.equipChestplateOnDisable();
      }

      this.attackHandler.releaseMace();
      this.armorSwapHandler.forceRestore();
      this.fireworkHandler.forceRestore();
      this.target = null;
      this.targetFinder.releaseTarget();
      this.armorSwapHandler.reset();
      this.fireworkHandler.reset();
      this.predictor.reset();
      RotationController.INSTANCE.clear();
   }

   private void equipChestplateOnDisable() {
      if (InventoryUtils.hasElytra()) {
         int slot = InventoryUtils.findChestArmorSlot();
         if (slot != -1) {
            int wrappedSlot = InventoryUtils.wrapSlot(slot);
            InventoryUtils.swap(wrappedSlot, 6);
            InventoryUtils.closeScreen();
         }
      }
   }

   @EventHandler
   public void onRotationUpdate(RotationUpdateEvent event) {
      if (mc.player != null && mc.world != null) {
         if (event.getType() == 0) {
            this.updateHandlers();
            if (this.target == null || !this.target.isAlive()) {
               this.findTarget();
            }

            if (this.target == null) {
               return;
            }

            this.predictor.update(this.target);
            MaceState.Stage currentStage = this.stageHandler.getStage();
            switch (currentStage) {
               case FLYING_UP:
                  if (InventoryUtils.hasElytra() && mc.player.isGliding()) {
                     Angle targetAnglex = this.flightController.calculateAngle(this.target, currentStage);
                     this.rotateTo(targetAnglex);
                  }
                  break;
               case TARGETTING:
               case ATTACKING:
                  Angle targetAngle = this.flightController.calculateAngle(this.target, currentStage);
                  this.rotateTo(targetAngle);
            }
         }

         if (event.getType() == 2) {
            this.handlePostRotation();
         }
      }
   }

   private void handlePostRotation() {
      if (this.target != null && this.target.isAlive()) {
         if (this.stageHandler.getStage() == MaceState.Stage.ATTACKING) {
            if (!this.armorSwapHandler.isActive() && !this.fireworkHandler.isActive()) {
               if (!InventoryUtils.hasElytra() || !mc.player.isGliding()) {
                  if (this.attackHandler.shouldAttack(this.target)) {
                     this.attackHandler.performAttack(this.target);
                     if (this.isReallyWorldMode()) {
                        this.setState(false);
                     } else {
                        this.stageHandler.setStage(MaceState.Stage.FLYING_UP);
                        this.fireworkTimer.reset();
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (!this.isSilentMode()) {
            this.armorSwapHandler.processLoop();
            this.fireworkHandler.processLoop();
         }

         if (!this.armorSwapHandler.isActive() && !this.fireworkHandler.isActive()) {
            if (this.target != null && this.target.isAlive()) {
               this.processStage();
            }
         }
      } else {
         this.resetAllStates();
      }
   }

   private void processStage() {
      boolean hasElytra = InventoryUtils.hasElytra();
      MaceState.Stage currentStage = this.stageHandler.getStage();
      switch (currentStage) {
         case FLYING_UP:
            this.stageHandler.handleFlyingUp(this.target, hasElytra);
            break;
         case TARGETTING:
            this.stageHandler.handleTargetting(this.target);
            break;
         case ATTACKING:
            this.stageHandler.handleAttacking(this.target, hasElytra);
            break;
         case PREPARE:
            this.stageHandler.handlePrepare(hasElytra);
      }
   }

   @EventHandler
   public void onInput(InputEvent event) {
      if (mc.player != null) {
         if (this.armorSwapHandler.getMovement().isBlocked() || this.fireworkHandler.getMovement().isBlocked()) {
            event.setDirectionalLow(false, false, false, false);
            event.setJumping(false);
         }

         if (this.target != null && InventoryUtils.hasElytra() && this.stageHandler.getStage() == MaceState.Stage.FLYING_UP) {
            if (mc.player.isOnGround()) {
               event.setJumping(true);
            } else if (!mc.player.isGliding() && !mc.player.getAbilities().flying) {
               event.setJumping(mc.player.age % 2 == 0);
            }
         }
      }
   }

   private void findTarget() {
      TargetSelector.EntityFilter filter = new TargetSelector.EntityFilter(this.targetType.getSelected());
      this.targetFinder.searchTargets(mc.world.getEntities(), 128.0F, 360.0F, true);
      this.targetFinder.validateTarget(filter::isValid);
      this.target = this.targetFinder.getCurrentTarget();
   }

   private void rotateTo(Angle angle) {
      RotationConfig config = new RotationConfig(new LinearSmoothMode(), true, false, false);
      Angle.VecRotation rotation = new Angle.VecRotation(angle, angle.toVector());
      RotationController.INSTANCE.rotateTo(rotation, this.target, 1, config, TaskPriority.HIGH_IMPORTANCE_1, this);
   }

   private void resetAllStates() {
      this.armorSwapHandler.reset();
      this.fireworkHandler.reset();
      this.attackHandler.reset();
      this.predictor.reset();
      this.target = null;
      this.stageHandler.reset();
   }

   @Generated
   public SelectSetting getServerMode() {
      return this.serverMode;
   }

   @Generated
   public SelectSetting getModeSetting() {
      return this.modeSetting;
   }

   @Generated
   public SliderSettings getHeight() {
      return this.height;
   }

   @Generated
   public MultiSelectSetting getTargetType() {
      return this.targetType;
   }

   @Generated
   public BooleanSetting getAutoEquipChest() {
      return this.autoEquipChest;
   }

   @Generated
   public BooleanSetting getPredictMovement() {
      return this.predictMovement;
   }

   @Generated
   public TargetPredictor getPredictor() {
      return this.predictor;
   }

   @Generated
   public FlightController getFlightController() {
      return this.flightController;
   }

   @Generated
   public ArmorSwapHandler getArmorSwapHandler() {
      return this.armorSwapHandler;
   }

   @Generated
   public FireworkHandler getFireworkHandler() {
      return this.fireworkHandler;
   }

   @Generated
   public AttackHandler getAttackHandler() {
      return this.attackHandler;
   }

   @Generated
   public StageHandler getStageHandler() {
      return this.stageHandler;
   }

   @Generated
   public TargetSelector getTargetFinder() {
      return this.targetFinder;
   }

   @Generated
   public StopWatch getFireworkTimer() {
      return this.fireworkTimer;
   }

   @Generated
   public LivingEntity getTarget() {
      return this.target;
   }
}
