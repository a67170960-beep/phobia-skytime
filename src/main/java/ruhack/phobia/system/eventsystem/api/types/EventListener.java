package ruhack.phobia.system.eventsystem.api.types;

import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.eventsystem.impl.UsingItemEvent;

public class EventListener implements Listener {
   public static boolean serverSprint;
   public static int selectedSlot;

   @EventHandler
   public void onTick(TickEvent e) {
      if (Initialization.getInstance().getManager().getHudManager() != null) {
         Initialization.getInstance().getManager().getHudManager().tick();
      }

      Initialization.getInstance().getManager().getAttackPerpetrator().tick();
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      switch (e.getPacket()) {
         case ClientCommandC2SPacket command:
            serverSprint = switch (command.getMode()) {
               case START_SPRINTING -> true;
               case STOP_SPRINTING -> false;
               default -> serverSprint;
            };
            break;
         case UpdateSelectedSlotC2SPacket slot:
            selectedSlot = slot.getSelectedSlot();
            break;
         default:
      }

      Initialization.getInstance().getManager().getHudManager().onPacket(e);
   }

   @EventHandler
   public void onUsingItemEvent(UsingItemEvent e) {
   }
}
