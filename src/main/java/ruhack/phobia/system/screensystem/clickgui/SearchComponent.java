package ruhack.phobia.system.screensystem.clickgui;

import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

public final class SearchComponent {
   private boolean active;
   private String query = "";
   private boolean selectAll;

   public boolean isActive() {
      return this.active;
   }

   public String getQuery() {
      return this.query;
   }

   public boolean hasQuery() {
      return !this.query.isBlank();
   }

   public void open() {
      this.active = true;
   }

   public void close() {
      this.active = false;
      this.query = "";
      this.selectAll = false;
   }

   public boolean keyPressed(KeyInput input) {
      if (!this.active) {
         return false;
      }

      if (input.key() == 256) {
         this.close();
         return true;
      }

      if (input.key() == 259) {
         if (this.selectAll) {
            this.query = "";
            this.selectAll = false;
         } else if (!this.query.isEmpty()) {
            this.query = this.query.substring(0, this.query.length() - 1);
         }

         return true;
      } else if (input.key() == 261) {
         this.query = "";
         return true;
      } else if (input.key() == 65 && (input.modifiers() & 2) != 0) {
         this.selectAll = true;
         return true;
      } else {
         return false;
      }
   }

   public boolean charTyped(CharInput input) {
      if (this.active && !Character.isISOControl(input.codepoint())) {
         if (this.selectAll) {
            this.query = "";
            this.selectAll = false;
         }

         this.query = this.query + new String(Character.toChars(input.codepoint()));
         return true;
      } else {
         return false;
      }
   }
}
