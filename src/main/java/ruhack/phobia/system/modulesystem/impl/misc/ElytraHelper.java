package ruhack.phobia.system.modulesystem.impl.misc;

import java.util.function.Predicate;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.InputEvent;
import ruhack.phobia.system.eventsystem.impl.KeyEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BindSetting;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.util.player.swap.InventoryResult;
import ruhack.phobia.util.player.swap.InventoryUtils;
import ruhack.phobia.util.player.swap.SwapManager;
import ruhack.phobia.util.player.swap.SwapSettings;
import ruhack.phobia.util.string.PlayerInteractionHelper;

public class ElytraHelper extends ModuleStructure {
   private static final int CHEST_ARMOR_SLOT = 6;
   private static final int TEMP_HOTBAR_SLOT = 8;
   private static final String SWAP_ID_ELYTRA = "ElytraHelper_Elytra";
   private static final String SWAP_ID_FIREWORK = "ElytraHelper_Firework";
   private final BindSetting elytraBind = new BindSetting("Кнопка элитры", "Переключение элитры/нагрудника");
   private final BindSetting fireworkBind = new BindSetting("Кнопка фейерверка", "Быстрое использование фейерверка");
   private final SelectSetting mode = new SelectSetting("Режим", "Быстрый - моментальный, New - остановка на один тик", "Быстрый", "Быстрый", "New");
   private final BooleanSetting recast = new BooleanSetting("Авто взлет", "Автоматически начинает полет").setValue(true);
   private int fireworkSlot = -1;
   private int previousSlot = -1;
   private boolean fireworkFromHotbar = false;
   private int elytraTargetSlot = -1;

   public ElytraHelper() {
      super("ElytraHelper", "Переключение элитры и быстрый фейерверк", ModuleCategory.MISC);
      this.settings(this.elytraBind, this.fireworkBind, this.mode, this.recast);
   }

   @EventHandler
   public void onInput(InputEvent e) {
      if (mc.player != null) {
         if (this.recast.isValue()) {
            if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
               if (mc.player.isOnGround()) {
                  e.setJumping(true);
               } else if (!mc.player.isGliding()) {
                  PlayerInteractionHelper.startFallFlying();
               }
            }
         }
      }
   }

   @EventHandler
   public void onKey(KeyEvent e) {
      if (mc.player != null && mc.currentScreen == null) {
         String swapMode = this.mode.getValue();
         boolean isFast = swapMode.equals("Быстрый");
         if (e.isBindReleased(this.elytraBind)) {
            if (isFast) {
               this.executeElytraSwapInstant();
            } else if (!SwapManager.isSwapQueued("ElytraHelper_Elytra")) {
               this.prepareElytraSwapNew();
            }
         }

         if (e.isBindReleased(this.fireworkBind)) {
            if (isFast) {
               this.executeFireworkInstant();
            } else if (!SwapManager.isSwapQueued("ElytraHelper_Firework")) {
               this.prepareFireworkNew();
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null) {
         SwapManager.tick();
      }
   }

   private void executeElytraSwapInstant() {
      Item currentChest = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem();
      int targetSlot;
      if (currentChest == Items.ELYTRA) {
         InventoryResult chest = this.findChestplate();
         if (!chest.found()) {
            return;
         }

         targetSlot = chest.slot();
      } else {
         InventoryResult elytra = this.findItem(Items.ELYTRA);
         if (!elytra.found()) {
            return;
         }

         targetSlot = elytra.slot();
      }

      InventoryUtils.swapHotbar(targetSlot, 8);
      InventoryUtils.click(6, 8, SlotActionType.SWAP);
      InventoryUtils.swapHotbar(targetSlot, 8);
   }

   private void executeFireworkInstant() {
      if (mc.player.isGliding()) {
         InventoryResult hotbar = this.findItemHotbar(Items.FIREWORK_ROCKET);
         if (hotbar.found()) {
            int slot = hotbar.slot();
            InventoryUtils.selectSlotSilent(slot);
            InventoryUtils.sendUsePacket(Hand.MAIN_HAND);
            InventoryUtils.selectSlotSilent(mc.player.getInventory().getSelectedSlot());
         } else {
            InventoryResult inv = this.findItem(Items.FIREWORK_ROCKET);
            if (!inv.found()) {
               return;
            }

            int currentSlot = mc.player.getInventory().getSelectedSlot();
            InventoryUtils.swapHotbar(inv.slot(), currentSlot);
            InventoryUtils.sendUsePacket(Hand.MAIN_HAND);
            InventoryUtils.swapHotbar(inv.slot(), currentSlot);
         }
      }
   }

   private void prepareElytraSwapNew() {
      Item currentChest = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem();
      int targetSlot;
      if (currentChest == Items.ELYTRA) {
         InventoryResult chest = this.findChestplate();
         if (!chest.found()) {
            return;
         }

         targetSlot = chest.slot();
      } else {
         InventoryResult elytra = this.findItem(Items.ELYTRA);
         if (!elytra.found()) {
            return;
         }

         targetSlot = elytra.slot();
      }

      this.elytraTargetSlot = targetSlot;
      SwapManager.queueSwap("ElytraHelper_Elytra", 10, this::executeChestSwapLegit, this.createSwapSettings(), this::cleanupElytra);
   }

   private void executeChestSwapLegit() {
      if (this.elytraTargetSlot != -1) {
         InventoryUtils.swapHotbar(this.elytraTargetSlot, 8);
         InventoryUtils.click(6, 8, SlotActionType.SWAP);
         InventoryUtils.swapHotbar(this.elytraTargetSlot, 8);
      }
   }

   private void cleanupElytra() {
      this.elytraTargetSlot = -1;
   }

   private void prepareFireworkNew() {
      if (mc.player.isGliding()) {
         InventoryResult hotbar = this.findItemHotbar(Items.FIREWORK_ROCKET);
         if (hotbar.found()) {
            this.previousSlot = mc.player.getInventory().getSelectedSlot();
            this.fireworkSlot = hotbar.slot();
            this.fireworkFromHotbar = true;
         } else {
            InventoryResult inv = this.findItem(Items.FIREWORK_ROCKET);
            if (!inv.found()) {
               return;
            }

            this.previousSlot = mc.player.getInventory().getSelectedSlot();
            this.fireworkSlot = inv.slot();
            this.fireworkFromHotbar = false;
         }

         SwapManager.queueSwap("ElytraHelper_Firework", 10, this::executeFireworkLegit, this.createSwapSettings(), this::cleanupFirework);
      }
   }

   private void executeFireworkLegit() {
      if (this.fireworkFromHotbar) {
         InventoryUtils.selectSlot(this.fireworkSlot);
         InventoryUtils.use(Hand.MAIN_HAND);
         InventoryUtils.selectSlot(this.previousSlot);
      } else {
         InventoryUtils.swapHotbar(this.fireworkSlot, this.previousSlot);
         InventoryUtils.use(Hand.MAIN_HAND);
         InventoryUtils.swapHotbar(this.fireworkSlot, this.previousSlot);
      }
   }

   private void cleanupFirework() {
      this.previousSlot = -1;
      this.fireworkSlot = -1;
      this.fireworkFromHotbar = false;
   }

   private SwapSettings createSwapSettings() {
      return new SwapSettings()
         .stopMovement(true)
         .stopSprint(false)
         .closeInventory(false)
         .preStopDelay(0, 0)
         .waitStopDelay(0, 0)
         .minimumStopDelay(50)
         .preSwapDelay(0, 0)
         .postSwapDelay(0, 0)
         .resumeDelay(0, 0)
         .velocityThreshold(0.003);
   }

   private InventoryResult findItem(Item item) {
      return this.findItem(stack -> stack.getItem() == item);
   }

   private InventoryResult findItem(Predicate<ItemStack> predicate) {
      if (mc.player == null) {
         return InventoryResult.notFound();
      }

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && predicate.test(stack)) {
            return InventoryResult.of(i + 36, stack);
         }
      }

      for (int i = 9; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && predicate.test(stack)) {
            return InventoryResult.of(i, stack);
         }
      }

      return InventoryResult.notFound();
   }

   private InventoryResult findChestplate() {
      return this.findItem(
         stack -> {
            Item item = stack.getItem();
            return item == Items.NETHERITE_CHESTPLATE
               || item == Items.DIAMOND_CHESTPLATE
               || item == Items.IRON_CHESTPLATE
               || item == Items.GOLDEN_CHESTPLATE
               || item == Items.CHAINMAIL_CHESTPLATE
               || item == Items.LEATHER_CHESTPLATE;
         }
      );
   }

   private InventoryResult findItemHotbar(Item item) {
      if (mc.player == null) {
         return InventoryResult.notFound();
      }

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getItem() == item) {
            return new InventoryResult(i, true, stack);
         }
      }

      return InventoryResult.notFound();
   }

   @Override
   public void deactivate() {
      SwapManager.cancelSwap("ElytraHelper_Elytra");
      SwapManager.cancelSwap("ElytraHelper_Firework");
      this.previousSlot = -1;
      this.fireworkSlot = -1;
      this.fireworkFromHotbar = false;
      this.elytraTargetSlot = -1;
   }
}
