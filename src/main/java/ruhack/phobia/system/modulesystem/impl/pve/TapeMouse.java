package ruhack.phobia.system.modulesystem.impl.pve;

import lombok.Generated;
import net.minecraft.util.Hand;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.timer.StopWatch;

public class TapeMouse extends ModuleStructure {
   private final SelectSetting modeClick = new SelectSetting("Тип", "Тип клика", "Левая кнопка", "Левая кнопка", "Правая кнопка");
   private final SliderSettings delayForClick = new SliderSettings("Задержка", "Задержка между кликами", 1.0F).range(1.0F, 15.0F);
   private final StopWatch delay = new StopWatch();

   public TapeMouse() {
      super("TapeMouse", "Автоматически нажимает клавишу", ModuleCategory.PVE);
      this.settings(this.modeClick, this.delayForClick);
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         if (mc.currentScreen == null) {
            long delayMs = (long)(this.delayForClick.getValue() * 300.0F);
            if (this.delay.finished(delayMs)) {
               this.performClick();
               this.delay.reset();
            }
         }
      }
   }

   private void performClick() {
      if (this.modeClick.isSelected("Левая кнопка")) {
         this.leftClick();
      } else {
         this.rightClick();
      }
   }

   private void leftClick() {
      if (mc.interactionManager != null) {
         if (mc.targetedEntity != null) {
            mc.interactionManager.attackEntity(mc.player, mc.targetedEntity);
            mc.player.swingHand(Hand.MAIN_HAND);
         } else if (mc.crosshairTarget != null) {
            mc.doAttack();
         }
      }
   }

   private void rightClick() {
      if (mc.interactionManager != null) {
         mc.doItemUse();
      }
   }

   @Generated
   public SelectSetting getModeClick() {
      return this.modeClick;
   }

   @Generated
   public SliderSettings getDelayForClick() {
      return this.delayForClick;
   }

   @Generated
   public StopWatch getDelay() {
      return this.delay;
   }
}
