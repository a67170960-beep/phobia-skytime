package ruhack.phobia.system.screensystem;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import ruhack.phobia.system.modulesystem.impl.rage.AutoSwap;

public final class AutoSwapInventoryScreen extends InventoryScreen {
   private final AutoSwap autoSwap;
   private final AutoSwapRingScreen ringScreen;
   private final int ringSlot;

   public AutoSwapInventoryScreen(ClientPlayerEntity player, AutoSwap autoSwap, AutoSwapRingScreen ringScreen, int ringSlot) {
      super(player);
      this.autoSwap = autoSwap;
      this.ringScreen = ringScreen;
      this.ringSlot = ringSlot;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
      context.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal("Выберите Player Head или тотем"),
         this.width / 2,
         Math.max(8, (this.height - this.backgroundHeight) / 2 - 14),
         -921608
      );
   }

   public boolean mouseClicked(Click click, boolean doubled) {
      if (click.button() == 0 && this.focusedSlot != null && this.focusedSlot.hasStack()) {
         ItemStack stack = this.focusedSlot.getStack();
         if (this.autoSwap.isSelectableRingItem(stack)) {
            this.autoSwap.selectRingItem(this.ringSlot, stack);
            if (this.client != null) {
               this.client.setScreen(this.ringScreen);
            }

            return true;
         }
      }

      return super.mouseClicked(click, doubled);
   }

   public void close() {
      if (this.client != null) {
         this.client.setScreen(this.ringScreen);
      }
   }
}
