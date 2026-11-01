package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import ruhack.phobia.system.eventsystem.api.events.callables.EventCancellable;

public class ChatEvent extends EventCancellable {
   private String message;

   @Generated
   public ChatEvent(String message) {
      this.message = message;
   }

   @Generated
   public String getMessage() {
      return this.message;
   }

   @Generated
   public void setMessage(String message) {
      this.message = message;
   }
}
