package ruhack.phobia.system.eventsystem.impl;

import ruhack.phobia.system.eventsystem.api.events.Event;

public class WorldChangeEvent implements Event {
   private static final WorldChangeEvent INSTANCE = new WorldChangeEvent();

   public static WorldChangeEvent get() {
      return INSTANCE;
   }
}
