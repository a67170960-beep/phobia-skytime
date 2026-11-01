package ruhack.phobia.system.commandsystem.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.RunCommand;
import net.minecraft.text.HoverEvent.ShowText;
import net.minecraft.util.Formatting;
import ruhack.phobia.system.commandsystem.Command;
import ruhack.phobia.system.commandsystem.CommandManager;
import ruhack.phobia.system.commandsystem.helpers.Paginator;
import ruhack.phobia.system.commandsystem.helpers.TabCompleteHelper;
import ruhack.phobia.system.macrosystem.Macro;
import ruhack.phobia.system.macrosystem.MacroRepository;
import ruhack.phobia.util.string.KeyHelper;

public class MacroCommand extends Command {
   public MacroCommand() {
      super("macro", "Управление макросами", "macros");
   }

   @Override
   public void execute(String label, String[] args) {
      CommandManager manager = CommandManager.getInstance();
      MacroRepository macroRepository = MacroRepository.getInstance();
      String action = args.length > 0 ? args[0].toLowerCase(Locale.US) : "list";
      switch (action) {
         case "add":
            if (args.length < 4) {
               this.logDirect("Использование: macro add <key> <name> <message>", Formatting.RED);
               return;
            }

            String addKeyName = args[1];
            int addKey = KeyHelper.getKeyCode(addKeyName);
            if (addKey == -1) {
               this.logDirect(String.format("Неизвестная клавиша: %s", addKeyName), Formatting.RED);
               return;
            }

            String addName = args[2];
            StringBuilder messageBuilder = new StringBuilder();

            for (int i = 3; i < args.length; i++) {
               if (i > 3) {
                  messageBuilder.append(" ");
               }

               messageBuilder.append(args[i]);
            }

            String message = messageBuilder.toString();
            if (macroRepository.hasMacro(addName)) {
               this.logDirect(String.format("Макрос с именем %s уже существует!", addName), Formatting.RED);
               return;
            }

            macroRepository.addMacroAndSave(addName, message, addKey);
            this.logDirect(
               String.format("§aДобавлен макрос §f%s §aна клавишу §f%s §aс командой §f%s", addName, KeyHelper.getKeyName(addKey).toLowerCase(), message),
               Formatting.GREEN
            );
            break;
         case "remove":
         case "del":
         case "delete":
            if (args.length < 2) {
               this.logDirect("Использование: macro remove <name>", Formatting.RED);
               return;
            }

            String remName = args[1];
            if (!macroRepository.hasMacro(remName)) {
               this.logDirect(String.format("Макрос %s не найден!", remName), Formatting.RED);
               return;
            }

            macroRepository.deleteMacroAndSave(remName);
            this.logDirect(String.format("Макрос %s удален!", remName), Formatting.GREEN);
            break;
         case "clear":
            int count = macroRepository.size();
            macroRepository.clearListAndSave();
            this.logDirect(String.format("Все макросы удалены! Удалено: %d", count), Formatting.GREEN);
            break;
         case "list":
            int page = 1;
            if (args.length > 1) {
               try {
                  page = Integer.parseInt(args[1]);
               } catch (NumberFormatException var13) {
               }
            }

            List<Macro> macros = macroRepository.getMacroList();
            if (macros.isEmpty()) {
               this.logDirect("Список макросов пуст!", Formatting.RED);
               return;
            }

            Paginator<Macro> paginator = new Paginator<>(macros);
            paginator.setPage(page);
            paginator.display(
               () -> {
                  this.logDirectRaw(Text.literal(HelpCommand.getLine()));
                  this.logDirect("§7Список макросов: §f" + macros.size());
                  this.logDirectRaw(Text.literal(HelpCommand.getLine()));
               },
               macro -> {
                  String macroName = macro.name();
                  String listKeyName = KeyHelper.getKeyName(macro.key()).toLowerCase();
                  String messagex = macro.message();
                  MutableText component = Text.literal("  §e● §f" + macroName)
                     .append(Text.literal(" §8[§7" + listKeyName + "§8]"))
                     .append(Text.literal(" §8-> §7" + messagex));
                  MutableText hoverText = Text.literal("§7Нажмите чтобы удалить макрос §f" + macroName);
                  String removeCommand = manager.getPrefix() + "macro remove " + macroName;
                  component.setStyle(component.getStyle().withHoverEvent(new ShowText(hoverText)).withClickEvent(new RunCommand(removeCommand)));
                  return component;
               },
               manager.getPrefix() + label + " list"
            );
            break;
         default:
            this.logDirectRaw(Text.literal(HelpCommand.getLine()));
            this.logDirect("§7Использование .macro:");
            this.logDirectRaw(Text.literal(HelpCommand.getLine()));
            this.logDirect("§7> macro add <key> <name> <message> §8- §fДобавить макрос");
            this.logDirect("§7> macro remove <name> §8- §fУдалить макрос");
            this.logDirect("§7> macro list §8- §fПоказать список макросов");
            this.logDirect("§7> macro clear §8- §fУдалить все макросы");
            this.logDirectRaw(Text.literal(HelpCommand.getLine()));
      }
   }

   @Override
   public Stream<String> tabComplete(String label, String[] args) {
      if (args.length == 1) {
         return new TabCompleteHelper().append("add", "remove", "list", "clear").sortAlphabetically().filterPrefix(args[0]).stream();
      }

      if (args.length == 2) {
         String action = args[0].toLowerCase();
         if (action.equals("add")) {
            return new TabCompleteHelper().append(KeyHelper.getAllKeyNames()).filterPrefix(args[1]).stream();
         }

         if (action.equals("remove") || action.equals("del") || action.equals("delete")) {
            return new TabCompleteHelper().append(MacroRepository.getInstance().getMacroNames().toArray(new String[0])).filterPrefix(args[1]).stream();
         }
      }

      return Stream.empty();
   }

   @Override
   public String getShortDesc() {
      return "Управление макросами";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "Команда для управления макросами",
         "Использование:",
         "> macro add <key> <name> <message> - Добавить макрос",
         "> macro remove <name> - Удалить макрос",
         "> macro list - Показать список макросов",
         "> macro clear - Удалить все макросы"
      );
   }
}
