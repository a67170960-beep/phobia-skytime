package ruhack.phobia.mixin;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.RunCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.commandsystem.CommandManager;
import ruhack.phobia.system.dragsystem.Drag;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
   protected ChatScreenMixin(Text title) {
      super(title);
   }

   @Inject(method = "render", at = @At("TAIL"))
   private void onRender(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
      Drag.onDraw(context, mouseX, mouseY, deltaTicks, true);
   }

   @Inject(method = "handleClickEvent", at = @At("HEAD"), cancellable = true)
   private void phobia$handleClientCommandClick(Style style, boolean insert, CallbackInfoReturnable<Boolean> cir) {
      if (style != null && style.getClickEvent() instanceof RunCommand runCommand) {
         CommandManager manager = CommandManager.getInstance();
         if (manager != null) {
            String command = runCommand.command();
            String prefix = manager.getPrefix();
            if (command != null && prefix != null && !prefix.isEmpty() && command.startsWith(prefix)) {
               String clientCommand = command.substring(prefix.length()).trim();
               if (!clientCommand.isEmpty()) {
                  String commandName = clientCommand.split("\\s+", 2)[0];
                  if (manager.getCommand(commandName) != null) {
                     manager.execute(clientCommand);
                     cir.setReturnValue(true);
                  }
               }
            }
         }
      }
   }

   @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
      int mouseX = (int)click.x();
      int mouseY = (int)click.y();
      int button = click.button();
      if (Initialization.getInstance() != null
         && Initialization.getInstance().getManager() != null
         && Initialization.getInstance().getManager().getHudManager() != null
         && Initialization.getInstance().getManager().getHudManager().mouseClicked(mouseX, mouseY, button)) {
         cir.setReturnValue(true);
      } else {
         Drag.onMouseClick(click);
         if (Drag.isDragging()) {
            cir.setReturnValue(true);
         }
      }
   }

   public boolean mouseReleased(Click click) {
      Drag.onMouseRelease(click);
      return super.mouseReleased(click);
   }

   public boolean mouseDragged(Click click, double offsetX, double offsetY) {
      return super.mouseDragged(click, offsetX, offsetY);
   }

   @Inject(method = "removed", at = @At("HEAD"))
   private void onRemoved(CallbackInfo ci) {
      Drag.resetDragging();
   }

   @Inject(method = "close", at = @At("HEAD"))
   private void onClose(CallbackInfo ci) {
      Drag.resetDragging();
   }
}
