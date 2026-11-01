package ruhack.phobia.system.modulesystem.impl.misc;

import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.KeyEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BindSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.util.player.swap.InventoryUtils;
import ruhack.phobia.util.player.swap.MovementController;
import ruhack.phobia.util.string.chat.ChatMessage;

public class FuntimeHelper extends ModuleStructure {
   private final BindSetting disorientation = new BindSetting("Дезориентация", "Использовать дезориентацию");
   private final BindSetting trap = new BindSetting("Трапка", "Использовать трапку");
   private final BindSetting explicitDust = new BindSetting("Явная пыль", "Использовать явную пыль");
   private final BindSetting fireTornado = new BindSetting("Огненный смерч", "Использовать огненный смерч");
   private final BindSetting layer = new BindSetting("Пласт", "Использовать пласт");
   private final BindSetting divineAura = new BindSetting("Божья аура", "Использовать божью ауру");
   private final BindSetting snowball = new BindSetting("Снежок", "Использовать снежок");
   private final List<HelperItemBind> itemBinds;
   private final SelectSetting mode = new SelectSetting(
      "Режим свапа", "Быстрый - моментальное использование, New - остановка на один тик перед свапом", "Быстрый", "Быстрый", "New"
   );
   private final MovementController movement = new MovementController();
   private FuntimeHelper.ActionPhase phase = FuntimeHelper.ActionPhase.IDLE;
   private int previousSlot = -1;
   private int targetSlot = -1;
   private int temporaryHotbarSlot = -1;
   private boolean fromHotbar;
   private int stopTicks;
   private long restoreAt;

   public List<HelperItemBind> getItemBinds() {
      return this.itemBinds;
   }

   public FuntimeHelper() {
      super("FuntimeHelper", "Помощник для использования специальных предметов Funtime", ModuleCategory.MISC);
      this.itemBinds = List.of(
         new HelperItemBind("FT", Items.ENDER_EYE, this.disorientation),
         new HelperItemBind("FT", Items.NETHERITE_SCRAP, this.trap),
         new HelperItemBind("FT", Items.SUGAR, this.explicitDust),
         new HelperItemBind("FT", Items.FIRE_CHARGE, this.fireTornado),
         new HelperItemBind("FT", Items.DRIED_KELP, this.layer),
         new HelperItemBind("FT", Items.PHANTOM_MEMBRANE, this.divineAura),
         new HelperItemBind("FT", Items.SNOWBALL, this.snowball)
      );
      this.settings(this.disorientation, this.trap, this.explicitDust, this.fireTornado, this.layer, this.divineAura, this.snowball, this.mode);
   }

   @EventHandler
   public void onKey(KeyEvent event) {
      if (mc.player != null && mc.currentScreen == null && this.phase == FuntimeHelper.ActionPhase.IDLE) {
         Item item = this.getBoundItem(event);
         if (item != null) {
            if (!this.prepare(item)) {
               ChatMessage.brandmessage("Предмет не найден");
            } else {
               if (this.mode.isSelected("New")) {
                  this.movement.saveState();
                  this.movement.block();
                  this.stopTicks = 1;
                  this.phase = FuntimeHelper.ActionPhase.WAIT_USE_STOP;
               } else {
                  this.usePreparedItem();
               }
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player == null) {
         this.cleanup();
      } else if (this.phase == FuntimeHelper.ActionPhase.WAIT_USE_STOP) {
         this.movement.block();
         if (this.stopTicks-- <= 0) {
            this.usePreparedItem();
            this.restoreMovement();
         }
      } else if (this.phase == FuntimeHelper.ActionPhase.WAIT_RESTORE && System.currentTimeMillis() >= this.restoreAt) {
         if (this.mode.isSelected("New") && !this.fromHotbar) {
            this.movement.saveState();
            this.movement.block();
            this.stopTicks = 1;
            this.phase = FuntimeHelper.ActionPhase.WAIT_RESTORE_STOP;
         } else {
            this.restoreItem();
            this.cleanup();
         }
      } else {
         if (this.phase == FuntimeHelper.ActionPhase.WAIT_RESTORE_STOP) {
            this.movement.block();
            if (this.stopTicks-- > 0) {
               return;
            }

            this.restoreItem();
            this.cleanup();
         }
      }
   }

   private Item getBoundItem(KeyEvent event) {
      if (event.isBindReleased(this.disorientation)) {
         return Items.ENDER_EYE;
      } else if (event.isBindReleased(this.trap)) {
         return Items.NETHERITE_SCRAP;
      } else if (event.isBindReleased(this.explicitDust)) {
         return Items.SUGAR;
      } else if (event.isBindReleased(this.fireTornado)) {
         return Items.FIRE_CHARGE;
      } else if (event.isBindReleased(this.layer)) {
         return Items.DRIED_KELP;
      } else if (event.isBindReleased(this.divineAura)) {
         return Items.PHANTOM_MEMBRANE;
      } else {
         return event.isBindReleased(this.snowball) ? Items.SNOWBALL : null;
      }
   }

   private boolean prepare(Item item) {
      int hotbarSlot = InventoryUtils.findItemInHotbar(item);
      int inventorySlot = hotbarSlot == -1 ? InventoryUtils.findItemInInventory(item) : -1;
      if (hotbarSlot == -1 && inventorySlot == -1) {
         return false;
      }

      this.previousSlot = mc.player.getInventory().getSelectedSlot();
      this.fromHotbar = hotbarSlot != -1;
      this.targetSlot = this.fromHotbar ? hotbarSlot : inventorySlot;
      this.temporaryHotbarSlot = this.fromHotbar ? hotbarSlot : (this.previousSlot + 1) % 9;
      return true;
   }

   private void usePreparedItem() {
      if (!this.fromHotbar) {
         InventoryUtils.swapHotbar(this.targetSlot, this.temporaryHotbarSlot);
      }

      InventoryUtils.selectSlotSilent(this.temporaryHotbarSlot);
      InventoryUtils.sendUsePacket(Hand.MAIN_HAND);
      mc.player.swingHand(Hand.MAIN_HAND);
      this.restoreAt = System.currentTimeMillis() + 250L;
      this.phase = FuntimeHelper.ActionPhase.WAIT_RESTORE;
   }

   private void restoreItem() {
      InventoryUtils.selectSlotSilent(this.previousSlot);
      if (!this.fromHotbar) {
         InventoryUtils.swapHotbar(this.targetSlot, this.temporaryHotbarSlot);
      }
   }

   private void restoreMovement() {
      if (this.movement.isBlocked()) {
         this.movement.restoreFromCurrent();
      }
   }

   private void cleanup() {
      this.restoreMovement();
      this.phase = FuntimeHelper.ActionPhase.IDLE;
      this.previousSlot = -1;
      this.targetSlot = -1;
      this.temporaryHotbarSlot = -1;
      this.fromHotbar = false;
      this.stopTicks = 0;
      this.restoreAt = 0L;
   }

   @Override
   public void deactivate() {
      if ((this.phase == FuntimeHelper.ActionPhase.WAIT_RESTORE || this.phase == FuntimeHelper.ActionPhase.WAIT_RESTORE_STOP) && mc.player != null) {
         this.restoreItem();
      }

      this.cleanup();
   }

   enum ActionPhase {
      IDLE,
      WAIT_USE_STOP,
      WAIT_RESTORE,
      WAIT_RESTORE_STOP;
   }
}
