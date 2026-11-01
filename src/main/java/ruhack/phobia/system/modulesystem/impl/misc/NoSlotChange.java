package ruhack.phobia.system.modulesystem.impl.misc;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.ClickSlotEvent;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;

public class NoSlotChange extends ModuleStructure {
   private final MultiSelectSetting protections = new MultiSelectSetting("Защита", "Что запрещено случайно менять")
      .value("Смена слота сервером", "Выброс элитры", "Выброс нагрудника", "Выброс головы")
      .selected("Смена слота сервером", "Выброс элитры", "Выброс нагрудника");

   public NoSlotChange() {
      super("NoSlotChange", "Защищает слот и важные предметы от случайной смены или выброса", ModuleCategory.MISC);
      this.settings(this.protections);
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (!event.isSend() && this.protections.isSelected("Смена слота сервером") && event.getPacket() instanceof UpdateSelectedSlotS2CPacket) {
         event.cancel();
      }
   }

   @EventHandler
   public void onClickSlot(ClickSlotEvent event) {
      if (mc.player != null
         && event.getActionType() == SlotActionType.THROW
         && event.getSlotId() >= 0
         && event.getSlotId() < mc.player.currentScreenHandler.slots.size()) {
         Slot slot = mc.player.currentScreenHandler.getSlot(event.getSlotId());
         ItemStack stack = slot.getStack();
         if (this.protections.isSelected("Выброс элитры") && stack.isOf(Items.ELYTRA)
            || this.protections.isSelected("Выброс нагрудника") && isChestArmor(stack)
            || this.protections.isSelected("Выброс головы") && stack.isOf(Items.PLAYER_HEAD)) {
            event.cancel();
         }
      }
   }

   private static boolean isChestArmor(ItemStack stack) {
      EquippableComponent equippable = (EquippableComponent)stack.get(DataComponentTypes.EQUIPPABLE);
      return equippable != null && equippable.slot() == EquipmentSlot.CHEST && !stack.isOf(Items.ELYTRA);
   }
}
