package ruhack.phobia.system.commandsystem.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.RunCommand;
import net.minecraft.text.HoverEvent.ShowText;
import net.minecraft.util.Formatting;
import ruhack.phobia.system.commandsystem.Command;
import ruhack.phobia.system.commandsystem.CommandManager;
import ruhack.phobia.system.commandsystem.helpers.Paginator;
import ruhack.phobia.system.commandsystem.helpers.TabCompleteHelper;
import ruhack.phobia.system.friendsystem.Friend;
import ruhack.phobia.system.friendsystem.FriendUtils;

public class FriendCommand extends Command {
   private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

   public FriendCommand() {
      super("friend", "Управление списком друзей", "f", "friends");
   }

   @Override
   public void execute(String label, String[] args) {
      CommandManager manager = CommandManager.getInstance();
      String arg = args.length > 0 ? args[0].toLowerCase(Locale.US) : "list";
      switch (arg) {
         case "add":
            if (args.length < 2) {
               this.logDirect("Использование: friend add <name>", Formatting.RED);
               return;
            }

            String addFriendName = args[1];
            if (FriendUtils.isFriend(addFriendName)) {
               this.logDirect(String.format("Игрок %s уже в списке друзей!", addFriendName), Formatting.RED);
               return;
            }

            FriendUtils.addFriendAndSave(addFriendName);
            this.logDirect(String.format("Игрок %s добавлен в друзья!", addFriendName), Formatting.GREEN);
            break;
         case "remove":
         case "del":
         case "delete":
            if (args.length < 2) {
               this.logDirect("Использование: friend remove <name>", Formatting.RED);
               return;
            }

            String remFriendName = args[1];
            if (!FriendUtils.isFriend(remFriendName)) {
               this.logDirect(String.format("Игрок %s не найден в списке друзей!", remFriendName), Formatting.RED);
               return;
            }

            FriendUtils.removeFriendAndSave(remFriendName);
            this.logDirect(String.format("Игрок %s удален из друзей!", remFriendName), Formatting.GREEN);
            break;
         case "clear":
            int count = FriendUtils.size();
            FriendUtils.clearAndSave();
            this.logDirect(String.format("Список друзей очищен! Удалено: %d", count), Formatting.GREEN);
            break;
         case "list":
            int page = 1;
            if (args.length > 1) {
               try {
                  page = Integer.parseInt(args[1]);
               } catch (NumberFormatException var10) {
               }
            }

            List<Friend> friends = FriendUtils.getFriends();
            if (friends.isEmpty()) {
               this.logDirect("Список друзей пуст!", Formatting.RED);
               return;
            }

            Paginator<Friend> paginator = new Paginator<>(friends);
            paginator.setPage(page);
            paginator.display(
               () -> {
                  this.logDirectRaw(Text.literal(HelpCommand.getLine()));
                  this.logDirect("§7Список друзей: §f" + friends.size());
                  this.logDirectRaw(Text.literal(HelpCommand.getLine()));
               },
               friend -> {
                  String friendName = friend.getName();
                  String date = DATE_FORMAT.format(Instant.ofEpochMilli(friend.getAddedAt()));
                  String removeCommand = manager.getPrefix() + "friend remove " + friendName;
                  MutableText remove = Text.literal("[")
                     .formatted(Formatting.GRAY)
                     .append(Text.literal("Удалить из друзей").formatted(Formatting.RED))
                     .append(Text.literal("]").formatted(Formatting.GRAY));
                  remove.setStyle(
                     remove.getStyle()
                        .withHoverEvent(new ShowText(Text.literal("Удалить " + friendName + " из друзей").formatted(Formatting.RED)))
                        .withClickEvent(new RunCommand(removeCommand))
                  );
                  return Text.literal(friendName)
                     .formatted(Formatting.WHITE)
                     .append(Text.literal(" (").formatted(Formatting.GRAY))
                     .append(Text.literal(date).formatted(Formatting.WHITE))
                     .append(Text.literal(") ").formatted(Formatting.GRAY))
                     .append(remove);
               },
               manager.getPrefix() + label + " list"
            );
            break;
         default:
            this.logDirectRaw(Text.literal(HelpCommand.getLine()));
            this.logDirect("§7Использование .friend:");
            this.logDirectRaw(Text.literal(HelpCommand.getLine()));
            this.logDirect("§7> friend add <name> §8- §fДобавить игрока в друзья");
            this.logDirect("§7> friend remove <name> §8- §fУдалить игрока из друзей");
            this.logDirect("§7> friend list §8- §fПоказать список друзей");
            this.logDirect("§7> friend clear §8- §fОчистить список друзей");
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
            return new TabCompleteHelper().append(this.getOnlinePlayers().toArray(new String[0])).filterPrefix(args[1]).stream();
         }

         if (action.equals("remove") || action.equals("del") || action.equals("delete")) {
            return new TabCompleteHelper().append(FriendUtils.getFriendNames().toArray(new String[0])).filterPrefix(args[1]).stream();
         }
      }

      return Stream.empty();
   }

   @Override
   public String getShortDesc() {
      return "Управление списком друзей";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "Команда для управления списком друзей",
         "Использование:",
         "> friend add <name> - Добавить игрока в друзья",
         "> friend remove <name> - Удалить игрока из друзей",
         "> friend list - Показать список друзей",
         "> friend clear - Очистить список друзей"
      );
   }

   private List<String> getOnlinePlayers() {
      List<String> players = new ArrayList<>();
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.getNetworkHandler() != null) {
         for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            String name = entry.getProfile().name();
            if (!FriendUtils.isFriend(name)) {
               players.add(name);
            }
         }
      }

      return players;
   }
}
