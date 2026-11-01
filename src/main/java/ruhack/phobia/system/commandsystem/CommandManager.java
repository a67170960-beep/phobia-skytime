package ruhack.phobia.system.commandsystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ruhack.phobia.system.commandsystem.impl.BindCommand;
import ruhack.phobia.system.commandsystem.impl.ConfigCommand;
import ruhack.phobia.system.commandsystem.impl.FriendCommand;
import ruhack.phobia.system.commandsystem.impl.GpsCommand;
import ruhack.phobia.system.commandsystem.impl.HelpCommand;
import ruhack.phobia.system.commandsystem.impl.MacroCommand;
import ruhack.phobia.system.commandsystem.impl.NeuroCommand;
import ruhack.phobia.system.commandsystem.impl.PartyCommand;
import ruhack.phobia.system.commandsystem.impl.PrefixCommand;
import ruhack.phobia.system.commandsystem.impl.StaffCommand;
import ruhack.phobia.system.configsystem.impl.prefix.PrefixConfig;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.ChatEvent;
import ruhack.phobia.system.eventsystem.impl.TabCompleteEvent;
import ruhack.phobia.util.string.chat.ChatMessage;

public class CommandManager {
   private static CommandManager instance;
   private final List<Command> commands;
   private String prefix;

   public CommandManager() {
      instance = this;
      this.commands = new CopyOnWriteArrayList<>();
      this.prefix = PrefixConfig.getInstance().getPrefix();
   }

   public static CommandManager getInstance() {
      return instance;
   }

   public void init() {
      this.registerCommand(new HelpCommand());
      this.registerCommand(new GpsCommand());
      this.registerCommand(new PartyCommand());
      this.registerCommand(new ConfigCommand());
      this.registerCommand(new FriendCommand());
      this.registerCommand(new MacroCommand());
      this.registerCommand(new BindCommand());
      this.registerCommand(new PrefixCommand());
      this.registerCommand(new StaffCommand());
      this.registerCommand(new NeuroCommand());
      EventManager.register(this);
   }

   public void registerCommand(Command command) {
      this.commands.add(command);
   }

   public void unregisterCommand(Command command) {
      this.commands.remove(command);
   }

   public Command getCommand(String name) {
      return this.commands.stream().filter(cmd -> cmd.matches(name)).findFirst().orElse(null);
   }

   public List<Command> getCommands() {
      return new ArrayList<>(this.commands);
   }

   public String getPrefix() {
      return this.prefix;
   }

   public void setPrefix(String prefix) {
      this.prefix = prefix;
   }

   @EventHandler
   public void onChat(ChatEvent event) {
      if (!event.isCancelled()) {
         String msg = event.getMessage();
         if (msg.startsWith(this.prefix)) {
            event.cancel();
            String commandStr = msg.substring(this.prefix.length());
            if (commandStr.trim().isEmpty()) {
               this.execute("help");
               return;
            }

            if (!this.execute(commandStr)) {
               this.sendError("Неизвестная команда. Используйте " + this.prefix + "help для списка команд.");
            }
         }
      }
   }

   @EventHandler
   public void onTabComplete(TabCompleteEvent event) {
      String eventPrefix = event.prefix;
      if (eventPrefix.startsWith(this.prefix)) {
         String msg = eventPrefix.substring(this.prefix.length());
         Stream<String> stream = this.tabComplete(msg);
         String[] parts = msg.split(" ", -1);
         if (parts.length <= 1) {
            stream = stream.map(x -> this.prefix + x);
         }

         event.completions = stream.toArray(String[]::new);
      }
   }

   public boolean execute(String input) {
      if (input != null && !input.trim().isEmpty()) {
         String[] parts = input.trim().split("\\s+", 2);
         String commandName = parts[0];
         String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];
         Command command = this.getCommand(commandName);
         if (command != null) {
            try {
               command.execute(commandName, args);
               return true;
            } catch (Exception e) {
               this.sendError("Ошибка при выполнении команды: " + e.getMessage());
               e.printStackTrace();
            }
         }

         return false;
      } else {
         return this.execute("help");
      }
   }

   public Stream<String> tabComplete(String input) {
      if (input == null) {
         input = "";
      }

      String[] args = input.split("\\s+", -1);
      if (args.length <= 1) {
         String partial = args.length == 0 ? "" : args[0].toLowerCase();
         return this.getCommandSuggestions(partial);
      } else {
         String commandName = args[0];
         Command command = this.getCommand(commandName);
         if (command != null) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return command.tabComplete(commandName, subArgs);
         } else {
            return Stream.empty();
         }
      }
   }

   private Stream<String> getCommandSuggestions(String partial) {
      if (partial.isEmpty()) {
         return this.commands.stream().map(Command::getName).sorted();
      }

      Set<String> suggestions = new LinkedHashSet<>();

      for (Command cmd : this.commands) {
         String mainName = cmd.getName();
         if (mainName.toLowerCase().startsWith(partial)) {
            suggestions.add(mainName);
         } else {
            for (String alias : cmd.getAliases()) {
               if (alias.toLowerCase().startsWith(partial)) {
                  suggestions.add(alias);
                  break;
               }
            }
         }
      }

      return suggestions.stream().sorted();
   }

   public void sendMessage(String message) {
      ChatMessage.brandmessage(message);
   }

   public void sendSuccess(String message) {
      ChatMessage.brandmessage(Text.literal(message).formatted(Formatting.GREEN));
   }

   public void sendError(String message) {
      ChatMessage.brandmessage(Text.literal(message).formatted(Formatting.RED));
   }

   public void sendRaw(Text text) {
      if (!text.getString().isBlank()) {
         ChatMessage.brandmessage(text);
      }
   }
}
