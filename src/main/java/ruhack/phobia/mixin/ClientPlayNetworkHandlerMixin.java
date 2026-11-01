package ruhack.phobia.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.ChatEvent;
import ruhack.phobia.system.eventsystem.impl.GameLeftEvent;
import ruhack.phobia.system.eventsystem.impl.WorldChangeEvent;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin implements IMinecraft {
   @Shadow
   private ClientWorld world;
   @Unique
   private boolean worldNotNull;

   @Shadow
   private static ItemStack getActiveDeathProtector(PlayerEntity player) {
      return null;
   }

   @Inject(method = "onGameJoin", at = @At("HEAD"))
   private void onGameJoinHead(GameJoinS2CPacket packet, CallbackInfo info) {
      this.worldNotNull = this.world != null;
   }

   @Inject(method = "onGameJoin", at = @At("TAIL"))
   private void onGameJoinTail(GameJoinS2CPacket packet, CallbackInfo info) {
      if (this.worldNotNull) {
         EventManager.callEvent(GameLeftEvent.get());
      }
   }

   @Inject(method = "onGameJoin", at = @At("RETURN"))
   private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
      EventManager.callEvent(WorldChangeEvent.get());
   }

   @Inject(method = "onPlayerRespawn", at = @At("RETURN"))
   private void onPlayerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
      EventManager.callEvent(WorldChangeEvent.get());
   }

   @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
   private void sendChatMessage(String message, CallbackInfo ci) {
      ChatEvent event = new ChatEvent(message);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "sendChatCommand", at = @At("HEAD"), cancellable = true)
   private void sendChatCommand(String command, CallbackInfo ci) {
      ChatEvent event = new ChatEvent("/" + command);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "onGameMessage", at = @At("HEAD"), cancellable = true)
   private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
      String message = packet.content().getString();
      ChatEvent event = new ChatEvent(message);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }
}
