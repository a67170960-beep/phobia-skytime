package ruhack.phobia.system.modulesystem.impl.movement;

import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BindSetting;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.string.PlayerInteractionHelper;
import ruhack.phobia.util.timer.StopWatch;

public class LungeHelper extends ModuleStructure {
   private final BindSetting boostBind = new BindSetting("Буст", "Удерживай — свап слота + ЛКМ в одном тике");
   private final BooleanSetting autoSpear = new BooleanSetting("Авто копьё", "Сам ищет копьё с Рывком в хотбаре").setValue(true);
   private final SliderSettings spearSlot = new SliderSettings("Слот копья", "Хотбар-слот с копьём (1-9)", 7.0F)
      .range(1.0F, 9.0F)
      .visible(() -> !this.autoSpear.isValue());
   private final SliderSettings swapSlot = new SliderSettings("Слот свапа", "Слот откуда делается рывок", 2.0F).range(1.0F, 9.0F);
   private final SliderSettings burstDelay = new SliderSettings("Интервал", "Пауза между бустами", 0.0F).range(0.0F, 150.0F).suffix("ms");
   private final StopWatch timer = new StopWatch();
   private int savedSlot = -1;
   private boolean boosting;
   private boolean spearTurn = true;

   public LungeHelper() {
      super("LungeHelper", "Бесконечный буст копья: свап слота и ЛКМ одновременно", ModuleCategory.MOVEMENT);
      this.settings(this.boostBind, this.autoSpear, this.spearSlot, this.swapSlot, this.burstDelay);
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player == null || mc.world == null || mc.currentScreen != null || mc.getNetworkHandler() == null) {
         this.stopBoost();
      } else if (!PlayerInteractionHelper.isKey(this.boostBind)) {
         this.stopBoost();
      } else {
         int spear = this.resolveSpearSlot();
         int swap = this.hotbarIndex(this.swapSlot.getValue());
         if (spear >= 0 && swap >= 0 && spear != swap) {
            if (!this.boosting) {
               this.savedSlot = mc.player.getInventory().getSelectedSlot();
               this.boosting = true;
               this.spearTurn = mc.player.getInventory().getSelectedSlot() != spear;
               this.timer.reset();
            }

            if (!(this.burstDelay.getValue() > 0.0F) || this.timer.finished(this.burstDelay.getValue())) {
               if (this.burstDelay.getValue() <= 0.0F) {
                  this.swapAttack(spear);
                  this.swapAttack(swap);
               } else {
                  this.swapAttack(this.spearTurn ? spear : swap);
                  this.spearTurn = !this.spearTurn;
               }

               this.timer.reset();
            }
         } else {
            this.stopBoost();
         }
      }
   }

   private void swapAttack(int slot) {
      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      mc.player.getInventory().setSelectedSlot(slot);
      mc.doAttack();
   }

   private int resolveSpearSlot() {
      if (this.autoSpear.isValue()) {
         for (int i = 0; i < 9; i++) {
            if (this.isLungeSpear(mc.player.getInventory().getStack(i))) {
               return i;
            }
         }

         return -1;
      } else {
         int slot = this.hotbarIndex(this.spearSlot.getValue());
         return this.isLungeSpear(mc.player.getInventory().getStack(slot)) ? slot : -1;
      }
   }

   private boolean isLungeSpear(ItemStack stack) {
      if (!stack.isEmpty() && stack.isIn(ItemTags.SPEARS)) {
         ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);

         for (RegistryEntry<Enchantment> enchantment : enchantments.getEnchantments()) {
            if (enchantment.matchesKey(Enchantments.LUNGE)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private int hotbarIndex(float slotSetting) {
      return Math.max(0, Math.min(8, Math.round(slotSetting) - 1));
   }

   private void stopBoost() {
      if (this.boosting) {
         this.boosting = false;
         this.spearTurn = true;
         if (this.savedSlot >= 0 && this.savedSlot <= 8 && mc.player != null && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(this.savedSlot));
            mc.player.getInventory().setSelectedSlot(this.savedSlot);
         }

         this.savedSlot = -1;
      }
   }

   @Override
   public void deactivate() {
      this.stopBoost();
   }
}
