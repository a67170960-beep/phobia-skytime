package ruhack.phobia.system.modulesystem.impl.misc;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.KeyEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.friendsystem.FriendUtils;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BindSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.util.player.swap.InventoryResult;
import ruhack.phobia.util.player.swap.InventoryUtils;
import ruhack.phobia.util.player.swap.MovementController;
import ruhack.phobia.util.player.swap.SwapManager;
import ruhack.phobia.util.player.swap.script.Script;
import ruhack.phobia.util.string.chat.ChatMessage;

public class ClickAction extends ModuleStructure {
   private final BindSetting pearlBind = new BindSetting("Кнопка пёрла", "Быстрое использование эндер-жемчуга");
   private final BindSetting windChargeBind = new BindSetting("Кнопка заряда ветра", "Быстро бросить заряд ветра");
   private final BindSetting friendBind = new BindSetting("Кнопка друга", "Добавить/удалить игрока из друзей");
   public final SelectSetting mode = new SelectSetting(
      "Режим", "Быстрый = пакетный, New = остановка на тик, Легит = видимый свап через активный слот", "Быстрый", "Быстрый", "New", "Легит"
   );
   private final Script script = new Script();
   private final MovementController actionMovement = new MovementController();
   private int previousSlot = -1;
   private int pearlSlot = -1;
   private boolean isFromHotbar = false;
   private ClickAction.ActionPhase actionPhase = ClickAction.ActionPhase.IDLE;
   private Item pendingItem;
   private String pendingNotFoundMessage;
   private boolean visibleLegitSwap;
   private int stopTicks;

   public ClickAction() {
      super("ClickAction", "Действия по отдельным кнопкам", ModuleCategory.MISC);
      this.settings(this.pearlBind, this.windChargeBind, this.friendBind, this.mode);
   }

   @EventHandler
   public void onKey(KeyEvent e) {
      if (mc.player != null && mc.currentScreen == null) {
         if (e.isBindReleased(this.pearlBind)) {
            if (this.mode.getValue().equals("Быстрый")) {
               this.executeItemInstant(Items.ENDER_PEARL, "Жемчуг не найден");
            } else if (this.mode.getValue().equals("New")) {
               this.prepareOneTickAction(Items.ENDER_PEARL, "Жемчуг не найден", false);
            } else if (this.mode.getValue().equals("Легит")) {
               this.prepareOneTickAction(Items.ENDER_PEARL, "Жемчуг не найден", true);
            }
         }

         if (e.isBindReleased(this.windChargeBind)) {
            if (this.mode.getValue().equals("Быстрый")) {
               this.executeItemInstant(Items.WIND_CHARGE, "Заряд ветра не найден");
            } else if (this.mode.getValue().equals("New")) {
               this.prepareOneTickAction(Items.WIND_CHARGE, "Заряд ветра не найден", false);
            } else if (this.mode.getValue().equals("Легит")) {
               this.prepareOneTickAction(Items.WIND_CHARGE, "Заряд ветра не найден", true);
            }
         }

         if (e.isBindDown(this.friendBind) && mc.crosshairTarget instanceof EntityHitResult result && result.getEntity() instanceof PlayerEntity player) {
            if (FriendUtils.isFriend(player)) {
               FriendUtils.removeFriend(player);
            } else {
               FriendUtils.addFriend(player);
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null) {
         this.script.update();
         this.updateOneTickAction();
      }
   }

   private void executeItemInstant(Item item, String notFoundMessage) {
      InventoryResult hotbar = this.findInHotbar(item);
      if (hotbar.found()) {
         InventoryUtils.selectSlotSilent(hotbar.slot());
         InventoryUtils.sendUsePacket(Hand.MAIN_HAND);
         InventoryUtils.selectSlotSilent(mc.player.getInventory().getSelectedSlot());
      } else {
         InventoryResult inv = this.findInInventory(item);
         if (!inv.found()) {
            ChatMessage.brandmessage(notFoundMessage);
         } else {
            int currentSlot = mc.player.getInventory().getSelectedSlot();
            InventoryUtils.swapHotbar(inv.slot(), currentSlot);
            InventoryUtils.sendUsePacket(Hand.MAIN_HAND);
            InventoryUtils.swapHotbar(inv.slot(), currentSlot);
         }
      }
   }

   private void prepareItemLegit(Item item, String notFoundMessage) {
      InventoryResult hotbar = this.findInHotbar(item);
      if (hotbar.found()) {
         this.previousSlot = mc.player.getInventory().getSelectedSlot();
         this.pearlSlot = hotbar.slot();
         this.isFromHotbar = true;
      } else {
         InventoryResult inv = this.findInInventory(item);
         if (!inv.found()) {
            ChatMessage.brandmessage(notFoundMessage);
            return;
         }

         this.previousSlot = mc.player.getInventory().getSelectedSlot();
         this.pearlSlot = inv.slot();
         this.isFromHotbar = false;
      }

      this.script.cleanup();
      if (this.isFromHotbar) {
         this.script
            .addStep(0, () -> InventoryUtils.selectSlot(this.pearlSlot))
            .addStep(35, () -> InventoryUtils.use(Hand.MAIN_HAND))
            .addStep(30, () -> InventoryUtils.selectSlot(this.previousSlot))
            .addStep(25, this::cleanup);
      } else {
         this.script
            .addStep(0, () -> InventoryUtils.swapHotbar(this.pearlSlot, this.previousSlot))
            .addStep(40, () -> InventoryUtils.use(Hand.MAIN_HAND))
            .addStep(35, () -> InventoryUtils.swapHotbar(this.pearlSlot, this.previousSlot))
            .addStep(25, this::cleanup);
      }
   }

   private void prepareOneTickAction(Item item, String notFoundMessage, boolean visibleSwap) {
      if (this.actionPhase == ClickAction.ActionPhase.IDLE && this.script.isFinished() && !SwapManager.anySwapRunning()) {
         InventoryResult hotbar = this.findInHotbar(item);
         InventoryResult inventory = hotbar.found() ? InventoryResult.notFound() : this.findInInventory(item);
         if (!hotbar.found() && !inventory.found()) {
            ChatMessage.brandmessage(notFoundMessage);
         } else {
            this.previousSlot = mc.player.getInventory().getSelectedSlot();
            this.pearlSlot = hotbar.found() ? hotbar.slot() : inventory.slot();
            this.isFromHotbar = hotbar.found();
            this.pendingItem = item;
            this.pendingNotFoundMessage = notFoundMessage;
            this.visibleLegitSwap = visibleSwap;
            this.stopTicks = 1;
            this.actionMovement.saveState();
            this.actionMovement.block();
            this.actionPhase = ClickAction.ActionPhase.WAIT_STOP;
         }
      }
   }

   private void updateOneTickAction() {
      if (this.actionPhase != ClickAction.ActionPhase.IDLE) {
         this.actionMovement.block();
         if (this.actionPhase == ClickAction.ActionPhase.WAIT_STOP) {
            if (this.stopTicks-- <= 0) {
               if (!this.visibleLegitSwap) {
                  this.executeItemInstant(this.pendingItem, this.pendingNotFoundMessage);
                  this.finishOneTickAction();
               } else {
                  if (this.isFromHotbar) {
                     InventoryUtils.selectSlot(this.pearlSlot);
                     InventoryUtils.use(Hand.MAIN_HAND);
                  } else {
                     InventoryUtils.swapHotbar(this.pearlSlot, this.previousSlot);
                     InventoryUtils.use(Hand.MAIN_HAND);
                  }

                  this.actionPhase = ClickAction.ActionPhase.RESTORE;
               }
            }
         } else {
            if (this.actionPhase == ClickAction.ActionPhase.RESTORE) {
               if (this.isFromHotbar) {
                  InventoryUtils.selectSlot(this.previousSlot);
               } else {
                  InventoryUtils.swapHotbar(this.pearlSlot, this.previousSlot);
               }

               this.finishOneTickAction();
            }
         }
      }
   }

   private void finishOneTickAction() {
      if (this.actionMovement.isBlocked()) {
         this.actionMovement.restoreFromCurrent();
      }

      this.actionPhase = ClickAction.ActionPhase.IDLE;
      this.pendingItem = null;
      this.pendingNotFoundMessage = null;
      this.visibleLegitSwap = false;
      this.stopTicks = 0;
      this.cleanup();
   }

   private void cleanup() {
      this.previousSlot = -1;
      this.pearlSlot = -1;
      this.isFromHotbar = false;
   }

   private InventoryResult findInHotbar(Item item) {
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

   private InventoryResult findInInventory(Item item) {
      if (mc.player == null) {
         return InventoryResult.notFound();
      }

      for (int i = 9; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getItem() == item) {
            return InventoryResult.of(i, stack);
         }
      }

      return InventoryResult.notFound();
   }

   @Override
   public void deactivate() {
      this.script.cleanup();
      this.finishOneTickAction();
   }

   private enum ActionPhase {
      IDLE,
      WAIT_STOP,
      RESTORE;
   }
}
