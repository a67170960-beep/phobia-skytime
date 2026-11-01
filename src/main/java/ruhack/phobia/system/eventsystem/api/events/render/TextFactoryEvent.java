package ruhack.phobia.system.eventsystem.api.events.render;

import lombok.Generated;
import ruhack.phobia.system.eventsystem.api.events.Event;

public class TextFactoryEvent implements Event {
   private String text;

   public void replaceText(String protect, String replaced) {
      if (this.text != null && !this.text.isEmpty()) {
         if (this.text.contains(protect)
            && (
               this.text.equalsIgnoreCase(protect)
                  || this.text.contains(protect + " ")
                  || this.text.contains(" " + protect)
                  || this.text.contains("⏏" + protect)
                  || this.text.contains(protect + "§")
            )) {
            this.text = this.text.replace(protect, replaced);
         }
      }
   }

   @Generated
   public void setText(String text) {
      this.text = text;
   }

   @Generated
   public String getText() {
      return this.text;
   }

   @Generated
   public TextFactoryEvent(String text) {
      this.text = text;
   }
}
