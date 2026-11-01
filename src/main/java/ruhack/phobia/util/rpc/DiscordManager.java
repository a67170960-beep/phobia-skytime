package ruhack.phobia.util.rpc;

import com.adl.nativeprotect.User;
import lombok.Generated;
import ruhack.phobia.util.rpc.utils.DiscordEventHandlers;
import ruhack.phobia.util.rpc.utils.DiscordRPC;
import ruhack.phobia.util.rpc.utils.DiscordRichPresence;

public class DiscordManager {
   private static final long APPLICATION_ID = 1520891175026950294L;
   private static final String RPC_IMAGE_URL = "https://i.imgur.com/NgzcMaQ.gif";
   private static final String SITE_URL = "https://phobiadlc.fun/";
   private static final String TELEGRAM_URL = "https://t.me/phobia_dlc";
   private final DiscordRichPresence richPresence = new DiscordRichPresence();
   private final DiscordRPC discordRPC = DiscordRPC.INSTANCE;
   private DiscordManager.DiscordDaemonThread discordDaemonThread;
   private Thread updateThread;
   private boolean running;

   public DiscordManager start() {
      if (this.running) {
         return this;
      }

      try {
         DiscordEventHandlers handlers = new DiscordEventHandlers.Builder().build();
         this.discordRPC.Discord_Initialize(String.valueOf(1520891175026950294L), handlers, true, "");
         this.running = true;
         this.richPresence.startTimestamp = System.currentTimeMillis() / 1000L;
         this.updatePresence();
         this.updateThread = new Thread(this::runPresenceUpdater, "Discord-RPC-Updater");
         this.updateThread.setDaemon(true);
         this.updateThread.start();
         this.discordDaemonThread = new DiscordManager.DiscordDaemonThread();
         this.discordDaemonThread.start();
      } catch (Throwable ignored) {
         this.running = false;
      }

      return this;
   }

   public void stopRPC() {
      this.running = false;

      try {
         this.discordRPC.Discord_ClearPresence();
         this.discordRPC.Discord_Shutdown();
      } catch (Throwable var2) {
      }

      if (this.updateThread != null) {
         this.updateThread.interrupt();
         this.updateThread = null;
      }

      if (this.discordDaemonThread != null) {
         this.discordDaemonThread.interrupt();
         this.discordDaemonThread = null;
      }
   }

   private void runPresenceUpdater() {
      while (this.running) {
         try {
            this.updatePresence();
            Thread.sleep(2000L);
         } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return;
         } catch (Throwable ignored) {
            return;
         }
      }
   }

   private void updatePresence() {
      String username = readNativeProfile("username", "leaked by angelw1ngsov");
      String uid = readNativeProfile("uid", "0");
      String role = readNativeProfile("role", "Unknown");
      this.richPresence.details = "Login: " + username;
      this.richPresence.state = "Uid: " + uid;
      this.richPresence.largeImageKey = "https://i.imgur.com/NgzcMaQ.gif";
      this.richPresence.largeImageText = role;
      this.richPresence.button_label_1 = "Telegram";
      this.richPresence.button_url_1 = "https://t.me/phobia_dlc";
      this.richPresence.button_label_2 = "Site";
      this.richPresence.button_url_2 = "https://phobiadlc.fun/";
      this.discordRPC.Discord_UpdatePresence(this.richPresence);
   }

   private static String readNativeProfile(String key, String fallback) {
      try {
         String value = User.getInstance().profile(key);
         return value != null && !value.isBlank() ? value : fallback;
      } catch (Throwable ignored) {
         return fallback;
      }
   }

   @Generated
   public DiscordRichPresence getRichPresence() {
      return this.richPresence;
   }

   @Generated
   public DiscordRPC getDiscordRPC() {
      return this.discordRPC;
   }

   @Generated
   public DiscordManager.DiscordDaemonThread getDiscordDaemonThread() {
      return this.discordDaemonThread;
   }

   @Generated
   public Thread getUpdateThread() {
      return this.updateThread;
   }

   @Generated
   public boolean isRunning() {
      return this.running;
   }

   private final class DiscordDaemonThread extends Thread {
      private DiscordDaemonThread() {
         this.setName("Discord-RPC");
         this.setDaemon(true);
      }

      @Override
      public void run() {
         while (DiscordManager.this.running) {
            try {
               DiscordManager.this.discordRPC.Discord_RunCallbacks();
               Thread.sleep(15000L);
            } catch (InterruptedException ignored) {
               Thread.currentThread().interrupt();
               return;
            } catch (Throwable ignored) {
               DiscordManager.this.stopRPC();
               return;
            }
         }
      }
   }
}
