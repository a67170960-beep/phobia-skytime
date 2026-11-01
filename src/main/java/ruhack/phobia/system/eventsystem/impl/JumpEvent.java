package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import net.minecraft.entity.player.PlayerEntity;
import ruhack.phobia.system.eventsystem.api.events.callables.EventCancellable;

public class JumpEvent extends EventCancellable {
   private PlayerEntity player;

   @Generated
   public PlayerEntity getPlayer() {
      return this.player;
   }

   @Generated
   public JumpEvent(PlayerEntity player) {
      this.player = player;
   }
}
