package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import net.minecraft.client.gui.screen.Screen;
import ruhack.phobia.system.eventsystem.api.events.Event;

public class SetScreenEvent implements Event {
   public Screen screen;

   @Generated
   public Screen getScreen() {
      return this.screen;
   }

   @Generated
   public void setScreen(Screen screen) {
      this.screen = screen;
   }

   @Generated
   public SetScreenEvent(Screen screen) {
      this.screen = screen;
   }
}
