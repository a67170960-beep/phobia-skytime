package ruhack.phobia.mixin;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget.ServerEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerServerListWidget.class)
public class MultiplayerServerListWidgetMixin {
   @Mutable
   @Final
   @Shadow
   static ThreadPoolExecutor SERVER_PINGER_THREAD_POOL;
   @Unique
   private static final int PINGER_THREAD_COUNT_OVERHEAD = 5;
   @Final
   @Shadow
   private List<ServerEntry> servers;
   @Unique
   private static boolean threadpoolInitialized = false;

   @Inject(method = "updateEntries", at = @At("HEAD"))
   private void updateEntriesInject(CallbackInfo ci) {
      if (!threadpoolInitialized) {
         threadpoolInitialized = true;
         this.clearServerPingerThreadPool();
      }

      if (SERVER_PINGER_THREAD_POOL.getActiveCount() >= 5) {
         this.clearServerPingerThreadPool();
      }
   }

   @Unique
   private void clearServerPingerThreadPool() {
      SERVER_PINGER_THREAD_POOL.shutdownNow();
      SERVER_PINGER_THREAD_POOL = new ScheduledThreadPoolExecutor(
         this.servers.size() + 5, new ThreadFactoryBuilder().setNameFormat("Server Pinger #%d").setDaemon(true).build()
      );
   }
}
