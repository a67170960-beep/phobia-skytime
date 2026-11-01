package ruhack.phobia.util.player.swap;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;

public final class SwapManager {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public static final int PRIORITY_CRITICAL = 0;
   public static final int PRIORITY_HIGH = 10;
   public static final int PRIORITY_NORMAL = 20;
   public static final int PRIORITY_LOW = 30;
   private static final PriorityQueue<SwapRequest> swapQueue = new PriorityQueue<>();
   private static final Map<String, SwapRequest> swapRegistry = new ConcurrentHashMap<>();
   private static SwapRequest currentSwap = null;
   private static final long SWAP_TIMEOUT = 3000L;
   private static long currentSwapStartTime = 0L;

   private SwapManager() {
   }

   public static boolean queueSwap(String id, int priority, Runnable swapAction, SwapSettings settings, Runnable onComplete) {
      if (swapRegistry.containsKey(id)) {
         return false;
      }

      SwapRequest request = new SwapRequest(id, priority, swapAction, settings, onComplete);
      swapRegistry.put(id, request);
      swapQueue.add(request);
      return true;
   }

   public static boolean queueSwapInstant(String id, int priority, Runnable swapAction, Runnable onComplete) {
      return queueSwap(id, priority, swapAction, SwapSettings.instant().closeInventory(false), onComplete);
   }

   public static void cancelSwap(String id) {
      SwapRequest request = swapRegistry.remove(id);
      if (request != null) {
         request.cancel();
         swapQueue.remove(request);
         if (currentSwap == request) {
            currentSwap = null;
         }
      }
   }

   public static boolean isSwapQueued(String id) {
      return swapRegistry.containsKey(id);
   }

   public static boolean anySwapRunning() {
      return currentSwap != null && !currentSwap.isFinished();
   }

   public static boolean isSwapRunning(String id) {
      return currentSwap != null && id.equals(currentSwap.getId()) && !currentSwap.isFinished();
   }

   public static String getCurrentSwapId() {
      return currentSwap != null ? currentSwap.getId() : null;
   }

   public static void tick() {
      if (mc.player == null) {
         cancelAll();
      } else {
         if (currentSwap != null) {
            long elapsed = System.currentTimeMillis() - currentSwapStartTime;
            if (elapsed > 3000L) {
               currentSwap.cancel();
               swapRegistry.remove(currentSwap.getId());
               currentSwap = null;
            }
         }

         if (currentSwap != null) {
            currentSwap.tick();
            if (currentSwap.isFinished()) {
               swapRegistry.remove(currentSwap.getId());
               currentSwap = null;
            }
         }

         if (currentSwap == null && !swapQueue.isEmpty()) {
            while (!swapQueue.isEmpty()) {
               SwapRequest next = swapQueue.poll();
               if (next != null && !next.isCancelled()) {
                  currentSwap = next;
                  currentSwapStartTime = System.currentTimeMillis();
                  currentSwap.start();
                  break;
               }

               if (next != null) {
                  swapRegistry.remove(next.getId());
               }
            }
         }
      }
   }

   public static void cancelAll() {
      if (currentSwap != null) {
         currentSwap.cancel();
         currentSwap = null;
      }

      for (SwapRequest request : swapQueue) {
         request.cancel();
      }

      swapQueue.clear();
      swapRegistry.clear();
   }

   public static int getQueueSize() {
      int size = swapQueue.size();
      if (currentSwap != null && !currentSwap.isFinished()) {
         size++;
      }

      return size;
   }

   @Deprecated
   public static boolean tryAcquireLock(String owner) {
      return !isSwapQueued(owner);
   }

   @Deprecated
   public static void releaseLock(String owner) {
   }

   @Deprecated
   public static void forceReleaseLock() {
      cancelAll();
   }

   @Deprecated
   public static boolean isLocked() {
      return anySwapRunning();
   }

   @Deprecated
   public static boolean isLockedBy(String owner) {
      return isSwapRunning(owner);
   }

   @Deprecated
   public static String getLockOwner() {
      return getCurrentSwapId();
   }

   public static void swapAndUse(Item item) {
      swapAndUse(item, 20);
   }

   public static void swapAndUse(Item item, int priority) {
      InventoryResult result = InventoryUtils.find(item);
      if (result.found()) {
         String id = "swapAndUse_" + item.toString() + "_" + System.nanoTime();
         if (result.isHotbar()) {
            queueSwapInstant(id, priority, () -> {
               InventoryUtils.saveSlot();
               InventoryUtils.selectSlot(result.slot());
               InventoryUtils.use(Hand.MAIN_HAND);
               InventoryUtils.restoreSlot();
            }, null);
         } else {
            int hotbar = InventoryUtils.currentSlot();
            queueSwapInstant(id, priority, () -> {
               InventoryUtils.swapHotbar(result.slot(), hotbar);
               InventoryUtils.use(Hand.MAIN_HAND);
               InventoryUtils.swapHotbar(result.slot(), hotbar);
               InventoryUtils.closeScreen();
            }, null);
         }
      }
   }

   public static void swapAndUseSilent(Item item) {
      swapAndUseSilent(item, 20);
   }

   public static void swapAndUseSilent(Item item, int priority) {
      InventoryResult result = InventoryUtils.find(item);
      if (result.found()) {
         String id = "swapAndUseSilent_" + item.toString() + "_" + System.nanoTime();
         if (result.isHotbar()) {
            queueSwapInstant(id, priority, () -> {
               InventoryUtils.saveSlot();
               InventoryUtils.selectSlotSilent(result.slot());
               InventoryUtils.use(Hand.MAIN_HAND);
               InventoryUtils.restoreSlotSilent();
            }, null);
         } else {
            int hotbar = InventoryUtils.currentSlot();
            queueSwapInstant(id, priority, () -> {
               InventoryUtils.swapHotbar(result.slot(), hotbar);
               InventoryUtils.use(Hand.MAIN_HAND);
               InventoryUtils.swapHotbar(result.slot(), hotbar);
               InventoryUtils.closeScreen();
            }, null);
         }
      }
   }

   public static void moveToHotbar(Item item, int hotbarSlot) {
      InventoryResult result = InventoryUtils.find(item);
      if (result.found() && !result.isHotbar()) {
         String id = "moveToHotbar_" + item.toString() + "_" + System.nanoTime();
         queueSwapInstant(id, 30, () -> InventoryUtils.swapHotbar(result.slot(), hotbarSlot), null);
      }
   }

   public static void swapSlots(int from, int to) {
      String id = "swapSlots_" + from + "_" + to + "_" + System.nanoTime();
      queueSwapInstant(id, 30, () -> InventoryUtils.swap(from, to), null);
   }
}
