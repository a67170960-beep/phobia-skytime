package ruhack.phobia.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.HandledScreenEvent;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
   @Shadow
   public int backgroundWidth;
   @Shadow
   public int backgroundHeight;
   @Shadow
   @Nullable
   protected Slot focusedSlot;

   @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
   private void phobia$drawShulkerPreview(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
      HandledScreenEvent event = new HandledScreenEvent(context, this.focusedSlot, mouseX, mouseY, this.backgroundWidth, this.backgroundHeight);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }
}
