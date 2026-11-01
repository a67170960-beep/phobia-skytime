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
import ruhack.phobia.system.commandsystem.staffsystem.Staff;
import ruhack.phobia.system.commandsystem.staffsystem.StaffUtils;

public class StaffCommand extends Command {
   private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

   public StaffCommand() {
      super("staff", "Управление списком персонала");
   }

   @Override
   public void execute(String label, String[] args) {
      CommandManager manager = CommandManager.getInstance();
      String action = args.length > 0 ? args[0].toLowerCase(Locale.US) : "list";
         switch (action) {
         case "add":
            if (args.length < 2) {
               this.logDirect("Использование: staff add <name>", Formatting.RED);
               return;
            }

            String addStaffName = args[1];
            if (StaffUtils.isStaff(addStaffName)) {
               this.logDirect(String.format("Игрок %s уже в списке персонала!", addStaffName), Formatting.RED);
               return;
            }

            StaffUtils.addStaffAndSave(addStaffName);
            this.logDirect(String.format("Игрок %s добавлен в персонал!", addStaffName), Formatting.GREEN);
            break;
         case "remove":
         case "del":
         case "delete":
            if (args.length < 2) {
               this.logDirect("Использование: staff remove <name>", Formatting.RED);
               return;
            }

            String remStaffName = args[1];
            if (!StaffUtils.isStaff(remStaffName)) {
               this.logDirect(String.format("Игрок %s не найден в списке персонала!", remStaffName), Formatting.RED);
               return;
            }

            StaffUtils.removeStaffAndSave(remStaffName);
            this.logDirect(String.format("Игрок %s удален из персонала!", remStaffName), Formatting.GREEN);
            break;
         case "clear":
            int count = StaffUtils.size();
            StaffUtils.clearAndSave();
            this.logDirect(String.format("Список персонала очищен! Удалено: %d", count), Formatting.GREEN);
            break;
         case "list":
            int page = 1;
            if (args.length > 1) {
               try {
                  page = Integer.parseInt(args[1]);
               } catch (NumberFormatException var10) {
               }
            }

            List<Staff> staff = StaffUtils.getStaffList();
            if (staff.isEmpty()) {
               this.logDirect("Список персонала пуст!", Formatting.RED);
               return;
            }

            Paginator<Staff> paginator = new Paginator<>(staff);
            paginator.setPage(page);
            paginator.display(
               () -> {
                  this.logDirectRaw(Text.literal(HelpCommand.getLine()));
                  this.logDirect("§7Список стаффа: §f" + staff.size());
                  this.logDirectRaw(Text.literal(HelpCommand.getLine()));
               },
               staffEntry -> {
                  String entryName = staffEntry.name();
                  String date = DATE_FORMAT.format(Instant.ofEpochMilli(staffEntry.addedAt()));
                  String removeCommand = manager.getPrefix() + "staff remove " + entryName;
                  MutableText remove = Text.literal("[")
                     .formatted(Formatting.GRAY)
                     .append(Text.literal("Удалить из стаффа").formatted(Formatting.RED))
                     .append(Text.literal("]").formatted(Formatting.GRAY));
                  remove.setStyle(
                     remove.getStyle()
                        .withHoverEvent(new ShowText(Text.literal("Удалить " + entryName + " из стаффа").formatted(Formatting.RED)))
                        .withClickEvent(new RunCommand(removeCommand))
                  );
                  return Text.literal(entryName)
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
            this.logDirect("§7Использование .staff:");
            this.logDirectRaw(Text.literal(HelpCommand.getLine()));
            this.logDirect("§7> staff add <name> §8- §fДобавить игрока в персонал");
            this.logDirect("§7> staff remove <name> §8- §fУдалить игрока из персонала");
            this.logDirect("§7> staff list §8- §fПоказать список персонала");
            this.logDirect("§7> staff clear §8- §fОчистить список персонала");
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
            return new TabCompleteHelper().append(StaffUtils.getStaffNames().toArray(new String[0])).filterPrefix(args[1]).stream();
         }
      }

      return Stream.empty();
   }

   @Override
   public String getShortDesc() {
      return "Управление списком персонала";
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "Команда для управления списком персонала сервера",
         "Использование:",
         "> staff add <name> - Добавить игрока в персонал",
         "> staff remove <name> - Удалить игрока из персонала",
         "> staff list - Показать список персонала",
         "> staff clear - Очистить список персонала"
      );
   }

   private List<String> getOnlinePlayers() {
      List<String> players = new ArrayList<>();
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.getNetworkHandler() != null) {
         for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            String name = entry.getProfile().name();
            if (!StaffUtils.isStaff(name)) {
               players.add(name);
            }
         }
      }

      return players;
   }
}
