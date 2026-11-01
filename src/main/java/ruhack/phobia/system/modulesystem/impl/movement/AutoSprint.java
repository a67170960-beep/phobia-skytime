package ruhack.phobia.system.modulesystem.impl.movement;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.PlayerInput;
import net.minecraft.world.GameMode;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;

public class AutoSprint extends ModuleStructure {
   private final SelectSetting mode = new SelectSetting(
      "Режим", "Обычный постоянно зажимает спринт, Legit повторяет ванильное поведение", "Обычный", "Обычный", "Legit"
   );
   private final BooleanSetting omnidirectional = new BooleanSetting("Всенаправленный", "Спринт в любую сторону, а не только вперёд (менее легитно)")
      .setValue(false)
      .visible(() -> this.mode.isSelected("Legit"));
   private final BooleanSetting keepInWater = new BooleanSetting("Спринт в воде", "Разрешать спринт в воде")
      .setValue(false)
      .visible(() -> this.mode.isSelected("Legit"));
   private final BooleanSetting requireHunger = new BooleanSetting("Требовать голод", "Не спринтить при голоде ≤ 6 (как в ванилле)")
      .setValue(true)
      .visible(() -> this.mode.isSelected("Legit"));

   public AutoSprint() {
      super("AutoSprint", "Автоматический спринт с обычным и легитным режимами", ModuleCategory.MOVEMENT);
      this.settings(this.mode, this.omnidirectional, this.keepInWater, this.requireHunger);
   }

   @EventHandler
   public void tick(TickEvent event) {
      if (mc.player != null && mc.options != null) {
         if (this.mode.isSelected("Обычный")) {
            mc.options.sprintKey.setPressed(true);
         } else {
            mc.options.sprintKey.setPressed(this.shouldLegitSprint());
         }
      }
   }

   private boolean shouldLegitSprint() {
      if (mc.currentScreen != null || !this.hasMovementInput()) {
         return false;
      } else if (mc.player.isSneaking() || mc.player.isUsingItem() || mc.player.horizontalCollision) {
         return false;
      } else if (mc.player.hasStatusEffect(StatusEffects.BLINDNESS)) {
         return false;
      } else {
         return !this.keepInWater.isValue() && mc.player.isTouchingWater() ? false : !this.requireHunger.isValue() || this.hasHunger();
      }
   }

   private boolean hasMovementInput() {
      PlayerInput input = mc.player.input.playerInput;
      return this.omnidirectional.isValue() ? input.forward() || input.backward() || input.left() || input.right() : input.forward() && !input.backward();
   }

   private boolean hasHunger() {
      GameMode gameMode = mc.interactionManager != null ? mc.interactionManager.getCurrentGameMode() : null;
      return gameMode != GameMode.CREATIVE && gameMode != GameMode.SPECTATOR ? mc.player.getHungerManager().getFoodLevel() > 6 : true;
   }

   @Override
   public void deactivate() {
      if (mc.options != null) {
         mc.options.sprintKey.setPressed(false);
      }

      super.deactivate();
   }
}
