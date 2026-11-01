package ruhack.phobia.system.modulesystem.impl.rage.macetarget.armor;

import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import ruhack.phobia.system.modulesystem.impl.rage.macetarget.state.MaceState;
import ruhack.phobia.util.player.swap.InventoryUtils;
import ruhack.phobia.util.player.swap.MovementController;
import ruhack.phobia.util.player.swap.SwapSettings;

public class ArmorSwapHandler {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private final MovementController movement = new MovementController();
   private MaceState.SwapPhase phase = MaceState.SwapPhase.IDLE;
   private int slot = -1;
   private long phaseStartTime = 0L;
   private int currentDelay = 0;
   private final ArmorSwapHandler.SwapSettingsProvider settingsProvider;

   public ArmorSwapHandler(ArmorSwapHandler.SwapSettingsProvider settingsProvider) {
      this.settingsProvider = settingsProvider;
   }

   public boolean isActive() {
      return this.phase != MaceState.SwapPhase.IDLE;
   }

   public void startSwap(int slot, boolean isSilent) {
      if (isSilent) {
         this.swapSilent(slot);
      } else {
         this.startLegit(slot);
      }
   }

   private void swapSilent(int slot) {
      int wrappedSlot = InventoryUtils.wrapSlot(slot);
      InventoryUtils.swap(wrappedSlot, 6);
      InventoryUtils.closeScreen();
   }

   private void startLegit(int slot) {
      this.slot = slot;
      SwapSettings settings = this.settingsProvider.get();
      if (settings.shouldStopMovement()) {
         this.startPhase(MaceState.SwapPhase.PRE_STOP, settings.randomPreStopDelay());
      } else {
         this.startPhase(MaceState.SwapPhase.DO_SWAP, 0);
      }
   }

   public void processLoop() {
      if (this.phase != MaceState.SwapPhase.IDLE) {
         boolean continueProcessing = true;

         for (int iterations = 0; continueProcessing && iterations < 10; continueProcessing = this.processTick()) {
            iterations++;
         }
      }
   }

   private boolean processTick() {
      if (mc.player == null) {
         this.reset();
         return false;
      }

      long elapsed = System.currentTimeMillis() - this.phaseStartTime;
      SwapSettings settings = this.settingsProvider.get();
      switch (this.phase) {
         case PRE_STOP:
            if (elapsed >= this.currentDelay) {
               this.movement.saveState();
               this.movement.block();
               if (settings.shouldStopSprint()) {
                  this.movement.stopSprint();
               }

               this.startPhase(MaceState.SwapPhase.STOPPING, 0);
               return true;
            }
            break;
         case STOPPING:
            this.movement.block();
            if (settings.shouldStopSprint()) {
               this.movement.stopSprint();
            }

            this.startPhase(MaceState.SwapPhase.WAIT_STOP, settings.randomWaitStopDelay());
            return this.currentDelay == 0;
         case WAIT_STOP:
            this.movement.block();
            boolean stopped = this.movement.isPlayerStopped(settings.getVelocityThreshold());
            boolean timeout = elapsed >= this.currentDelay;
            if (!stopped && !timeout) {
               break;
            }

            this.startPhase(MaceState.SwapPhase.PRE_SWAP, settings.randomPreSwapDelay());
            return this.currentDelay == 0;
         case PRE_SWAP:
            this.movement.block();
            if (elapsed >= this.currentDelay) {
               this.startPhase(MaceState.SwapPhase.DO_SWAP, 0);
               return true;
            }
            break;
         case DO_SWAP:
            int wrappedSlot = InventoryUtils.wrapSlot(this.slot);
            InventoryUtils.swap(wrappedSlot, 6);
            this.startPhase(MaceState.SwapPhase.POST_SWAP, settings.randomPostSwapDelay());
            return this.currentDelay == 0;
         case POST_SWAP:
            if (elapsed >= this.currentDelay) {
               if (settings.shouldCloseInventory()) {
                  InventoryUtils.closeScreen();
               }

               this.startPhase(MaceState.SwapPhase.RESUMING, settings.randomResumeDelay());
               return this.currentDelay == 0;
            }
            break;
         case RESUMING:
            if (elapsed >= this.currentDelay) {
               this.movement.restoreFromCurrent();
               this.reset();
               return false;
            }
      }

      return false;
   }

   private void startPhase(MaceState.SwapPhase phase, int delay) {
      this.phase = phase;
      this.phaseStartTime = System.currentTimeMillis();
      this.currentDelay = delay;
   }

   public void reset() {
      this.movement.reset();
      this.phase = MaceState.SwapPhase.IDLE;
      this.slot = -1;
      this.phaseStartTime = 0L;
      this.currentDelay = 0;
   }

   public void forceRestore() {
      if (this.movement.isBlocked()) {
         this.movement.restoreFromCurrent();
      }
   }

   @Generated
   public MovementController getMovement() {
      return this.movement;
   }

   @Generated
   public MaceState.SwapPhase getPhase() {
      return this.phase;
   }

   @Generated
   public int getSlot() {
      return this.slot;
   }

   @Generated
   public long getPhaseStartTime() {
      return this.phaseStartTime;
   }

   @Generated
   public int getCurrentDelay() {
      return this.currentDelay;
   }

   @Generated
   public ArmorSwapHandler.SwapSettingsProvider getSettingsProvider() {
      return this.settingsProvider;
   }

   @Generated
   public void setPhase(MaceState.SwapPhase phase) {
      this.phase = phase;
   }

   @FunctionalInterface
   public interface SwapSettingsProvider {
      SwapSettings get();
   }
}
