package ruhack.phobia.system.modulesystem.impl.player;

import ruhack.phobia.system.commandsystem.CommandManager;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.ChatEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.util.baritone.BaritoneHelper;
import ruhack.phobia.util.istance.Instance;

public class NoCommand extends ModuleStructure {
   public final BooleanSetting clientCommands = new BooleanSetting("Команды клиента", "Блокировать команды клиента (префикс .)").setValue(true);
   public final BooleanSetting baritoneCommands = new BooleanSetting("Baritone", "Блокировать команды Baritone (префикс #)").setValue(true);

   public NoCommand() {
      super("NoCommand", "Блокирует команды клиента и Baritone", ModuleCategory.PLAYER);
      this.settings(this.clientCommands, this.baritoneCommands);
   }

   public static NoCommand getInstance() {
      return Instance.get(NoCommand.class);
   }

   @Override
   public void activate() {
      if (this.baritoneCommands.isValue()) {
         BaritoneHelper.setCommandsEnabled(false);
      }
   }

   @Override
   public void deactivate() {
      BaritoneHelper.setCommandsEnabled(true);
   }

   @EventHandler(0)
   public void onChat(ChatEvent event) {
      String msg = event.getMessage();
      if (msg != null && !msg.isEmpty()) {
         if (this.clientCommands.isValue()) {
            String prefix = CommandManager.getInstance().getPrefix();
            if (prefix != null && !prefix.isEmpty() && msg.startsWith(prefix)) {
               event.cancel();
               return;
            }
         }

         if (this.baritoneCommands.isValue() && msg.startsWith("#")) {
            BaritoneHelper.setCommandsEnabled(false);
            event.cancel();
         }
      }
   }
}
