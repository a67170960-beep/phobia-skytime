package ruhack.phobia.util;

import lombok.Generated;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.util.string.PlayerInteractionHelper;
import ruhack.phobia.util.timer.StopWatch;

public final class ServerUtil implements IMinecraft {
   private static final StopWatch pvpWatch = new StopWatch();
   public static String server = "Vanilla";
   public static float TPS = 20.0F;
   public static long timestamp;
   public static int anarchy;
   public static boolean pvpEnd;

   public static void tick() {
      anarchy = getAnarchyMode();
      server = getServer();
      pvpEnd = inPvpEnd();
      if (inPvp()) {
         pvpWatch.reset();
      }
   }

   public static void packet(PacketEvent e) {
      switch (e.getPacket()) {
         case WorldTimeUpdateS2CPacket time:
            long nanoTime = System.nanoTime();
            float maxTPS = 20.0F;
            float rawTPS = maxTPS * (1.0E9F / (float)(nanoTime - timestamp));
            TPS = MathHelper.clamp(rawTPS, 0.0F, maxTPS);
            timestamp = nanoTime;
         default:
      }
   }

   private static String getServer() {
      if (!PlayerInteractionHelper.nullCheck()
         && mc.getNetworkHandler() != null
         && mc.getNetworkHandler().getServerInfo() != null
         && mc.getNetworkHandler().getBrand() != null) {
         String serverIp = mc.getNetworkHandler().getServerInfo().address.toLowerCase();
         String brand = mc.getNetworkHandler().getBrand().toLowerCase();
         if (brand.contains("botfilter")) {
            return "FunTime";
         } else if (brand.contains("§6spooky§ccore")) {
            return "SpookyTime";
         } else if (serverIp.contains("funtime") || serverIp.contains("skytime") || serverIp.contains("space-times") || serverIp.contains("funsky")) {
            return "CopyTime";
         } else if (brand.contains("holyworld") || brand.contains("vk.com/idwok")) {
            return "HolyWorld";
         } else if (serverIp.contains("gulpvp")) {
            return "GulPvP";
         } else if (serverIp.contains("heavenworld")) {
            return "HeavenWorld";
         } else {
            return serverIp.contains("reallyworld") ? "ReallyWorld" : "Vanilla";
         }
      } else {
         return "Vanilla";
      }
   }

   private static int getAnarchyMode() {
      Scoreboard scoreboard = mc.world.getScoreboard();
      ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
      switch (server) {
         case "FunTime":
            if (objective != null) {
               String[] string = objective.getDisplayName().getString().split("-");
               if (string.length > 1) {
                  return Integer.parseInt(string[1]);
               }
            }
            break;
         case "HolyWorld":
            for (ScoreboardEntry scoreboardEntry : scoreboard.getScoreboardEntries(objective)) {
               String text = Team.decorateName(scoreboard.getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name()).getString();
               if (!text.isEmpty()) {
                  String string = StringUtils.substringBetween(text, "#", " -◆-");
                  if (string != null && !string.isEmpty()) {
                     return Integer.parseInt(string.replace(" (1.20)", ""));
                  }
               }
            }
      }

      return -1;
   }

   public static boolean isPvp() {
      return !pvpWatch.finished(500.0);
   }

   private static boolean inPvp() {
      return mc.inGameHud
         .getBossBarHud()
         .bossBars
         .values()
         .stream()
         .map(c -> c.getName().getString().toLowerCase())
         .anyMatch(s -> s.contains("pvp") || s.contains("пвп"));
   }

   private static boolean inPvpEnd() {
      return mc.inGameHud
         .getBossBarHud()
         .bossBars
         .values()
         .stream()
         .map(c -> c.getName().getString().toLowerCase())
         .anyMatch(s -> (s.contains("pvp") || s.contains("пвп")) && (s.contains("0") || s.contains("1")));
   }

   public static String getWorldType() {
      return mc.world.getRegistryKey().getValue().getPath();
   }

   public static boolean isCopyTime() {
      return server.equals("CopyTime") || server.equals("SpookyTime") || server.equals("FunTime");
   }

   public static boolean isFunTime() {
      return server.equals("FunTime");
   }

   public static boolean isReallyWorld() {
      return server.equals("ReallyWorld");
   }

   public static boolean isHolyWorld() {
      return server.equals("HolyWorld");
   }

   public static boolean isVanilla() {
      return server.equals("Vanilla");
   }

   @Generated
   private ServerUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   @Generated
   public static int getAnarchy() {
      return anarchy;
   }

   @Generated
   public static boolean isPvpEnd() {
      return pvpEnd;
   }
}
