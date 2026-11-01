package ruhack.phobia.system.modulesystem.impl.misc;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket.Status;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.util.timer.TimerUtil;

public class ServerRPSpoofer extends ModuleStructure {
   private ServerRPSpoofer.ResourcePackAction currentAction = ServerRPSpoofer.ResourcePackAction.WAIT;
   private final TimerUtil counter = TimerUtil.create();

   public ServerRPSpoofer() {
      super("ServerRPSpoof", "Убирает нужду загрузить ресурспак от сервера", ModuleCategory.MISC);
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (e.getPacket() instanceof ResourcePackSendS2CPacket) {
         this.currentAction = ServerRPSpoofer.ResourcePackAction.ACCEPT;
         e.cancel();
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
      if (networkHandler != null) {
         this.processResourcePackAction(networkHandler);
      }
   }

   private void processResourcePackAction(ClientPlayNetworkHandler networkHandler) {
      if (this.currentAction == ServerRPSpoofer.ResourcePackAction.ACCEPT) {
         networkHandler.sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), Status.ACCEPTED));
         this.currentAction = ServerRPSpoofer.ResourcePackAction.SEND;
         this.counter.resetCounter();
      } else if (this.currentAction == ServerRPSpoofer.ResourcePackAction.SEND && this.counter.isReached(300L)) {
         networkHandler.sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), Status.SUCCESSFULLY_LOADED));
         this.currentAction = ServerRPSpoofer.ResourcePackAction.WAIT;
      }
   }

   @Override
   public void deactivate() {
      this.currentAction = ServerRPSpoofer.ResourcePackAction.WAIT;
      super.deactivate();
   }

   public enum ResourcePackAction {
      ACCEPT,
      SEND,
      WAIT;
   }
}
