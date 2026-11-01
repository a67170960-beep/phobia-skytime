package ruhack.phobia.system.modulesystem.impl.misc;

import java.util.stream.Stream;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.ClickSlotEvent;
import ruhack.phobia.system.eventsystem.impl.HandledScreenEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.player.swap.InventoryUtils;
import ruhack.phobia.util.string.PlayerInteractionHelper;
import ruhack.phobia.util.timer.StopWatch;

public class ItemScroller extends ModuleStructure {
   private final StopWatch stopWatch = new StopWatch();
   private final SliderSettings scrollerSetting = new SliderSettings("Задержка прокрутки предметов", "Выберите задержку прокрутки предметов", 50.0F)
      .range(0, 200);

   public ItemScroller() {
      super("ItemScroller", "Помогает игроку складывать/забирать предметы", ModuleCategory.MISC);
      this.settings(this.scrollerSetting);
   }

   @EventHandler
   public void onHandledScreen(HandledScreenEvent e) {
      if (mc.player != null) {
         Slot hoverSlot = e.getSlotHover();
         SlotActionType actionType = this.getActionType();
         if (PlayerInteractionHelper.isKey(mc.options.sneakKey)
            && !PlayerInteractionHelper.isKey(mc.options.sprintKey)
            && hoverSlot != null
            && hoverSlot.hasStack()
            && actionType != null
            && this.stopWatch.every(this.scrollerSetting.getValue())) {
            InventoryUtils.click(hoverSlot.id, actionType.equals(SlotActionType.THROW) ? 1 : 0, actionType);
         }
      }
   }

   private SlotActionType getActionType() {
      return PlayerInteractionHelper.isKey(mc.options.dropKey)
         ? SlotActionType.THROW
         : (PlayerInteractionHelper.isKey(mc.options.attackKey) ? SlotActionType.QUICK_MOVE : null);
   }

   @EventHandler
   public void onClickSlot(ClickSlotEvent e) {
      if (mc.player != null) {
         int slotId = e.getSlotId();
         if (slotId >= 0 && slotId < mc.player.currentScreenHandler.slots.size()) {
            Slot slot = mc.player.currentScreenHandler.getSlot(slotId);
            Item item = slot.getStack().getItem();
            if (item != null
               && PlayerInteractionHelper.isKey(mc.options.sneakKey)
               && PlayerInteractionHelper.isKey(mc.options.sprintKey)
               && this.stopWatch.every(50.0)) {
               this.processSlotClick(slot, item, e);
            }
         }
      }
   }

   private void processSlotClick(Slot slot, Item item, ClickSlotEvent e) {
      this.getSlots()
         .filter(s -> s.getStack().getItem().equals(item) && s.inventory.equals(slot.inventory))
         .forEach(s -> InventoryUtils.click(s.id, 1, e.getActionType()));
   }

   private Stream<Slot> getSlots() {
      return mc.player.currentScreenHandler.slots.stream();
   }
}
