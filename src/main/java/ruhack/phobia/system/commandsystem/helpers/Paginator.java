package ruhack.phobia.system.commandsystem.helpers;

import java.util.List;
import java.util.function.Function;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.RunCommand;
import net.minecraft.text.HoverEvent.ShowText;
import ruhack.phobia.system.commandsystem.CommandManager;
import ruhack.phobia.system.commandsystem.impl.HelpCommand;

public class Paginator<T> {
   private final List<T> items;
   private final int itemsPerPage;
   private int currentPage;

   public Paginator(List<T> items) {
      this(items, 8);
   }

   public Paginator(List<T> items, int itemsPerPage) {
      this.items = items;
      this.itemsPerPage = itemsPerPage;
      this.currentPage = 1;
   }

   public int getTotalPages() {
      return Math.max(1, (int)Math.ceil((double)this.items.size() / this.itemsPerPage));
   }

   public List<T> getCurrentPageItems() {
      int start = (this.currentPage - 1) * this.itemsPerPage;
      int end = Math.min(start + this.itemsPerPage, this.items.size());
      return this.items.subList(start, end);
   }

   public void setPage(int page) {
      this.currentPage = Math.max(1, Math.min(page, this.getTotalPages()));
   }

   public void display(Runnable header, Function<T, MutableText> itemFormatter, String commandPrefix) {
      CommandManager manager = CommandManager.getInstance();
      if (header != null) {
         header.run();
      }

      for (T item : this.getCurrentPageItems()) {
         MutableText formatted = itemFormatter.apply(item);
         manager.sendRaw(formatted);
      }

      if (this.getTotalPages() > 1) {
         this.displayNavigation(manager, commandPrefix);
      } else {
         manager.sendRaw(Text.literal(HelpCommand.getLine()));
      }
   }

   private void displayNavigation(CommandManager manager, String commandPrefix) {
      manager.sendRaw(Text.literal(HelpCommand.getLine()));
      MutableText navigation = Text.literal("");
      if (this.currentPage > 1) {
         MutableText prevButton = Text.literal("§8[§b◄ Назад§8]");
         String prevCommand = commandPrefix + " " + (this.currentPage - 1);
         prevButton.setStyle(
            prevButton.getStyle()
               .withHoverEvent(new ShowText(Text.literal("§7Страница " + (this.currentPage - 1))))
               .withClickEvent(new RunCommand(prevCommand))
         );
         navigation.append(prevButton);
      } else {
         navigation.append(Text.literal("§8[§7◄ Назад§8]"));
      }

      navigation.append(Text.literal(" §7Страница §b" + this.currentPage + "§7/§b" + this.getTotalPages() + " "));
      if (this.currentPage < this.getTotalPages()) {
         MutableText nextButton = Text.literal("§8[§bВперёд ►§8]");
         String nextCommand = commandPrefix + " " + (this.currentPage + 1);
         nextButton.setStyle(
            nextButton.getStyle()
               .withHoverEvent(new ShowText(Text.literal("§7Страница " + (this.currentPage + 1))))
               .withClickEvent(new RunCommand(nextCommand))
         );
         navigation.append(nextButton);
      } else {
         navigation.append(Text.literal("§8[§7Вперёд ►§8]"));
      }

      manager.sendRaw(navigation);
   }

   public static <T> void paginate(String[] args, Paginator<T> paginator, Runnable header, Function<T, MutableText> itemFormatter, String commandPrefix) {
      if (args.length > 0) {
         try {
            int page = Integer.parseInt(args[0]);
            paginator.setPage(page);
         } catch (NumberFormatException var6) {
         }
      }

      paginator.display(header, itemFormatter, commandPrefix);
   }
}
