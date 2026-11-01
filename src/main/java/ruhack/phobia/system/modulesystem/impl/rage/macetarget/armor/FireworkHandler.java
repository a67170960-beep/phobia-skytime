package ruhack.phobia.system.modulesystem.impl.rage.macetarget.armor;

import java.util.function.IntFunction;
import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import ruhack.phobia.mixin.ClientWorldAccessor;
import ruhack.phobia.system.modulesystem.impl.rage.macetarget.state.MaceState;
import ruhack.phobia.util.player.swap.InventoryUtils;
import ruhack.phobia.util.player.swap.MovementController;
import ruhack.phobia.util.player.swap.SwapSettings;
import ruhack.phobia.util.rotation.RotationController;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;

public class FireworkHandler {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private final MovementController movement = new MovementController();
   private MaceState.FireworkPhase phase = MaceState.FireworkPhase.IDLE;
   private int slot = -1;
   private int savedSlot = -1;
   private boolean fromInventory = false;
   private long phaseStartTime = 0L;
   private int currentDelay = 0;
   private final FireworkHandler.SwapSettingsProvider settingsProvider;

   public FireworkHandler(FireworkHandler.SwapSettingsProvider settingsProvider) {
      this.settingsProvider = settingsProvider;
   }

   public boolean isActive() {
      return this.phase != MaceState.FireworkPhase.IDLE;
   }

   public void useFirework(boolean isSilent) {
      if (isSilent) {
         this.useSilent();
      } else {
         this.startLegit();
      }
   }

   private void useSilent() {
      if (mc.player != null) {
         int hotbarSlot = InventoryUtils.findHotbarItem(Items.FIREWORK_ROCKET);
         if (hotbarSlot != -1) {
            int currentSlot = mc.player.getInventory().getSelectedSlot();
            if (hotbarSlot != currentSlot) {
               mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
            }

            Angle rotation = this.getRotation();
            this.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, rotation.getYaw(), rotation.getPitch()));
            if (hotbarSlot != currentSlot) {
               mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
            }
         } else {
            int invSlot = InventoryUtils.findItemInInventory(Items.FIREWORK_ROCKET);
            if (invSlot != -1) {
               int currentHotbarSlot = mc.player.getInventory().getSelectedSlot();
               int wrappedSlot = InventoryUtils.wrapSlot(invSlot);
               InventoryUtils.click(wrappedSlot, currentHotbarSlot, SlotActionType.SWAP);
               Angle rotation = this.getRotation();
               this.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, rotation.getYaw(), rotation.getPitch()));
               InventoryUtils.click(wrappedSlot, currentHotbarSlot, SlotActionType.SWAP);
               InventoryUtils.closeScreen();
            }
         }
      }
   }

   private void startLegit() {
      if (mc.player != null) {
         this.savedSlot = mc.player.getInventory().getSelectedSlot();
         int hotbarSlot = InventoryUtils.findHotbarItem(Items.FIREWORK_ROCKET);
         if (hotbarSlot != -1) {
            this.slot = hotbarSlot;
            this.fromInventory = false;
            InventoryUtils.selectSlot(this.slot);
            this.startPhase(MaceState.FireworkPhase.AWAIT_ITEM, 0);
         } else {
            int invSlot = InventoryUtils.findItemInInventory(Items.FIREWORK_ROCKET);
            if (invSlot != -1) {
               this.slot = invSlot;
               this.fromInventory = true;
               SwapSettings settings = this.settingsProvider.get();
               if (settings.shouldStopMovement()) {
                  this.startPhase(MaceState.FireworkPhase.PRE_STOP, settings.randomPreStopDelay());
               } else {
                  this.startPhase(MaceState.FireworkPhase.SWAP_TO_HAND, 0);
               }
            }
         }
      }
   }

   public void processLoop() {
      if (this.phase != MaceState.FireworkPhase.IDLE) {
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

               this.startPhase(MaceState.FireworkPhase.STOPPING, 0);
               return true;
            }
            break;
         case STOPPING:
            this.movement.block();
            if (settings.shouldStopSprint()) {
               this.movement.stopSprint();
            }

            this.startPhase(MaceState.FireworkPhase.WAIT_STOP, settings.randomWaitStopDelay());
            return this.currentDelay == 0;
         case WAIT_STOP:
            this.movement.block();
            boolean stopped = this.movement.isPlayerStopped(settings.getVelocityThreshold());
            boolean timeout = elapsed >= this.currentDelay;
            if (!stopped && !timeout) {
               break;
            }

            this.startPhase(MaceState.FireworkPhase.PRE_SWAP, settings.randomPreSwapDelay());
            return this.currentDelay == 0;
         case PRE_SWAP:
            this.movement.block();
            if (elapsed >= this.currentDelay) {
               this.startPhase(MaceState.FireworkPhase.SWAP_TO_HAND, 0);
               return true;
            }
            break;
         case SWAP_TO_HAND:
            int hotbarSlot = mc.player.getInventory().getSelectedSlot();
            InventoryUtils.click(this.slot, hotbarSlot, SlotActionType.SWAP);
            this.startPhase(MaceState.FireworkPhase.AWAIT_ITEM, 0);
            return true;
         case AWAIT_ITEM:
            if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
               this.startPhase(MaceState.FireworkPhase.USE, 0);
               return true;
            }
            break;
         case USE:
            Angle rotation = this.getRotation();
            this.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, rotation.getYaw(), rotation.getPitch()));
            mc.player.swingHand(Hand.MAIN_HAND);
            this.startPhase(MaceState.FireworkPhase.POST_USE, settings.randomPostSwapDelay());
            return this.currentDelay == 0;
         case POST_USE:
            if (elapsed >= this.currentDelay) {
               if (this.fromInventory) {
                  this.startPhase(MaceState.FireworkPhase.SWAP_BACK, 0);
                  return true;
               }

               InventoryUtils.selectSlot(this.savedSlot);
               this.startPhase(MaceState.FireworkPhase.RESUMING, settings.randomResumeDelay());
               return this.currentDelay == 0;
            }
            break;
         case SWAP_BACK:
            hotbarSlot = mc.player.getInventory().getSelectedSlot();
            InventoryUtils.click(this.slot, hotbarSlot, SlotActionType.SWAP);
            InventoryUtils.selectSlot(this.savedSlot);
            if (settings.shouldCloseInventory()) {
               InventoryUtils.closeScreen();
            }

            this.startPhase(MaceState.FireworkPhase.RESUMING, settings.randomResumeDelay());
            return this.currentDelay == 0;
         case RESUMING:
            if (elapsed >= this.currentDelay) {
               this.movement.restoreFromCurrent();
               this.reset();
               return false;
            }
      }

      return false;
   }

   private Angle getRotation() {
      Angle rotation = RotationController.INSTANCE.getRotation();
      return rotation != null ? rotation : AngleUtil.cameraAngle();
   }

   private void sendSequencedPacket(IntFunction<Packet<?>> packetCreator) {
      if (mc.player != null && mc.getNetworkHandler() != null && mc.world != null) {
         try {
            ClientWorldAccessor worldAccessor = (ClientWorldAccessor)mc.world;
            PendingUpdateManager pendingUpdateManager = worldAccessor.phobia$getPendingUpdateManager().incrementSequence();
            int sequence = pendingUpdateManager.getSequence();
            mc.getNetworkHandler().sendPacket(packetCreator.apply(sequence));
            pendingUpdateManager.close();
         } catch (Exception e) {
            mc.getNetworkHandler().sendPacket(packetCreator.apply(0));
         }
      }
   }

   private void startPhase(MaceState.FireworkPhase phase, int delay) {
      this.phase = phase;
      this.phaseStartTime = System.currentTimeMillis();
      this.currentDelay = delay;
   }

   public void reset() {
      this.movement.reset();
      this.phase = MaceState.FireworkPhase.IDLE;
      this.slot = -1;
      this.savedSlot = -1;
      this.fromInventory = false;
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
   public MaceState.FireworkPhase getPhase() {
      return this.phase;
   }

   @Generated
   public int getSlot() {
      return this.slot;
   }

   @Generated
   public int getSavedSlot() {
      return this.savedSlot;
   }

   @Generated
   public boolean isFromInventory() {
      return this.fromInventory;
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
   public FireworkHandler.SwapSettingsProvider getSettingsProvider() {
      return this.settingsProvider;
   }

   @Generated
   public void setPhase(MaceState.FireworkPhase phase) {
      this.phase = phase;
   }

   @FunctionalInterface
   public interface SwapSettingsProvider {
      SwapSettings get();
   }
}
