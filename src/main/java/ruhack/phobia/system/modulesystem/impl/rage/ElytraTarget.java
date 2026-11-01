package ruhack.phobia.system.modulesystem.impl.rage;

import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.KeyEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BindSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.istance.Instance;
import ruhack.phobia.util.string.chat.ChatMessage;

public class ElytraTarget extends ModuleStructure {
   public SliderSettings elytraFindRange = new SliderSettings("Дистанция наводки", "Дальность поиска цели во время полета на элитре", 32.0F).range(6.0F, 64.0F);
   public SliderSettings elytraForward = new SliderSettings("Значение перегона", "Сила перегона от противника", 3.0F).range(0.0F, 6.0F);
   final BindSetting forward = new BindSetting("Кнопка вкл/выкл перегона", "");
   public static boolean shouldElytraTarget = false;

   public static ElytraTarget getInstance() {
      return Instance.get(ElytraTarget.class);
   }

   public ElytraTarget() {
      super("ElytraTarget", "Таргетит таргета ауры на элитре", ModuleCategory.RAGE);
      this.settings(this.elytraFindRange, this.elytraForward, this.forward);
   }

   @EventHandler
   private void onEventKey(KeyEvent e) {
      if (e.isKeyDown(this.forward.getKey())) {
         shouldElytraTarget = !shouldElytraTarget;
         ChatMessage.brandmessage("Elytra Forward " + (shouldElytraTarget ? "включен" : "выключен"));
      }
   }
}
