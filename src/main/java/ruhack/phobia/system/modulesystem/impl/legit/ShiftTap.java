package ruhack.phobia.system.modulesystem.impl.legit;

import net.minecraft.client.MinecraftClient;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.AttackEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;

public class ShiftTap extends ModuleStructure {
   private long shiftTapEndTime = 0L;
   private boolean isModuleControllingSneak = false;
   private int shiftTapDuration = 100;
   public final SliderSettings speed = new SliderSettings("Время", "Время удержания шифта", 50.0F).range(5.0F, 250.0F).step(5.0F);
   private final MinecraftClient mc = MinecraftClient.getInstance();

   public ShiftTap() {
      super("ShiftTap", "Автоматически нажимает шифт при ударе", ModuleCategory.LEGIT);
      this.settings(this.speed);
   }

   private void startShiftTap() {
      this.shiftTapEndTime = System.currentTimeMillis() + this.speed.getInt();
      if (!this.isModuleControllingSneak) {
         this.mc.options.sneakKey.setPressed(true);
         this.isModuleControllingSneak = true;
      }
   }

   private void stopShiftTap() {
      if (this.isModuleControllingSneak) {
         this.mc.options.sneakKey.setPressed(false);
         this.isModuleControllingSneak = false;
      }
   }

   @EventHandler
   public void onAttack(AttackEvent event) {
      if (this.mc.player != null) {
         this.startShiftTap();
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (this.mc.player != null && !this.mc.player.isSpectator()) {
         long currentTime = System.currentTimeMillis();
         if (this.isModuleControllingSneak && currentTime > this.shiftTapEndTime) {
            this.stopShiftTap();
         }
      } else {
         this.stopShiftTap();
      }
   }
}
