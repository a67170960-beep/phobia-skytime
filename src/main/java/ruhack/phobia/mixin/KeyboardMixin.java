package ruhack.phobia.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil.Type;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.CharTypedEvent;
import ruhack.phobia.system.eventsystem.impl.KeyEvent;
import ruhack.phobia.system.screensystem.clickgui.ClickGui;

@Mixin(Keyboard.class)
public class KeyboardMixin {
   @Final
   @Shadow
   private MinecraftClient client;

   @Inject(method = "onKey", at = @At("HEAD"))
   private void onKey(long window, int action, KeyInput input, CallbackInfo ci) {
      if (input.key() != -1 && window == this.client.getWindow().getHandle()) {
         if (action == 0 && (input.key() == 344 || input.key() == 345 || input.key() == 96) && this.canOpenClickGui()) {
            ClickGui.INSTANCE.openGui();
         }

         EventManager.callEvent(new KeyEvent(this.client.currentScreen, Type.KEYSYM, input.key(), action));
      }
   }

   @Inject(method = "onChar", at = @At("HEAD"))
   private void onChar(long window, CharInput input, CallbackInfo ci) {
      if (window == this.client.getWindow().getHandle()) {
         char chr = (char)input.codepoint();
         EventManager.callEvent(new CharTypedEvent(chr));
      }
   }

   private boolean canOpenClickGui() {
      return this.client.world == null || this.client.player == null ? false : this.client.currentScreen == null;
   }
}
