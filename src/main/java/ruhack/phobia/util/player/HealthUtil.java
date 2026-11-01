package ruhack.phobia.util.player;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.util.ServerUtil;

public final class HealthUtil implements IMinecraft {
   private static final Pattern HP_TOKEN = Pattern.compile("(?:\\[|\\s|^)(\\d{1,3})(?:[.,](\\d))?\\s*(?:hp|хп|❤|♥|❤️)?", 66);
   private static final Pattern HP_SUFFIX = Pattern.compile("(\\d{1,3})(?:[.,](\\d))\\s*(?:hp|хп)\\b", 66);
   private static final Int2ObjectOpenHashMap<HealthUtil.Snapshot> SNAPSHOTS = new Int2ObjectOpenHashMap();

   private HealthUtil() {
   }

   public static boolean shouldFix() {
      return mc.player != null && mc.world != null ? ServerUtil.isFunTime() || ServerUtil.isCopyTime() : false;
   }

   public static float fixHealth(PlayerEntity player, float vanilla) {
      if (!shouldFix()) {
         return vanilla;
      }

      HealthUtil.Snapshot snapshot = resolve(player, vanilla);
      SNAPSHOTS.put(player.getId(), snapshot);
      return snapshot.health;
   }

   public static float fixAbsorption(PlayerEntity player, float vanilla) {
      if (!shouldFix()) {
         return vanilla;
      } else {
         HealthUtil.Snapshot snapshot = (HealthUtil.Snapshot)SNAPSHOTS.get(player.getId());
         if (snapshot != null && snapshot.scoreboard) {
            return 0.0F;
         } else {
            return looksFake(player, vanilla) ? 0.0F : vanilla;
         }
      }
   }

   public static float total(LivingEntity entity) {
      if (entity instanceof PlayerEntity player && shouldFix()) {
         HealthUtil.Snapshot cached = (HealthUtil.Snapshot)SNAPSHOTS.get(player.getId());
         if (cached != null) {
            return cached.health;
         }

         float vanilla = entity.getHealth() + entity.getAbsorptionAmount();
         HealthUtil.Snapshot snapshot = resolve(player, entity.getHealth());
         SNAPSHOTS.put(player.getId(), snapshot);
         return snapshot.scoreboard ? snapshot.health : vanilla;
      } else {
         return entity.getHealth() + entity.getAbsorptionAmount();
      }
   }

   private static HealthUtil.Snapshot resolve(PlayerEntity player, float vanilla) {
      Float board = fromScoreboard(player);
      if (board != null) {
         return new HealthUtil.Snapshot(board, true);
      } else {
         Float parsed = fromName(player);
         if (parsed != null) {
            return new HealthUtil.Snapshot(parsed, false);
         } else if (looksFake(player, vanilla)) {
            float max = Math.max(1.0F, player.getMaxHealth());
            return new HealthUtil.Snapshot(max, false);
         } else {
            return new HealthUtil.Snapshot(Math.max(0.0F, vanilla), false);
         }
      }
   }

   private static Float fromScoreboard(PlayerEntity player) {
      Scoreboard scoreboard = mc.world.getScoreboard();
      ScoreboardDisplaySlot[] slots = new ScoreboardDisplaySlot[]{ScoreboardDisplaySlot.BELOW_NAME, ScoreboardDisplaySlot.LIST};
      float max = Math.max(1.0F, player.getMaxHealth());
      float best = Float.NaN;

      for (ScoreboardDisplaySlot slot : slots) {
         ScoreboardObjective objective = scoreboard.getObjectiveForSlot(slot);
         if (objective != null) {
            ReadableScoreboardScore score = scoreboard.getScore(player, objective);
            if (score != null) {
               int points = score.getScore();
               if (points >= 0 && points <= 200) {
                  if (points == 0 && !player.isAlive()) {
                     return 0.0F;
                  }

                  if (!(points > max * 3.0F)) {
                     float value = points;
                     String criteria = objective.getCriterion().getName();
                     if ("health".equals(criteria) && value > max + 0.5F) {
                        value = Math.min(value, max * 2.0F);
                     }

                     if (Float.isNaN(best) || value > 0.0F || best <= 0.0F) {
                        best = value;
                     }
                  }
               }
            }
         }
      }

      return Float.isNaN(best) ? null : best;
   }

   private static Float fromName(LivingEntity entity) {
      Float value = parse(entity.getDisplayName().getString());
      if (value != null) {
         return value;
      } else if (entity instanceof PlayerEntity player && mc.getNetworkHandler() != null) {
         PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
         return entry != null && entry.getDisplayName() != null ? parse(entry.getDisplayName().getString()) : null;
      } else {
         return null;
      }
   }

   private static Float parse(String text) {
      if (text != null && !text.isBlank()) {
         String plain = text.replace('§', ' ').trim();
         Matcher suffix = HP_SUFFIX.matcher(plain);
         if (suffix.find()) {
            return parseNumber(suffix.group(1), suffix.group(2));
         }

         Matcher token = HP_TOKEN.matcher(plain);
         Float last = null;

         while (token.find()) {
            Float candidate = parseNumber(token.group(1), token.group(2));
            if (candidate != null && candidate <= 200.0F) {
               last = candidate;
            }
         }

         return last;
      } else {
         return null;
      }
   }

   private static Float parseNumber(String whole, String fraction) {
      if (whole != null && !whole.isBlank()) {
         try {
            float value = Float.parseFloat(whole);
            if (fraction != null && !fraction.isBlank()) {
               value += Float.parseFloat(fraction) / (float)Math.pow(10.0, fraction.length());
            }

            return value;
         } catch (NumberFormatException ignored) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static boolean looksFake(PlayerEntity player, float vanilla) {
      if (!player.isAlive()) {
         return false;
      }

      float max = Math.max(1.0F, player.getMaxHealth());
      return vanilla >= 100.0F || vanilla > max * 2.5F;
   }

   private record Snapshot(float health, boolean scoreboard) {
   }
}
