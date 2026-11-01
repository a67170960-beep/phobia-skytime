package ruhack.phobia.system.commandsystem.impl;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ruhack.phobia.system.commandsystem.Command;
import ruhack.phobia.system.commandsystem.helpers.TabCompleteHelper;
import ruhack.phobia.system.screensystem.clickgui.IrcClient;

public final class PartyCommand extends Command {
   private final IrcClient client = IrcClient.INSTANCE;

   public PartyCommand() {
      super("party", "Управление Party через IRC");
   }

   @Override
   public void execute(String label, String[] args) {
      this.client.startFromProfile();
      if (args.length == 0) {
         this.usage();
      } else {
         boolean sent = switch (args[0].toLowerCase()) {
            case "create" -> this.client.createParty();
            case "invite" -> args.length == 2 && this.client.inviteToParty(args[1]);
            case "accept" -> args.length == 2 && this.client.acceptParty(args[1]);
            case "decline" -> args.length == 2 && this.client.declineParty(args[1]);
            case "disband" -> this.client.disbandParty();
            case "info" -> this.client.requestPartyInfo(true);
            default -> false;
         };
         if (!sent) {
            if ((args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("decline")) && args.length != 2) {
               this.logDirect(Text.literal("Использование: .party " + args[0] + " <login>").formatted(Formatting.GRAY));
            } else {
               this.logDirect(Text.literal("IRC ещё не подключён или команда указана неверно").formatted(Formatting.RED));
            }
         }
      }
   }

   @Override
   public Stream<String> tabComplete(String label, String[] args) {
      return args.length == 1
         ? new TabCompleteHelper().filterPrefix(args[0]).append("create", "invite", "accept", "decline", "disband", "info").stream()
         : Stream.empty();
   }

   @Override
   public List<String> getLongDesc() {
      return List.of(
         "Управляет общей Party через IRC-сервер.",
         "> party create - создать Party",
         "> party invite <login> - добавить пользователя",
         "> party disband - распустить Party",
         "> party info - показать состав Party"
      );
   }

   private void usage() {
      this.logDirect(Text.literal("Использование: .party create | invite <login> | disband | info").formatted(Formatting.GRAY));
   }
}
