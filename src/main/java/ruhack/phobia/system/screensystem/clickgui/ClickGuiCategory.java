package ruhack.phobia.system.screensystem.clickgui;

import ruhack.phobia.system.modulesystem.category.ModuleCategory;

public enum ClickGuiCategory {
   COMBAT("Combat", "\ue002", ModuleCategory.RAGE, ModuleCategory.LEGIT),
   MOVEMENT("Movement", "C", ModuleCategory.MOVEMENT),
   PLAYER("Player", "Y", ModuleCategory.PLAYER, ModuleCategory.PVE),
   VISUALS("Visuals", "a", ModuleCategory.RENDER, ModuleCategory.DISPLAY, ModuleCategory.ESP),
   MISC("Misc", "\ue005", ModuleCategory.MISC, ModuleCategory.BINDS),
   THEME("Theme", "T", ModuleCategory.OTHER),
   IRC("IRC", "\ue00b");

   private final String readableName;
   private final String icon;
   private final ModuleCategory[] moduleCategories;

   ClickGuiCategory(String readableName, String icon, ModuleCategory... moduleCategories) {
      this.readableName = readableName;
      this.icon = icon;
      this.moduleCategories = moduleCategories;
   }

   public String getReadableName() {
      return this.readableName;
   }

   public String getIcon() {
      return this.icon;
   }

   public boolean contains(ModuleCategory category) {
      for (ModuleCategory value : this.moduleCategories) {
         if (value == category) {
            return true;
         }
      }

      return false;
   }

   public static ClickGuiCategory fromModuleCategory(ModuleCategory category) {
      for (ClickGuiCategory value : values()) {
         if (value.contains(category)) {
            return value;
         }
      }

      return MISC;
   }
}
