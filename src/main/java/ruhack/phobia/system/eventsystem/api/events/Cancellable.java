package ruhack.phobia.system.eventsystem.api.events;

public interface Cancellable {
   boolean isCancelled();

   void cancel();
}
