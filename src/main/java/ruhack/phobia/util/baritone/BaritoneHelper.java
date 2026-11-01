package ruhack.phobia.util.baritone;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.util.math.BlockPos;

public final class BaritoneHelper {
   private static boolean checked;
   private static boolean available;
   private static Method getProviderMethod;
   private static Method getPrimaryBaritoneMethod;
   private static Method getCustomGoalProcessMethod;
   private static Method setGoalAndPathMethod;
   private static Method getPathingBehaviorMethod;
   private static Method isPathingMethod;
   private static Method cancelEverythingMethod;
   private static Method getSettingsMethod;
   private static Constructor<?> goalNearConstructor;
   private static final String[] AUTO_SETTINGS = new String[]{
      "allowBreak", "allowPlace", "allowParkour", "allowSprint", "renderPath", "smoothLook", "smoothLookTicks"
   };
   private static final Map<String, Object> settingsSnapshot = new HashMap<>();
   private static final String[] COMMAND_SETTINGS = new String[]{"prefixControl", "chatControl", "chatControlAnywhere"};
   private static final Map<String, Object> commandSnapshot = new HashMap<>();

   private BaritoneHelper() {
   }

   private static void resolve() {
      if (!checked) {
         checked = true;

         try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            getProviderMethod = apiClass.getMethod("getProvider");
            getSettingsMethod = apiClass.getMethod("getSettings");
            Class<?> providerClass = Class.forName("baritone.api.IBaritoneProvider");
            getPrimaryBaritoneMethod = providerClass.getMethod("getPrimaryBaritone");
            Class<?> baritoneClass = Class.forName("baritone.api.IBaritone");
            getCustomGoalProcessMethod = baritoneClass.getMethod("getCustomGoalProcess");
            getPathingBehaviorMethod = baritoneClass.getMethod("getPathingBehavior");
            Class<?> goalClass = Class.forName("baritone.api.pathing.goals.Goal");
            Class<?> goalProcessClass = Class.forName("baritone.api.process.ICustomGoalProcess");
            setGoalAndPathMethod = goalProcessClass.getMethod("setGoalAndPath", goalClass);
            Class<?> pathingClass = Class.forName("baritone.api.behavior.IPathingBehavior");
            isPathingMethod = pathingClass.getMethod("isPathing");
            cancelEverythingMethod = pathingClass.getMethod("cancelEverything");
            goalNearConstructor = Class.forName("baritone.api.pathing.goals.GoalNear").getConstructor(BlockPos.class, int.class);
            available = true;
         } catch (ClassNotFoundException | NoSuchMethodException e) {
            available = false;
         }
      }
   }

   private static Object primaryBaritone() {
      resolve();
      if (!available) {
         return null;
      }

      try {
         Object provider = getProviderMethod.invoke(null);
         return provider == null ? null : getPrimaryBaritoneMethod.invoke(provider);
      } catch (Exception e) {
         return null;
      }
   }

   public static boolean isAvailable() {
      return primaryBaritone() != null;
   }

   public static boolean goTo(BlockPos pos, int range) {
      Object baritone = primaryBaritone();
      if (baritone != null && pos != null) {
         try {
            Object process = getCustomGoalProcessMethod.invoke(baritone);
            Object goal = goalNearConstructor.newInstance(pos, range);
            setGoalAndPathMethod.invoke(process, goal);
            return true;
         } catch (Exception e) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean isPathing() {
      Object baritone = primaryBaritone();
      if (baritone == null) {
         return false;
      }

      try {
         Object pathing = getPathingBehaviorMethod.invoke(baritone);
         return Boolean.TRUE.equals(isPathingMethod.invoke(pathing));
      } catch (Exception e) {
         return false;
      }
   }

   public static void stop() {
      Object baritone = primaryBaritone();
      if (baritone != null) {
         try {
            Object pathing = getPathingBehaviorMethod.invoke(baritone);
            cancelEverythingMethod.invoke(pathing);
         } catch (Exception var2) {
         }
      }
   }

   public static void applySettings(boolean auto, boolean renderPath) {
      resolve();
      if (available) {
         try {
            Object settings = getSettingsMethod.invoke(null);
            if (auto) {
               snapshotIfNeeded(settings);
               setSetting(settings, "allowBreak", false);
               setSetting(settings, "allowPlace", false);
               setSetting(settings, "allowParkour", true);
               setSetting(settings, "allowSprint", true);
               setSetting(settings, "renderPath", renderPath);
            } else {
               restoreSnapshot(settings);
            }
         } catch (Exception var3) {
         }
      }
   }

   public static void applyMiningSettings(boolean enabled, boolean renderPath) {
      resolve();
      if (available) {
         try {
            Object settings = getSettingsMethod.invoke(null);
            if (enabled) {
               snapshotIfNeeded(settings);
               setSetting(settings, "allowBreak", true);
               setSetting(settings, "allowPlace", false);
               setSetting(settings, "allowParkour", true);
               setSetting(settings, "allowSprint", true);
               setSetting(settings, "renderPath", renderPath);
               setSetting(settings, "smoothLook", true);
               setSetting(settings, "smoothLookTicks", 3);
            } else {
               restoreSnapshot(settings);
            }
         } catch (Exception var3) {
         }
      }
   }

   public static void setAllowBreak(boolean allowed) {
      resolve();
      if (available) {
         try {
            Object settings = getSettingsMethod.invoke(null);
            setSetting(settings, "allowBreak", allowed);
         } catch (Exception var2) {
         }
      }
   }

   public static void setCommandsEnabled(boolean enabled) {
      resolve();
      if (available) {
         try {
            Object settings = getSettingsMethod.invoke(null);
            if (!enabled) {
               if (commandSnapshot.isEmpty()) {
                  for (String name : COMMAND_SETTINGS) {
                     Object cur = getSettingSafe(settings, name);
                     if (cur != null) {
                        commandSnapshot.put(name, cur);
                     }
                  }
               }

               for (String name : commandSnapshot.keySet()) {
                  setSettingSafe(settings, name, Boolean.FALSE);
               }
            } else {
               for (Entry<String, Object> entry : commandSnapshot.entrySet()) {
                  setSettingSafe(settings, entry.getKey(), entry.getValue());
               }

               commandSnapshot.clear();
            }
         } catch (Exception var7) {
         }
      }
   }

   private static Object getSettingSafe(Object settings, String name) {
      try {
         return getSetting(settings, name);
      } catch (Exception e) {
         return null;
      }
   }

   private static void setSettingSafe(Object settings, String name, Object value) {
      try {
         setSetting(settings, name, value);
      } catch (Exception var4) {
      }
   }

   private static void snapshotIfNeeded(Object settings) throws Exception {
      if (settingsSnapshot.isEmpty()) {
         for (String name : AUTO_SETTINGS) {
            settingsSnapshot.put(name, getSetting(settings, name));
         }
      }
   }

   private static void restoreSnapshot(Object settings) throws Exception {
      for (Entry<String, Object> entry : settingsSnapshot.entrySet()) {
         if (entry.getValue() != null) {
            setSetting(settings, entry.getKey(), entry.getValue());
         }
      }

      settingsSnapshot.clear();
   }

   private static Object getSetting(Object settings, String name) throws Exception {
      Object holder = settings.getClass().getField(name).get(settings);
      Field value = holder.getClass().getField("value");
      return value.get(holder);
   }

   private static void setSetting(Object settings, String name, Object newValue) throws Exception {
      Object holder = settings.getClass().getField(name).get(settings);
      Field value = holder.getClass().getField("value");
      value.set(holder, newValue);
   }
}
