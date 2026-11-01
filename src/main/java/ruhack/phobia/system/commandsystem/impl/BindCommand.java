package ruhack.phobia.system.commandsystem.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.RunCommand;
import net.minecraft.text.HoverEvent.ShowText;
import net.minecraft.util.Formatting;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.commandsystem.Command;
import ruhack.phobia.system.commandsystem.CommandManager;
import ruhack.phobia.system.commandsystem.helpers.Paginator;
import ruhack.phobia.system.commandsystem.helpers.TabCompleteHelper;
import ruhack.phobia.system.configsystem.ConfigSystem;
import ruhack.phobia.system.configsystem.impl.bind.BindConfig;
import ruhack.phobia.system.modulesystem.ModuleRepository;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.util.string.KeyHelper;

public class BindCommand extends Command {
   public BindCommand() {
      super("bind", "Управление биндами модулей", "b");
   }

   @Override
   public void execute(String label, String[] args) {
      CommandManager manager = CommandManager.getInstance();
      ModuleRepository repository = this.getModuleRepository();
      if (repository == null) {
         this.logDirect("Репозиторий модулей не найден!", Formatting.RED);
      } else {
         String action = args.length > 0 ? args[0].toLowerCase(Locale.US) : "list";
         String moduleName;
         String keyName;
         ModuleStructure module;
         int key;
         switch (action) {
            case "add":
               if (args.length < 3) {
                  this.logDirect("Использование: bind add <module> <key>", Formatting.RED);
                  return;
               }

               moduleName = args[1];
               keyName = args[2];
               module = this.findModule(repository, moduleName);
               if (module == null) {
                  this.logDirect(String.format("Модуль %s не найден!", moduleName), Formatting.RED);
                  return;
               }

               key = KeyHelper.getKeyCode(keyName);
               if (key == -1) {
                  this.logDirect(String.format("Неизвестная клавиша: %s", keyName), Formatting.RED);
                  return;
               }

               module.setKey(key);
               ConfigSystem.getInstance().save();
               this.logDirect(
                  String.format("§aМодуль §f%s §aпривязан к клавише §f%s", module.getName(), KeyHelper.getKeyName(key).toLowerCase()), Formatting.GREEN
               );
               break;
            case "remove":
            case "del":
            case "delete":
               if (args.length < 2) {
                  this.logDirect("Использование: bind remove <module>", Formatting.RED);
                  return;
               }

               moduleName = args[1];
               module = this.findModule(repository, moduleName);
               if (module == null) {
                  this.logDirect(String.format("Модуль %s не найден!", moduleName), Formatting.RED);
                  return;
               }

               module.setKey(-1);
               ConfigSystem.getInstance().save();
               this.logDirect(String.format("Бинд для модуля %s удален!", module.getName()), Formatting.GREEN);
               break;
            case "clear":
               int count = 0;

               for (ModuleStructure modItem : repository.modules()) {
                  if (modItem.getKey() != -1) {
                     modItem.setKey(-1);
                     count++;
                  }
               }

               ConfigSystem.getInstance().save();
               this.logDirect(String.format("Все бинды модулей удалены! Удалено: %d", count), Formatting.GREEN);
               break;
            case "set":
               if (args.length < 3) {
                  this.logDirect("Использование: bind set <target> <key>", Formatting.RED);
                  this.logDirect("Доступные цели: Bind", Formatting.RED);
                  return;
               }

               String target = args[1].toLowerCase(Locale.US);
               keyName = args[2];
               key = KeyHelper.getKeyCode(keyName);
               if (key == -1) {
                  this.logDirect(String.format("Неизвестная клавиша: %s", keyName), Formatting.RED);
                  return;
               }

               if (target.equals("Bind")) {
                  BindConfig.getInstance().setKeyAndSave(key);
                  this.logDirect(String.format("§aКлавиша для Bind изменена на: §f%s", KeyHelper.getKeyName(key).toLowerCase()), Formatting.GREEN);
               } else {
                  this.logDirect(String.format("Неизвестная цель: %s", target), Formatting.RED);
               }
               break;
            case "list":
               int page = 1;
               if (args.length > 1) {
                  try {
                     page = Integer.parseInt(args[1]);
                  } catch (NumberFormatException var12) {
                  }
               }

               List<ModuleStructure> boundModules = repository.modules()
                  .stream()
                  .filter(m -> m.getKey() != -1 && m.getKey() != -1)
                  .collect(Collectors.toList());
               if (boundModules.isEmpty()) {
                  this.logDirect("Нет модулей с биндами!", Formatting.RED);
                  return;
               }

               Paginator<ModuleStructure> paginator = new Paginator<>(boundModules);
               paginator.setPage(page);
               paginator.display(
                  () -> {
                     this.logDirectRaw(Text.literal(HelpCommand.getLine()));
                     this.logDirect("§7Список биндов: §f" + boundModules.size());
                     this.logDirectRaw(Text.literal(HelpCommand.getLine()));
                  },
                  modItem -> {
                     String name = modItem.getName();
                     String kn = KeyHelper.getKeyName(modItem.getKey()).toUpperCase(Locale.ROOT);
                     String removeCommand = manager.getPrefix() + "bind remove " + name;
                     MutableText remove = Text.literal("[")
                        .formatted(Formatting.GRAY)
                        .append(Text.literal("Удалить бинд").formatted(Formatting.RED))
                        .append(Text.literal("]").formatted(Formatting.GRAY));
                     remove.setStyle(
                        remove.getStyle()
                           .withHoverEvent(new ShowText(Text.literal("Удалить бинд " + name).formatted(Formatting.RED)))
                           .withClickEvent(new RunCommand(removeCommand))
                     );
                     return Text.literal(name)
                        .formatted(Formatting.WHITE)
                        .append(Text.literal(" - ").formatted(Formatting.GRAY))
                        .append(Text.literal(kn).formatted(Formatting.WHITE))
                        .append(Text.literal(" "))
                        .append(remove);
                  },
                  manager.getPrefix() + label + " list"
               );
               break;
            default:
               this.logDirectRaw(Text.literal(HelpCommand.getLine()));
               this.logDirect("§7Использование .bind:");
               this.logDirectRaw(Text.literal(HelpCommand.getLine()));
               this.logDirect("§7> bind add <module> <key> §8- §fПривязать модуль к клавише");
               this.logDirect("§7> bind remove <module> §8- §fУдалить бинд модуля");
               this.logDirect("§7> bind list §8- §fПоказать список биндов");
               this.logDirect("§7> bind clear §8- §fУдалить все бинды");
               this.logDirect("§7> bind set Bind <key> §8- §fИзменить клавишу Bind");
               this.logDirectRaw(Text.literal(HelpCommand.getLine()));
         }
      }
   }

   @Override
   public Stream<String> tabComplete(String label, String[] args) {
      ModuleRepository repository = this.getModuleRepository();
      if (args.length == 1) {
         return new TabCompleteHelper().append("add", "remove", "list", "clear", "set").sortAlphabetically().filterPrefix(args[0]).stream();
      }

      if (args.length == 2) {
         String action = args[0].toLowerCase();
         if (action.equals("add")) {
            return new TabCompleteHelper().append(this.getModuleNames(repository)).filterPrefix(args[1]).stream();
         }

         if (action.equals("remove") || action.equals("del") || action.equals("delete")) {
            return new TabCompleteHelper().append(this.getBoundModuleNames(repository)).filterPrefix(args[1]).stream();
         }

         if (action.equals("set")) {
            return new TabCompleteHelper().append("Bind").filterPrefix(args[1]).stream();
         }
      }

      if (args.length == 3) {
         String action = args[0].toLowerCase();
         if (action.equals("add") || action.equals("set")) {
            return new TabCompleteHelper().append(KeyHelper.getAllKeyNames()).filterPrefix(args[2]).stream();
         }
      }

      return Stream.empty();
   }

   @Override
   public String getShortDesc() {
      return "Управление биндами модулей";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "Команда для управления биндами модулей и GUI",
         "Использование:",
         "> bind add <module> <key> - Привязать модуль к клавише",
         "> bind remove <module> - Удалить бинд модуля",
         "> bind list - Показать список биндов",
         "> bind clear - Удалить все бинды",
         "> bind set Bind <key> - Изменить клавишу Bind"
      );
   }

   private ModuleRepository getModuleRepository() {
      Initialization instance = Initialization.getInstance();
      return instance != null && instance.getManager() != null ? instance.getManager().getModuleRepository() : null;
   }

   private ModuleStructure findModule(ModuleRepository repository, String name) {
      return repository.modules().stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
   }

   private String[] getModuleNames(ModuleRepository repository) {
      return repository == null ? new String[0] : repository.modules().stream().map(ModuleStructure::getName).toArray(String[]::new);
   }

   private String[] getBoundModuleNames(ModuleRepository repository) {
      return repository == null
         ? new String[0]
         : repository.modules().stream().filter(m -> m.getKey() != -1 && m.getKey() != -1).map(ModuleStructure::getName).toArray(String[]::new);
   }
}

