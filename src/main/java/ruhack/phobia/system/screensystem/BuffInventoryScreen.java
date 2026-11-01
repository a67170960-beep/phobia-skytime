package ruhack.phobia.system.screensystem;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import ruhack.phobia.system.modulesystem.impl.rage.BuffHelper;

public final class BuffInventoryScreen extends InventoryScreen {
   private final BuffHelper helper;
   private final BuffRingScreen ringScreen;
   private final int ringSlot;

   public BuffInventoryScreen(ClientPlayerEntity player, BuffHelper helper, BuffRingScreen ringScreen, int ringSlot) {
      super(player);
      this.helper = helper;
      this.ringScreen = ringScreen;
      this.ringSlot = ringSlot;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
      context.drawCenteredTextWithShadow(
         this.textRenderer, Text.literal("Выберите взрывное зелье"), this.width / 2, Math.max(8, (this.height - this.backgroundHeight) / 2 - 14), -921608
      );
   }

   public boolean mouseClicked(Click click, boolean doubled) {
      if (click.button() == 0 && this.focusedSlot != null && this.focusedSlot.hasStack()) {
         ItemStack stack = this.focusedSlot.getStack();
         if (this.helper.isSelectablePotion(stack)) {
            this.helper.selectPotion(this.ringSlot, stack);
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
