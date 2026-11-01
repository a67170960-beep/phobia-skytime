package ruhack.phobia.system.modulesystem.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.Hand;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.HandOffsetEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;

public class ViewModel extends ModuleStructure {
   private final SliderSettings mainHandXSetting = new SliderSettings("Основная рука X", "Настройка значения X для основной руки", 0.0F).range(-1.0F, 1.0F);
   private final SliderSettings mainHandYSetting = new SliderSettings("Основная рука Y", "Настройка значения Y для основной руки", 0.0F).range(-1.0F, 1.0F);
   private final SliderSettings mainHandZSetting = new SliderSettings("Основная рука Z", "Настройка значения Z для основной руки", 0.0F).range(-2.5F, 2.5F);
   private final SliderSettings offHandXSetting = new SliderSettings("Второстепенная рука X", "Настройка значения X для второстепенной руки", 0.0F)
      .range(-1.0F, 1.0F);
   private final SliderSettings offHandYSetting = new SliderSettings("Второстепенная рука Y", "Настройка значения Y для второстепенной руки", 0.0F)
      .range(-1.0F, 1.0F);
   private final SliderSettings offHandZSetting = new SliderSettings("Второстепенная рука Z", "Настройка значения Z для второстепенной руки", 0.0F)
      .range(-2.5F, 2.5F);

   public ViewModel() {
      super("ViewModel", "Изменения позиции руки", ModuleCategory.RENDER);
      this.settings(this.mainHandXSetting, this.mainHandYSetting, this.mainHandZSetting, this.offHandXSetting, this.offHandYSetting, this.offHandZSetting);
   }

   @EventHandler
   public void onHandOffset(HandOffsetEvent e) {
      if (!e.getStack().isEmpty()) {
         Hand hand = e.getHand();
         if (!hand.equals(Hand.MAIN_HAND) || !(e.getStack().getItem() instanceof CrossbowItem)) {
            MatrixStack matrix = e.getMatrices();
            if (hand.equals(Hand.MAIN_HAND)) {
               matrix.translate(this.mainHandXSetting.getValue(), this.mainHandYSetting.getValue(), this.mainHandZSetting.getValue());
            } else {
               matrix.translate(this.offHandXSetting.getValue(), this.offHandYSetting.getValue(), this.offHandZSetting.getValue());
            }
         }
      }
   }
}
