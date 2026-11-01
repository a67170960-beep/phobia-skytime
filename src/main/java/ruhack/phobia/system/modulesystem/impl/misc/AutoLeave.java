package ruhack.phobia.system.modulesystem.impl.misc;

import net.minecraft.text.Text;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.friendsystem.FriendUtils;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.ServerUtil;

public class AutoLeave extends ModuleStructure {
   private final SelectSetting leaveType = new SelectSetting("Тип выхода", "Позволяет выбрать тип выхода", "Hub", "Hub", "Main Menu");
   private final MultiSelectSetting triggerSetting = new MultiSelectSetting("Триггеры", "Выберите, в каких случаях произойдет выход")
      .value("Players", "Staff")
      .selected("Players", "Staff");
   private final SliderSettings distanceSetting = new SliderSettings("Максимальная дистанция", "Максимальная дистанция для активации авто-выхода", 20.0F)
      .range(5, 40)
      .visible(() -> this.triggerSetting.isSelected("Players"));

   public AutoLeave() {
      super("AutoLeave", "Автоматически выходит с режима если рядом игрок", ModuleCategory.MISC);
      this.settings(this.leaveType, this.triggerSetting, this.distanceSetting);
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (!ServerUtil.isPvp()) {
         if (this.triggerSetting.isSelected("Players")) {
            mc.world
               .getPlayers()
               .stream()
               .filter(p -> mc.player.distanceTo(p) < this.distanceSetting.getValue() && mc.player != p && !FriendUtils.isFriend(p))
               .findFirst()
               .ifPresent(p -> this.leave(p.getName().copy().append(" - Появился рядом " + mc.player.distanceTo(p) + "м")));
         }
      }
   }

   public void leave(Text text) {
      switch (this.leaveType.getValue()) {
         case "Hub":
            mc.getNetworkHandler().sendChatCommand("hub");
            break;
         case "Main Menu":
            mc.getNetworkHandler().getConnection().disconnect(Text.of("[Auto Leave] \n").copy().append(text));
      }

      this.setState(false);
   }
}
