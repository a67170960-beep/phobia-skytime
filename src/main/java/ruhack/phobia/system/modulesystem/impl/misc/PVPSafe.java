package ruhack.phobia.system.modulesystem.impl.misc;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.ChatEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.impl.display.Notifications;

public final class PVPSafe extends ModuleStructure {
   private static final Pattern LEGACY_FORMATTING = Pattern.compile("(?i)§[0-9A-FK-ORX]");
   private static final Pattern SEPARATORS = Pattern.compile("[^\\p{L}\\p{N}]+");
   private static PVPSafe instance;
   private long lastWarningAt;

   public PVPSafe() {
      super("PVPSafe", "Запрещает выход и команду /hub, пока активен PvP-режим", ModuleCategory.MISC);
      instance = this;
   }

   public static boolean shouldBlockExit() {
      return instance != null && instance.isState() && instance.hasPvpBossBar();
   }

   @EventHandler
   public void onChat(ChatEvent event) {
      if (this.hasPvpBossBar() && isBlockedCommand(event.getMessage())) {
         event.setCancelled(true);
         long now = System.currentTimeMillis();
         if (now - this.lastWarningAt >= 750L) {
            this.lastWarningAt = now;
            Notifications.info("Выход запрещён так как активен PvP-режим");
         }
      }
   }

   private boolean hasPvpBossBar() {
      if (mc.inGameHud == null) {
         return false;
      }

      BossBarHud hud = mc.inGameHud.getBossBarHud();
      if (hud != null && hud.bossBars != null && !hud.bossBars.isEmpty()) {
         for (ClientBossBar bossBar : hud.bossBars.values()) {
            String text = normalizeBossBarText(bossBar.getName().getString());
            String compact = text.replace(" ", "");
            if (compact.contains("pvpрежим")
               || compact.contains("пвпрежим")
               || compact.contains("режимpvp")
               || compact.contains("режимпвп")
               || compact.contains("доокончанияpvp")
               || compact.contains("доокончанияпвп")) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static String normalizeBossBarText(String text) {
      String normalized = Normalizer.normalize(text, Form.NFKC);
      normalized = LEGACY_FORMATTING.matcher(normalized).replaceAll("");
      return SEPARATORS.matcher(normalized.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
   }

   private static boolean isBlockedCommand(String message) {
      if (message == null) {
         return false;
      }

      String command = message.trim();
      if (command.startsWith("/")) {
         command = command.substring(1).trim();
      }

      int separator = command.indexOf(32);
      String root = separator < 0 ? command : command.substring(0, separator);
      return root.equalsIgnoreCase("hub")
         || root.equalsIgnoreCase("lobby")
         || root.equalsIgnoreCase("leave")
         || root.toLowerCase(Locale.ROOT).matches("an\\d{3}");
   }
}
