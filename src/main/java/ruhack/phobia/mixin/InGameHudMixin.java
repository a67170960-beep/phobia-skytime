package ruhack.phobia.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruhack.phobia.system.modulesystem.impl.render.NoRender;
import ruhack.phobia.system.screensystem.clickgui.ClickGui;

@Mixin(InGameHud.class)
public class InGameHudMixin {
   @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
   private void onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (MinecraftClient.getInstance().currentScreen instanceof ClickGui || MinecraftClient.getInstance().currentScreen instanceof GameMenuScreen) {
         ci.cancel();
      }
   }

   @Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true)
   private void onRenderNauseaOverlay(DrawContext context, float nauseaStrength, CallbackInfo ci) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Nausea")) {
         ci.cancel();
      }
   }

   @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
   private void onRenderScoreboard(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Scoreboard")) {
         ci.cancel();
      }
   }

   @Inject(method = "renderBossBarHud", at = @At("HEAD"), cancellable = true)
   private void onRenderBossBar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("BossBar")) {
         ci.cancel();
      }
   }
}
