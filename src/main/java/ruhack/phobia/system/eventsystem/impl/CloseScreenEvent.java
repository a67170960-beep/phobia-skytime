package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import net.minecraft.client.gui.screen.Screen;
import ruhack.phobia.system.eventsystem.api.events.callables.EventCancellable;

public class CloseScreenEvent extends EventCancellable {
   private Screen screen;

   @Generated
   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof CloseScreenEvent other)) {
         return false;
      } else {
         if (!other.canEqual(this)) {
            return false;
         }

         if (!super.equals(o)) {
            return false;
         }

         Object this$screen = this.getScreen();
         Object other$screen = other.getScreen();
         return this$screen == null ? other$screen == null : this$screen.equals(other$screen);
      }
   }

   @Generated
   protected boolean canEqual(Object other) {
      return other instanceof CloseScreenEvent;
   }

   @Generated
   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = super.hashCode();
      Object $screen = this.getScreen();
      return result * 59 + ($screen == null ? 43 : $screen.hashCode());
   }

   @Generated
   public Screen getScreen() {
      return this.screen;
   }

   @Generated
   public void setScreen(Screen screen) {
      this.screen = screen;
   }

   @Generated
   @Override
   public String toString() {
      return "CloseScreenEvent(screen=" + this.getScreen() + ")";
   }

   @Generated
   public CloseScreenEvent(Screen screen) {
      this.screen = screen;
   }
}
