package ruhack.phobia.system.eventsystem.impl;

import ruhack.phobia.system.eventsystem.api.events.callables.EventCancellable;

public class TabCompleteEvent extends EventCancellable {
   public final String prefix;
   public String[] completions;

   public TabCompleteEvent(String prefix) {
      this.prefix = prefix;
      this.completions = null;
   }

   public String getPrefix() {
      return this.prefix;
   }

   public void setCompletions(String[] completions) {
      this.completions = completions;
   }
}
