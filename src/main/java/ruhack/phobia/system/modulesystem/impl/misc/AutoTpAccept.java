package ruhack.phobia.system.modulesystem.impl.misc;

import java.util.Arrays;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.friendsystem.FriendUtils;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.util.ServerUtil;

public class AutoTpAccept extends ModuleStructure {
   private final String[] teleportMessages = new String[]{
      "has requested teleport", "просит телепортироваться", "хочет телепортироваться к вам", "просит к вам телепортироваться"
   };
   private boolean canAccept;
   private final BooleanSetting friendSetting = new BooleanSetting("Только друзья", "Будет принимать запросы только от друзей").setValue(true);

   public AutoTpAccept() {
      super("AutoTpAccept", "Автоматически принимает запросы на телепортацию", ModuleCategory.MISC);
      this.settings(this.friendSetting);
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (e.getPacket() instanceof GameMessageS2CPacket m) {
         String message = m.content().getString();
         boolean validPlayer = !this.friendSetting.isValue() || FriendUtils.getFriends().stream().anyMatch(s -> message.contains(s.getName()));
         if (this.isTeleportMessage(message)) {
            this.canAccept = validPlayer;
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (!ServerUtil.isPvp() && this.canAccept) {
         mc.player.networkHandler.sendChatCommand("tpaccept");
         this.canAccept = false;
      }
   }

   private boolean isTeleportMessage(String message) {
      return Arrays.stream(this.teleportMessages).map(String::toLowerCase).anyMatch(message::contains);
   }
}
