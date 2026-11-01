package ruhack.phobia.system.commandsystem.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ruhack.phobia.system.commandsystem.Command;
import ruhack.phobia.system.commandsystem.CommandManager;
import ruhack.phobia.system.configsystem.impl.prefix.PrefixConfig;

public class PrefixCommand extends Command {
   public PrefixCommand() {
      super("prefix", "Изменение префикса команд");
   }

   @Override
   public void execute(String label, String[] args) {
      CommandManager manager = CommandManager.getInstance();
      if (args.length == 0) {
         this.logDirectRaw(Text.literal(HelpCommand.getLine()));
         this.logDirect("§7Префикс команд:");
         this.logDirectRaw(Text.literal(HelpCommand.getLine()));
         this.logDirect("§7Текущий префикс: §f" + manager.getPrefix());
         this.logDirect("§7> prefix set <symbol> §8- §fУстановить новый префикс");
         this.logDirectRaw(Text.literal(HelpCommand.getLine()));
      } else {
         String action = args[0].toLowerCase();
         if (action.equals("set")) {
            if (args.length < 2) {
               this.logDirect("Использование: prefix set <symbol>", Formatting.RED);
               return;
            }

            String newPrefix = args[1];
            if (newPrefix.length() > 3) {
               this.logDirect("Префикс не может быть длиннее 3 символов!", Formatting.RED);
               return;
            }

            if (newPrefix.contains(" ")) {
               this.logDirect("Префикс не может содержать пробелы!", Formatting.RED);
               return;
            }

            PrefixConfig.getInstance().setPrefixAndSave(newPrefix);
            this.logDirect(String.format("§aПрефикс изменен на: §f%s", newPrefix), Formatting.GREEN);
            this.logDirect(String.format("§7Теперь команды вводятся как: §f%shelp", newPrefix), Formatting.GREEN);
         } else {
            this.logDirect("Использование: prefix set <symbol>", Formatting.RED);
         }
      }
   }

   @Override
   public Stream<String> tabComplete(String label, String[] args) {
      if (args.length == 1) {
         return Stream.of("set").filter(s -> s.startsWith(args[0].toLowerCase()));
      } else {
         return args.length == 2 && args[0].equalsIgnoreCase("set")
            ? Stream.of(".", "!", "$", "#", "-", "/").filter(s -> s.startsWith(args[1]))
            : Stream.empty();
      }
   }

   @Override
   public String getShortDesc() {
      return "Изменение префикса команд";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "Команда для изменения префикса команд в чите",
         "Использование:",
         "> prefix - Показать текущий префикс",
         "> prefix set <symbol> - Установить новый префикс"
      );
   }
}
