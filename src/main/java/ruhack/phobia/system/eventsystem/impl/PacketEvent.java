package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import net.minecraft.network.packet.Packet;
import ruhack.phobia.system.eventsystem.api.events.callables.EventCancellable;

public class PacketEvent extends EventCancellable {
   private Packet<?> packet;
   private PacketEvent.Type type;

   public boolean isSend() {
      return this.type.equals(PacketEvent.Type.SEND);
   }

   @Generated
   public Packet<?> getPacket() {
      return this.packet;
   }

   @Generated
   public PacketEvent.Type getType() {
      return this.type;
   }

   @Generated
   public void setPacket(Packet<?> packet) {
      this.packet = packet;
   }

   @Generated
   public void setType(PacketEvent.Type type) {
      this.type = type;
   }

   @Generated
   public PacketEvent(Packet<?> packet, PacketEvent.Type type) {
      this.packet = packet;
      this.type = type;
   }

   public enum Type {
      SEND,
      RECEIVE;
   }
}
