package ruhack.phobia.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.screensystem.clickgui.ClickGui;

@Mixin(Screen.class)
public class ScreenMixin {
   @Inject(method = "renderWithTooltip", at = @At("HEAD"))
   private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      Screen self = (Screen)(Object)this;
      if (!(self instanceof ClickGui) && !(self instanceof ChatScreen)) {
         Render2D.clearOverrideTasks();
      }
   }
}
