package ruhack.phobia.system.modulesystem.impl.render;

import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import ruhack.phobia.mixin.ClientWorldAccessor;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.istance.Instance;

public class Ambience extends ModuleStructure {
   private final SelectSetting mode = new SelectSetting("Режим", "Режим времени суток", "День", "День", "Полдень", "Ночь", "Полночь", "Свой");
   private final SliderSettings customTime = new SliderSettings("Время", "Установите свое время", 1000.0F)
      .range(0, 24000)
      .visible(() -> this.mode.isSelected("Свой"));
   private final SliderSettings saturation = new SliderSettings("Насыщенность", "Насыщенность мира", 0.0F).range(-1.0F, 1.0F).step(0.1F);
   private final SliderSettings brightness = new SliderSettings("Яркость", "Яркость мира", 0.0F).range(-1.0F, 1.0F).step(0.1F);

   public static Ambience getInstance() {
      return Instance.get(Ambience.class);
   }

   public Ambience() {
      super("Ambience", "Изменяет время суток в мире", ModuleCategory.RENDER);
      this.settings(this.mode, this.customTime, this.saturation, this.brightness);
   }

   public float getBrightnessValue() {
      return this.brightness.getValue();
   }

   public float getSaturationFactor() {
      return 1.0F + this.saturation.getValue();
   }

   @Override
   public void activate() {
      EventManager.register(this);
   }

   @Override
   public void deactivate() {
      EventManager.unregister(this);
   }

   public long getInternalTime() {
      return this.mode.isSelected("День")
         ? 1000L
         : (
            this.mode.isSelected("Полдень")
               ? 6000L
               : (this.mode.isSelected("Ночь") ? 13000L : (this.mode.isSelected("Полночь") ? 18000L : (long)this.customTime.getValue()))
         );
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.world != null) {
         ((ClientWorldAccessor)mc.world).phobia$getClientWorldProperties().setTimeOfDay(this.getInternalTime());
      }
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (event.getType() == PacketEvent.Type.RECEIVE && event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
         event.cancel();
      }
   }
}
