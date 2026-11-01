package ruhack.phobia.system.commandsystem.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ruhack.phobia.system.commandsystem.Command;
import ruhack.phobia.system.commandsystem.CommandManager;
import ruhack.phobia.system.commandsystem.helpers.TabCompleteHelper;
import ruhack.phobia.system.modulesystem.impl.other.Theme;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.render.GradientUtil;

public class HelpCommand extends Command {
   private static final Map<String, String[]> ARGUMENTS = createArguments();

   public HelpCommand() {
      super("help", "Показывает список всех доступных команд");
   }

   @Override
   public void execute(String label, String[] args) {
      CommandManager manager = CommandManager.getInstance();
      if (args.length > 0) {
         this.displayDetailed(manager, args[0]);
      } else {
         for (Command command : manager.getCommands()) {
            if (!command.hiddenFromHelp()) {
               this.displayCommand(manager, command);

               for (String alias : command.getAliases()) {
                  this.displayAlias(manager, alias, command.getName());
               }
            }
         }
      }
   }

   private void displayCommand(CommandManager manager, Command command) {
      MutableText line = this.gradient(manager.getPrefix() + command.getName());
      String[] arguments = ARGUMENTS.getOrDefault(command.getName().toLowerCase(), new String[0]);
      if (arguments.length > 0) {
         line.append(Text.literal(" (" + String.join(" ¦ ", arguments) + ")").formatted(Formatting.GRAY));
      }

      this.logDirectRaw(line);
   }

   private void displayAlias(CommandManager manager, String alias, String command) {
      MutableText line = this.gradient(manager.getPrefix() + alias);
      line.append(Text.literal(" -> " + command).formatted(Formatting.GRAY));
      this.logDirectRaw(line);
   }

   private void displayDetailed(CommandManager manager, String name) {
      Command command = manager.getCommand(name.toLowerCase());
      if (command == null) {
         this.logDirect("Команда '" + name + "' не найдена!", Formatting.RED);
      } else {
         this.logDirectRaw(this.gradient(manager.getPrefix() + command.getName()));

         for (String line : command.getLongDesc()) {
            if (!line.isBlank()) {
               this.logDirectRaw(Text.literal(line).formatted(Formatting.GRAY));
            }
         }
      }
   }

   private MutableText gradient(String text) {
      Theme theme = Theme.getInstance();
      int start = theme == null ? ColorUtil.getClientColor() : theme.getAccentColor();
      int end = ColorUtil.multDark(start, 0.5F);
      return GradientUtil.gradient(text, start, end);
   }

   @Override
   public Stream<String> tabComplete(String label, String[] args) {
      return args.length == 1 ? new TabCompleteHelper().filterPrefix(args[0]).addCommands(CommandManager.getInstance()).stream() : Stream.empty();
   }

   @Override
   public String getShortDesc() {
      return "Просмотр всех доступных команд";
   }

   @Override
   public List<String> getLongDesc() {
      return List.of("Справка по командам клиента.", "> help - показать все команды", "> help <command> - подробная справка");
   }

   private static Map<String, String[]> createArguments() {
      Map<String, String[]> result = new LinkedHashMap<>();
      result.put("config", new String[]{"clear", "dir", "list", "load", "remove", "save", "reset"});
      result.put("friend", new String[]{"add", "clear", "list", "remove"});
      result.put("macro", new String[]{"add", "clear", "list", "remove"});
      result.put("bind", new String[]{"add", "clear", "list", "remove"});
      result.put("staff", new String[]{"add", "clear", "list", "remove"});
      result.put("prefix", new String[]{"<prefix>"});
      result.put("help", new String[]{"<command>"});
      result.put("gps", new String[]{"off", "info", "<x> <z>"});
      result.put("neuro", new String[]{"record", "stop", "train", "play", "list", "remove", "clear", "status"});
      return result;
   }

   public static String getLine() {
      return "";
   }
}
