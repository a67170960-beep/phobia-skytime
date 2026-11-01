package ruhack.phobia.system.modulesystem.impl.rage;

import lombok.Generated;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;

public class AutoGApple extends ModuleStructure {
   private final SliderSettings healthThreshold = new SliderSettings("Здоровье", "Порог здоровья для еды", 16.0F).range(3.0F, 20.0F);
   private final BooleanSetting smartMode = new BooleanSetting("Умный", "Есть из хотбара, иначе только с левой руки").setValue(true);
   private final BooleanSetting goldenHearts = new BooleanSetting("Золотые сердца", "Учитывать absorption").setValue(true);
   private final BooleanSetting returnSlot = new BooleanSetting("Возвращать слот", "Возвращать предыдущий слот после еды").setValue(true);
   private boolean isEating = false;
   private int previousSlot = -1;

   public AutoGApple() {
      super("AutoGApple", "Автоматически ест золотые яблоки", ModuleCategory.RAGE);
      this.settings(this.healthThreshold, this.smartMode, this.goldenHearts, this.returnSlot);
   }

   @Override
   public void activate() {
      this.isEating = false;
      this.previousSlot = -1;
   }

   @Override
   public void deactivate() {
      this.stopEating();
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         this.handleEating();
      }
   }

   private void handleEating() {
      if (this.canEat()) {
         if (this.smartMode.isValue()) {
            if (!this.hasGappleInHand()) {
               this.swapToGappleSlot();
            }

            this.startEating();
         } else if (this.hasGappleInOffhand()) {
            this.startEating();
         }
      } else if (this.isEating && !this.shouldContinueEating()) {
         this.stopEating();
      }
   }

   private boolean hasGappleInHand() {
      ItemStack mainHand = mc.player.getMainHandStack();
      return mainHand.getItem() == Items.GOLDEN_APPLE;
   }

   private boolean canEat() {
      if (mc.player.isDead()) {
         return false;
      }

      if (mc.player.getItemCooldownManager().isCoolingDown(Items.GOLDEN_APPLE.getDefaultStack())) {
         return false;
      }

      if (this.smartMode.isValue()) {
         if (!this.hasGapple()) {
            return false;
         }
      } else if (!this.hasGappleInOffhand()) {
         return false;
      }

      float health = this.getEffectiveHealth();
      return health <= this.healthThreshold.getValue();
   }

   private boolean shouldContinueEating() {
      if (mc.player.isDead()) {
         return false;
      }

      if (!mc.player.isUsingItem()) {
         return false;
      }

      ItemStack usingItem = mc.player.getActiveItem();
      return usingItem.getItem() == Items.GOLDEN_APPLE;
   }

   private float getEffectiveHealth() {
      float health = mc.player.getHealth();
      if (this.goldenHearts.isValue()) {
         health += mc.player.getAbsorptionAmount();
      }

      return health;
   }

   private boolean hasGappleInOffhand() {
      ItemStack offhandStack = mc.player.getOffHandStack();
      return offhandStack.getItem() == Items.GOLDEN_APPLE;
   }

   private boolean hasGappleInHotbar() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.GOLDEN_APPLE) {
            return true;
         }
      }

      return false;
   }

   private boolean hasGapple() {
      return this.hasGappleInOffhand() || this.hasGappleInHotbar();
   }

   private int findGappleInHotbar() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.GOLDEN_APPLE) {
            return i;
         }
      }

      return -1;
   }

   private void swapToGappleSlot() {
      int gappleSlot = this.findGappleInHotbar();
      if (gappleSlot != -1) {
         int currentSlot = mc.player.getInventory().getSelectedSlot();
         if (currentSlot != gappleSlot) {
            if (this.previousSlot == -1 && this.returnSlot.isValue()) {
               this.previousSlot = currentSlot;
            }

            mc.player.getInventory().setSelectedSlot(gappleSlot);
         }
      }
   }

   private void startEating() {
      if (!this.isEating && !mc.options.useKey.isPressed()) {
         mc.options.useKey.setPressed(true);
         this.isEating = true;
      }
   }

   private void stopEating() {
      if (this.isEating) {
         mc.options.useKey.setPressed(false);
         this.isEating = false;
         if (this.previousSlot != -1 && this.returnSlot.isValue()) {
            mc.player.getInventory().setSelectedSlot(this.previousSlot);
            this.previousSlot = -1;
         }
      }
   }

   @Generated
   public SliderSettings getHealthThreshold() {
      return this.healthThreshold;
   }

   @Generated
   public BooleanSetting getSmartMode() {
      return this.smartMode;
   }

   @Generated
   public BooleanSetting getGoldenHearts() {
      return this.goldenHearts;
   }

   @Generated
   public BooleanSetting getReturnSlot() {
      return this.returnSlot;
   }

   @Generated
   public boolean isEating() {
      return this.isEating;
   }

   @Generated
   public int getPreviousSlot() {
      return this.previousSlot;
   }
}
