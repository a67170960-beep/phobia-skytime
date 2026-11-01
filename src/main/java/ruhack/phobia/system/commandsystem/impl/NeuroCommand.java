package ruhack.phobia.system.commandsystem.impl;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ruhack.phobia.system.commandsystem.Command;
import ruhack.phobia.system.commandsystem.CommandManager;
import ruhack.phobia.system.commandsystem.helpers.TabCompleteHelper;
import ruhack.phobia.util.aim.neuro.NeuroController;
import ruhack.phobia.util.aim.neuro.NeuroStore;

public class NeuroCommand extends Command {
   public NeuroCommand() {
      super("neuro", "Нейро-ротация: запись аима, обучение и воспроизведение", "ai");
   }

   @Override
   public void execute(String label, String[] args) {
      NeuroController ctl = NeuroController.get();
      ctl.ensureRegistered();
      String action = args.length > 0 ? args[0].toLowerCase(Locale.US) : "help";
      String name = args.length > 1 ? args[1] : null;
      switch (action) {
         case "record":
            if (name == null) {
               this.needName("record");
               return;
            }

            ctl.record(name);
            break;
         case "train":
            if (name == null) {
               this.needName("train");
               return;
            }

            ctl.train(name);
            break;
         case "play":
            if (name == null) {
               this.needName("play");
               return;
            }

            ctl.play(name);
            break;
         case "remove":
         case "delete":
            if (name == null) {
               this.needName("remove");
               return;
            }

            ctl.remove(name);
            break;
         case "stop":
            ctl.stop();
            break;
         case "list":
            ctl.list();
            break;
         case "clear":
            ctl.clear();
            break;
         case "status":
         case "info":
            this.printStatus(ctl);
            break;
         default:
            this.printHelp();
      }
   }

   private void needName(String sub) {
      String p = CommandManager.getInstance().getPrefix();
      this.logDirect("§cУкажи имя: §f" + p + "neuro " + sub + " <имя>", Formatting.RED);
   }

   private void printStatus(NeuroController ctl) {
      this.logDirectRaw(Text.literal(HelpCommand.getLine()));
      this.logDirect("§7Neuro - статус:");
      this.logDirectRaw(Text.literal(HelpCommand.getLine()));
      this.logDirect("§7Запись: " + (ctl.isRecording() ? "§aидёт («§f" + ctl.getRecordName() + "§a», " + ctl.getDatasetSize() + " сэмплов)" : "§8выкл"));
      this.logDirect("§7Обучение: " + (ctl.isTraining() ? "§aидёт..." : "§8не идёт"));
      this.logDirect("§7Воспроизведение: " + (ctl.isPlaying() ? "§aмодель «§f" + ctl.getPlayName() + "§a»" : "§8выкл (триггербот)"));
      this.logDirect("§7Записей всего: §f" + NeuroStore.list().size());
      this.logDirectRaw(Text.literal(HelpCommand.getLine()));
   }

   private void printHelp() {
      String p = CommandManager.getInstance().getPrefix();
      this.logDirectRaw(Text.literal(HelpCommand.getLine()));
      this.logDirect("§7Neuro - нейро-ротация:");
      this.logDirectRaw(Text.literal(HelpCommand.getLine()));
      this.logDirect("§7Как обучить (аура для записи не нужна):");
      this.logDirect("§f 1. " + p + "neuro record <имя> §8— §7спавнит фейка, дерись с ним сам (дозапись)");
      this.logDirect("§f 2. " + p + "neuro stop §8— §7закончить запись");
      this.logDirect("§f 3. " + p + "neuro train <имя> §8— §7обучить модель в игре");
      this.logDirect("§f 4. " + p + "neuro play <имя> §8— §7ротация целится этой моделью");
      this.logDirectRaw(Text.literal(HelpCommand.getLine()));
      this.logDirect("§8Ещё: " + p + "neuro list | remove <имя> | clear | status | stop");
      this.logDirectRaw(Text.literal(HelpCommand.getLine()));
   }

   @Override
   public Stream<String> tabComplete(String label, String[] args) {
      if (args.length == 1) {
         return new TabCompleteHelper()
            .append("record", "play", "train", "stop", "list", "remove", "clear", "status")
            .sortAlphabetically()
            .filterPrefix(args[0])
            .stream();
      }

      if (args.length == 2) {
         String a = args[0].toLowerCase(Locale.US);
         if (a.equals("play") || a.equals("train") || a.equals("remove") || a.equals("delete") || a.equals("record")) {
            TabCompleteHelper helper = new TabCompleteHelper();
            NeuroStore.list().forEach(xva$0 -> helper.append(xva$0));
            return helper.filterPrefix(args[1]).stream();
         }
      }

      return Stream.empty();
   }

   @Override
   public List<String> getLongDesc() {
      return List.of(
         "Нейро-ротация: запись аима по фейку, обучение и воспроизведение.",
         "> neuro record <имя> - спавн фейка + запись твоего аима (дозапись)",
         "> neuro stop - закончить запись",
         "> neuro train <имя> - обучить модель",
         "> neuro play <имя> - ротация от обученной модели",
         "> neuro list / remove <имя> / clear / status"
      );
   }
}
