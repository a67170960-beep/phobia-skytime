package ruhack.phobia.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.session.Session;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.system.configsystem.ConfigSystem;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.GameLeftEvent;
import ruhack.phobia.system.eventsystem.impl.HotBarUpdateEvent;
import ruhack.phobia.system.eventsystem.impl.SetScreenEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.impl.player.NoInteract;
import ruhack.phobia.system.screensystem.MainMenuScreen;
import ruhack.phobia.util.DWMApi;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
   @Shadow
   @Nullable
   public ClientPlayerEntity player;
   @Shadow
   @Nullable
   public ClientPlayerInteractionManager interactionManager;
   @Shadow
   @Final
   public GameRenderer gameRenderer;
   @Shadow
   public ClientWorld world;
   @Shadow
   private Window window;
   private static boolean fontsInitialized = false;
   @Shadow
   @Mutable
   private Session session;

   @Inject(method = "stop", at = @At("HEAD"))
   private void onStop(CallbackInfo ci) {
      ConfigSystem configSystem = ConfigSystem.getInstance();
      if (configSystem != null) {
         configSystem.shutdown();
      }
   }

   @Inject(method = "<init>", at = @At("TAIL"))
   private void applyDarkWindowFrame(CallbackInfo ci) {
      if (this.window != null) {
         DWMApi.setDarkMode(this.window.getHandle());
      }
   }

   @Inject(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Hand;values()[Lnet/minecraft/util/Hand;"), cancellable = true)
   public void doItemUseHook(CallbackInfo ci) {
      if (NoInteract.getInstance().isState()) {
         for (Hand hand : Hand.values()) {
            if (!this.player.getStackInHand(hand).isEmpty()) {
               ActionResult result = this.interactionManager.interactItem(this.player, hand);
               if (result.isAccepted()) {
                  if (result instanceof Success success && success.swingSource().equals(SwingSource.CLIENT)) {
                     this.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                     this.player.swingHand(hand);
                  }

                  ci.cancel();
               }
            }
         }
      }
   }

   @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;Z)V", at = @At("HEAD"))
   private void onDisconnect(Screen screen, boolean transferring, CallbackInfo info) {
      if (this.world != null) {
         EventManager.callEvent(GameLeftEvent.get());
      }
   }

   @Inject(method = "tick", at = @At("HEAD"))
   private void onTick(CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.currentScreen instanceof TitleScreen) {
         mc.setScreen(new MainMenuScreen());
      } else if (mc.player != null && mc.world != null) {
         EventManager.callEvent(new TickEvent());
      }
   }

   @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
   public void setScreenHook(Screen screen, CallbackInfo ci) {
      if (screen instanceof TitleScreen) {
         IMinecraft.mc.setScreen(new MainMenuScreen());
         ci.cancel();
      } else if (screen == null && this.world == null && IMinecraft.mc.getNetworkHandler() == null) {
         IMinecraft.mc.setScreen(new MainMenuScreen());
         ci.cancel();
      } else {
         SetScreenEvent event = new SetScreenEvent(screen);
         EventManager.callEvent(event);
         Screen eventScreen = event.getScreen();
         if (screen != eventScreen) {
            IMinecraft.mc.setScreen(eventScreen);
            ci.cancel();
         }
      }
   }

   @Inject(method = "getWindowTitle", at = @At("RETURN"), cancellable = true)
   private void getWindowTitle(CallbackInfoReturnable<String> cir) {
      cir.setReturnValue(String.format("Phobia Beta 1.21.11"));
   }

   @Inject(
      method = "handleInputEvents",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"),
      cancellable = true
   )
   public void handleInputEventsHook(CallbackInfo ci) {
      HotBarUpdateEvent event = new HotBarUpdateEvent();
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }
}
