package ruhack.phobia.system.modulesystem.category;

import lombok.Generated;

public enum ModuleCategory {
   RAGE("Rage"),
   LEGIT("Legit"),
   MOVEMENT("Movement"),
   PLAYER("Player"),
   PVE("PvE"),
   RENDER("Render"),
   DISPLAY("Display"),
   ESP("ESP"),
   MISC("Miscellaneous"),
   BINDS("Binds"),
   CONFIG("Configs"),
   OTHER("Other");

   final String readableName;

   @Generated
   ModuleCategory(final String readableName) {
      this.readableName = readableName;
   }

   @Generated
   public String getReadableName() {
      return this.readableName;
   }
}
