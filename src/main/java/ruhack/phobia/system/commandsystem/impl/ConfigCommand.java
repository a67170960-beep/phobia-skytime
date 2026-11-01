package ruhack.phobia.system.commandsystem.impl;

import java.io.IOException;
import java.nio.file.Path;
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
import ruhack.phobia.system.configsystem.ConfigSystem;
import ruhack.phobia.system.configsystem.impl.ConfigPath;
import ruhack.phobia.system.configsystem.impl.userconfig.UserConfigManager;
import ruhack.phobia.system.modulesystem.impl.display.Notifications;

public class ConfigCommand extends Command {
   public ConfigCommand() {
      super("config", "Управление конфигурациями", "cfg");
   }

   @Override
   public void execute(String label, String[] args) {
      CommandManager manager = CommandManager.getInstance();
      String arg = args.length > 0 ? args[0].toLowerCase(Locale.US) : "list";
      String name;
      switch (arg) {
         case "load":
            if (args.length < 2) {
               this.logDirect("Использование: config load <name>", Formatting.RED);
               return;
            }

            name = args[1];
            if (ConfigSystem.getInstance().loadNamed(name)) {
               UserConfigManager.getInstance().setActiveConfigName(name);
               Notifications.config(name);
               this.logDirect(String.format("Конфигурация %s загружена!", name));
            } else {
               this.logDirect(String.format("Конфигурация %s не найдена!", name), Formatting.RED);
            }
            break;
         case "save":
            if (args.length < 2) {
               ConfigSystem.getInstance().save();
               this.logDirect("Конфигурация сохранена!");
               return;
            }

            name = args[1];
            if (ConfigSystem.getInstance().saveNamed(name)) {
               UserConfigManager.getInstance().setActiveConfigName(name);
               this.logDirect(String.format("Конфигурация %s сохранена!", name));
            } else {
               this.logDirect("Ошибка при сохранении конфига!", Formatting.RED);
            }
            break;
         case "delete":
         case "remove":
         case "del":
            if (args.length < 2) {
               this.logDirect("Использование: config delete <name>", Formatting.RED);
               return;
            }

            name = args[1];
            if (ConfigSystem.getInstance().deleteNamed(name)) {
               UserConfigManager.getInstance().loadConfigList();
               this.logDirect(String.format("Конфигурация %s удалена!", name), Formatting.GREEN);
            } else {
               this.logDirect(String.format("Конфигурация %s не найдена!", name), Formatting.RED);
            }
            break;
         case "list":
            int page = 1;
            if (args.length > 1) {
               try {
                  page = Integer.parseInt(args[1]);
               } catch (NumberFormatException var11) {
               }
            }

            List<String> configs = this.getConfigs();
            if (configs.isEmpty()) {
               this.logDirect("Конфигурации не найдены!", Formatting.RED);
               return;
            }

            Paginator<String> paginator = new Paginator<>(configs);
            paginator.setPage(page);
            paginator.display(
               () -> {
                  this.logDirectRaw(Text.literal(HelpCommand.getLine()));
                  this.logDirect("§7Список конфигов:");
                  this.logDirectRaw(Text.literal(HelpCommand.getLine()));
               },
               config -> {
                  String loadCommand = manager.getPrefix() + "config load " + config;
                  String deleteCommand = manager.getPrefix() + "config delete " + config;
                  MutableText loadButton = Text.literal("[")
                     .formatted(Formatting.GRAY)
                     .append(Text.literal("Загрузить").formatted(Formatting.GREEN))
                     .append(Text.literal("]").formatted(Formatting.GRAY));
                  loadButton.setStyle(
                     loadButton.getStyle()
                        .withHoverEvent(new ShowText(Text.literal("Загрузить конфиг " + config).formatted(Formatting.GREEN)))
                        .withClickEvent(new RunCommand(loadCommand))
                  );
                  MutableText deleteButton = Text.literal("[")
                     .formatted(Formatting.GRAY)
                     .append(Text.literal("Удалить конфиг").formatted(Formatting.RED))
                     .append(Text.literal("]").formatted(Formatting.GRAY));
                  deleteButton.setStyle(
                     deleteButton.getStyle()
                        .withHoverEvent(new ShowText(Text.literal("Удалить конфиг " + config).formatted(Formatting.RED)))
                        .withClickEvent(new RunCommand(deleteCommand))
                  );
                  return Text.literal("  §b● §f" + config + " ").append(loadButton).append(Text.literal(" ")).append(deleteButton);
               },
               manager.getPrefix() + label + " list"
            );
            break;
         case "dir":
            try {
               Path configDir = ConfigPath.getConfigDirectory();
               String os = System.getProperty("os.name").toLowerCase();
               ProcessBuilder pb;
               if (os.contains("win")) {
                  pb = new ProcessBuilder("explorer", configDir.toAbsolutePath().toString());
               } else if (os.contains("mac")) {
                  pb = new ProcessBuilder("open", configDir.toAbsolutePath().toString());
               } else {
                  pb = new ProcessBuilder("xdg-open", configDir.toAbsolutePath().toString());
               }

               pb.start();
               this.logDirect("Папка с конфигурациями открыта!");
            } catch (IOException e) {
               this.logDirect("Папка с конфигурациями не найдена! " + e.getMessage(), Formatting.RED);
            }
            break;
         default:
            this.logDirectRaw(Text.literal(HelpCommand.getLine()));
            this.logDirect("§7Использование .config:");
            this.logDirectRaw(Text.literal(HelpCommand.getLine()));
            this.logDirect("§7> config load <name> §8- §fЗагружает конфиг.");
            this.logDirect("§7> config save <name> §8- §fСохраняет конфиг.");
            this.logDirect("§7> config delete <name> §8- §fУдаляет конфиг.");
            this.logDirect("§7> config list §8- §fВозвращает список конфигов");
            this.logDirect("§7> config dir §8- §fОткрывает папку с конфигами.");
            this.logDirectRaw(Text.literal(HelpCommand.getLine()));
      }
   }

   @Override
   public Stream<String> tabComplete(String label, String[] args) {
      if (args.length == 1) {
         return new TabCompleteHelper().append("load", "save", "delete", "list", "dir").sortAlphabetically().filterPrefix(args[0]).stream();
      }

      if (args.length == 2) {
         String action = args[0].toLowerCase();
         if (action.equals("load") || action.equals("save") || action.equals("delete") || action.equals("remove") || action.equals("del")) {
            return new TabCompleteHelper().append(this.getConfigs().toArray(new String[0])).filterPrefix(args[1]).stream();
         }
      }

      return Stream.empty();
   }

   @Override
   public String getShortDesc() {
      return "Позволяет взаимодействовать с конфигами в чите";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "С помощью этой команды можно загружать/сохранять конфиги",
         "Использование:",
         "> config load <name> - Загружает конфиг.",
         "> config save <name> - Сохраняет конфиг.",
         "> config delete <name> - Удаляет конфиг.",
         "> config list - Возвращает список конфигов",
         "> config dir - Открывает папку с конфигами."
      );
   }

   public List<String> getConfigs() {
      return ConfigSystem.getInstance().listNamedConfigs();
   }
}
