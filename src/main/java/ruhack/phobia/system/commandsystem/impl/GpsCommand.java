package ruhack.phobia.system.commandsystem.impl;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ruhack.phobia.system.commandsystem.Command;
import ruhack.phobia.system.commandsystem.helpers.TabCompleteHelper;

public final class GpsCommand extends Command {
   private static boolean enabled;
   private static double targetX;
   private static double targetZ;

   public GpsCommand() {
      super("gps", "Указывает направление и расстояние до координат");
   }

   @Override
   public void execute(String label, String[] args) {
      if (args.length == 1 && args[0].equalsIgnoreCase("off")) {
         enabled = false;
         targetX = 0.0;
         targetZ = 0.0;
         this.logDirect(Text.literal("GPS отключён!").formatted(Formatting.GRAY));
      } else if (args.length == 1 && args[0].equalsIgnoreCase("info")) {
         if (!enabled) {
            this.logDirect(Text.literal("Сейчас GPS отключён!").formatted(Formatting.GRAY));
         } else {
            this.logDirect(
               Text.literal("Информация о текущем GPS: x: ")
                  .formatted(Formatting.GRAY)
                  .append(Text.literal(format(targetX)).formatted(Formatting.WHITE))
                  .append(Text.literal(" z: ").formatted(Formatting.GRAY))
                  .append(Text.literal(format(targetZ)).formatted(Formatting.WHITE))
            );
         }
      } else {
         if (args.length == 2) {
            try {
               targetX = Double.parseDouble(args[0].replace(',', '.'));
               targetZ = Double.parseDouble(args[1].replace(',', '.'));
               enabled = true;
               this.logDirect(
                  Text.literal("GPS установлен на x: ")
                     .formatted(Formatting.GRAY)
                     .append(Text.literal(format(targetX)).formatted(Formatting.WHITE))
                     .append(Text.literal(" z: ").formatted(Formatting.GRAY))
                     .append(Text.literal(format(targetZ)).formatted(Formatting.WHITE))
               );
               return;
            } catch (NumberFormatException var4) {
            }
         }

         this.logDirect(Text.literal("Использование: .gps <x> <z> | .gps info | .gps off").formatted(Formatting.GRAY));
      }
   }

   @Override
   public Stream<String> tabComplete(String label, String[] args) {
      return args.length == 1 ? new TabCompleteHelper().filterPrefix(args[0]).append("off", "info").stream() : Stream.empty();
   }

   @Override
   public List<String> getLongDesc() {
      return List.of(
         "Указывает направление и расстояние до заданных координат.",
         "> gps <x> <z> - установить точку",
         "> gps info - показать текущую точку",
         "> gps off - отключить GPS"
      );
   }

   public static boolean isEnabled() {
      return enabled;
   }

   public static double getTargetX() {
      return targetX;
   }

   public static double getTargetZ() {
      return targetZ;
   }

   private static String format(double value) {
      return String.format(Locale.ROOT, "%.1f", value);
   }
}
