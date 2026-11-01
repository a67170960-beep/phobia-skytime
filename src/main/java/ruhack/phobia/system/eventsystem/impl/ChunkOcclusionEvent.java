package ruhack.phobia.system.eventsystem.impl;

import ruhack.phobia.system.eventsystem.api.events.callables.EventCancellable;

public class ChunkOcclusionEvent extends EventCancellable {
   private static final ChunkOcclusionEvent INSTANCE = new ChunkOcclusionEvent();

   public static ChunkOcclusionEvent get() {
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }
}
