package ruhack.phobia.system.modulesystem.impl.rage;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SplashPotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.EntitySpawnEvent;
import ruhack.phobia.system.eventsystem.impl.KeyEvent;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BindSetting;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.screensystem.BuffRingScreen;
import ruhack.phobia.util.player.swap.InventoryUtils;
import ruhack.phobia.util.player.swap.MovementController;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.string.PlayerInteractionHelper;
import ruhack.phobia.util.string.chat.ChatMessage;

public final class BuffHelper extends ModuleStructure {
   public static final int RING_SIZE = 3;
   private static final int THROW_IDLE = 0;
   private static final int THROW_WAITING_SWAP = 1;
   private static final int THROW_WAITING_USE = 2;
   private static final int THROW_WAITING_RESTORE = 3;
   private final ItemStack[] selectedPotions = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
   private int throwStage = 0;
   private int throwStageTicks;
   private int pendingInventoryScreenSlot = -1;
   private int pendingHotbarSlot = -1;
   private ItemStack pendingPotion = ItemStack.EMPTY;
   private boolean pendingSwapApplied;
   private final MovementController throwMovement = new MovementController();
   private final Map<Integer, BuffHelper.TrackedSplashPotion> trackedPotions = new LinkedHashMap<>();
   private final BindSetting openRingBind = new BindSetting("Бинд кольца", "Клавиша открытия кольца бафов");
   private final SelectSetting openRingMode = new SelectSetting(
      "Режим открытия", "Определяет поведение клавиши кольца", "По нажатию", "По нажатию", "При удержании"
   );
   private final BooleanSetting debugPotions = new BooleanSetting("Дебаг зелий", "Пишет попадание любого взрывного зелья по игрокам");

   public BuffHelper() {
      super("BuffHelper", "Кольцо для быстрого выбора и броска бафов", ModuleCategory.RAGE);
      this.settings(this.openRingBind, this.openRingMode, this.debugPotions);
   }

   @EventHandler
   public void onKey(KeyEvent event) {
      if (this.openRingMode.isSelected("При удержании")) {
         if (event.isBindDown(this.openRingBind)) {
            this.openRing();
         } else if (event.isBindReleased(this.openRingBind, true) && mc.currentScreen instanceof BuffRingScreen ringScreen) {
            ringScreen.close();
         }
      } else {
         if (event.isBindDown(this.openRingBind)) {
            this.openRing();
         } else if (event.isBindDown(this.openRingBind, true) && mc.currentScreen instanceof BuffRingScreen ringScreen) {
            ringScreen.close();
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      this.processPendingInventoryThrow();
      this.processWorldPotionDebug();
   }

   @EventHandler
   public void onEntitySpawn(EntitySpawnEvent event) {
      if (this.debugPotions.isValue() && event.getEntity() instanceof SplashPotionEntity potion && !potion.getStack().isEmpty()) {
         Entity owner = potion.getOwner();
         String throwerName = owner == null ? "неизвестно" : owner.getName().getString();
         this.trackedPotions
            .put(
               potion.getId(),
               new BuffHelper.TrackedSplashPotion(
                  potion.getId(), potion, potion.getStack().copyWithCount(1), throwerName, potion.getX(), potion.getY(), potion.getZ()
               )
            );
      }
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (this.debugPotions.isValue()
         && !event.isSend()
         && mc.world != null
         && event.getPacket() instanceof EntityStatusEffectS2CPacket packet
         && mc.world.getEntityById(packet.getEntityId()) instanceof PlayerEntity player) {
         BuffHelper.TrackedSplashPotion tracked = this.findPotionForEffect(player, packet.getEffectId(), packet.getAmplifier());
         if (tracked != null) {
            int fullDuration = this.findFullEffectDuration(tracked.potion, packet.getEffectId(), packet.getAmplifier());
            if (fullDuration >= 0) {
               BuffHelper.PlayerPotionHit hit = tracked.hits
                  .computeIfAbsent(player.getUuid(), uuid -> new BuffHelper.PlayerPotionHit(player.getName().getString()));
               hit.effects
                  .put(
                     packet.getEffectId(), new BuffHelper.ReceivedPotionEffect(packet.getEffectId(), packet.getAmplifier(), packet.getDuration(), fullDuration)
                  );
            }
         }
      }
   }

   @Override
   public void deactivate() {
      this.restorePendingInventorySwap();
      this.trackedPotions.clear();
   }

   private void openRing() {
      if (mc.player != null && mc.world != null) {
         mc.setScreen(new BuffRingScreen(this, mc.currentScreen));
      }
   }

   public boolean isSelectablePotion(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.isOf(Items.SPLASH_POTION) && stack.contains(DataComponentTypes.POTION_CONTENTS);
   }

   public void selectPotion(int ringSlot, ItemStack stack) {
      if (this.isRingSlot(ringSlot) && this.isSelectablePotion(stack)) {
         this.selectedPotions[ringSlot] = stack.copyWithCount(1);
         this.validateSelections();
      }
   }

   public void clearPotion(int ringSlot) {
      if (this.isRingSlot(ringSlot)) {
         this.selectedPotions[ringSlot] = ItemStack.EMPTY;
      }
   }

   public ItemStack getSelectedPotion(int ringSlot) {
      return !this.isRingSlot(ringSlot) ? ItemStack.EMPTY : this.selectedPotions[ringSlot];
   }

   public void validateSelections() {
      for (int i = 0; i < this.selectedPotions.length; i++) {
         if (!this.selectedPotions[i].isEmpty() && this.findPotionSlot(this.selectedPotions[i]) == -1) {
            this.selectedPotions[i] = ItemStack.EMPTY;
         }
      }
   }

   public boolean usePotion(int ringSlot) {
      if (this.isRingSlot(ringSlot) && mc.player != null && mc.world != null && this.throwStage == 0) {
         this.validateSelections();
         ItemStack selectedPotion = this.selectedPotions[ringSlot];
         int inventorySlot = this.findPotionSlot(selectedPotion);
         if (inventorySlot == -1) {
            this.clearPotion(ringSlot);
            return false;
         }

         mc.setScreen(null);
         int previousHotbarSlot = mc.player.getInventory().getSelectedSlot();
         if (inventorySlot < 9) {
            this.selectHotbarSlot(inventorySlot);
            this.throwSelectedPotion(selectedPotion);
            if (previousHotbarSlot != inventorySlot) {
               this.selectHotbarSlot(previousHotbarSlot);
            }
         } else {
            int screenSlot = InventoryUtils.wrapSlot(inventorySlot);
            this.pendingInventoryScreenSlot = screenSlot;
            this.pendingHotbarSlot = previousHotbarSlot;
            this.pendingPotion = selectedPotion.copyWithCount(1);
            this.pendingSwapApplied = false;
            this.throwMovement.saveState();
            this.throwMovement.block();
            this.throwStage = 1;
            this.throwStageTicks = 0;
         }

         return true;
      } else {
         return false;
      }
   }

   private void processPendingInventoryThrow() {
      if (this.throwStage != 0) {
         if (mc.player != null && mc.world != null && mc.player.networkHandler != null) {
            this.throwStageTicks++;
            this.throwMovement.block();
            if (this.throwStage == 1) {
               if (this.throwStageTicks >= 1) {
                  InventoryUtils.swapHotbar(this.pendingInventoryScreenSlot, this.pendingHotbarSlot);
                  this.pendingSwapApplied = true;
                  this.throwStage = 2;
                  this.throwStageTicks = 0;
               }
            } else if (this.throwStage == 2) {
               if (this.throwStageTicks >= 1) {
                  ItemStack handStack = mc.player.getInventory().getStack(this.pendingHotbarSlot);
                  if (!this.samePotion(handStack, this.pendingPotion)) {
                     if (this.throwStageTicks > 6) {
                        this.restorePendingInventorySwap();
                     }
                  } else {
                     this.selectHotbarSlot(this.pendingHotbarSlot);
                     this.throwSelectedPotion(this.pendingPotion);
                     this.throwStage = 3;
                     this.throwStageTicks = 0;
                  }
               }
            } else {
               if (this.throwStage == 3 && this.throwStageTicks >= 1) {
                  this.restorePendingInventorySwap();
               }
            }
         } else {
            this.clearPendingThrow();
         }
      }
   }

   private void restorePendingInventorySwap() {
      if (this.throwStage != 0 && mc.player != null && this.pendingInventoryScreenSlot >= 0 && this.pendingHotbarSlot >= 0 && this.pendingSwapApplied) {
         InventoryUtils.swapHotbar(this.pendingInventoryScreenSlot, this.pendingHotbarSlot);
      }

      this.clearPendingThrow();
   }

   private void clearPendingThrow() {
      this.throwStage = 0;
      this.throwStageTicks = 0;
      this.pendingInventoryScreenSlot = -1;
      this.pendingHotbarSlot = -1;
      this.pendingPotion = ItemStack.EMPTY;
      this.pendingSwapApplied = false;
      if (this.throwMovement.isBlocked()) {
         this.throwMovement.restoreFromCurrent();
      }

      this.throwMovement.reset();
   }

   private void selectHotbarSlot(int slot) {
      InventoryUtils.selectSlot(slot);
      if (mc.player != null && mc.player.networkHandler != null) {
         mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      }
   }

   private void throwSelectedPotion(ItemStack potion) {
      PlayerInteractionHelper.interactItem(Hand.MAIN_HAND, new Angle(mc.player.getYaw(), 90.0F));
   }

   private void processWorldPotionDebug() {
      if (this.debugPotions.isValue() && mc.world != null) {
         Iterator<BuffHelper.TrackedSplashPotion> iterator = this.trackedPotions.values().iterator();

         while (iterator.hasNext()) {
            BuffHelper.TrackedSplashPotion tracked = iterator.next();
            Entity entity = mc.world.getEntityById(tracked.entityId);
            if (entity instanceof SplashPotionEntity && !entity.isRemoved()) {
               tracked.missingTicks = 0;
               tracked.x = entity.getX();
               tracked.y = entity.getY();
               tracked.z = entity.getZ();
               this.refreshPotionStack(tracked);
               tracked.age++;
               if (tracked.age > 1200) {
                  iterator.remove();
               }
            } else {
               this.refreshPotionStack(tracked);
               tracked.missingTicks++;
               if (tracked.missingTicks >= 6) {
                  this.printPotionImpact(tracked);
                  iterator.remove();
               }
            }
         }
      } else {
         this.trackedPotions.clear();
      }
   }

   private void printPotionImpact(BuffHelper.TrackedSplashPotion tracked) {
      if (!tracked.hits.isEmpty()) {
         for (BuffHelper.PlayerPotionHit hit : tracked.hits.values()) {
            int percent = this.hitPercent(hit);
            ChatMessage.brandmessage(
               "§dЗелья §8| §7Бросил: §f"
                  + tracked.throwerName
                  + " §8| §7Зелье: §f"
                  + this.getPotionName(tracked.potion)
                  + " §8| §7Игрок: §f"
                  + hit.playerName
                  + " §8| §7Попадание: "
                  + this.percentColor(percent)
                  + percent
                  + "% §8| §7Получил: §f"
                  + this.describeReceivedEffects(hit)
            );
         }
      } else {
         ChatMessage.brandmessage(
            "§dЗелья §8| §7Бросил: §f"
               + tracked.throwerName
               + " §8| §7Зелье: §f"
               + this.getPotionName(tracked.potion)
               + " §8| §7Игрок: §fникто §8| §7Попадание: §c0% §8| §7Получил: §fнет"
         );
      }
   }

   private BuffHelper.TrackedSplashPotion findPotionForEffect(PlayerEntity player, RegistryEntry<StatusEffect> effect, int amplifier) {
      BuffHelper.TrackedSplashPotion best = null;
      double bestDistance = Double.MAX_VALUE;

      for (BuffHelper.TrackedSplashPotion tracked : this.trackedPotions.values()) {
         this.refreshPotionStack(tracked);
         if (this.findFullEffectDuration(tracked.potion, effect, amplifier) >= 0) {
            double dx = player.getX() - tracked.x;
            double dy = player.getY() - tracked.y;
            double dz = player.getZ() - tracked.z;
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            if (!(distanceSquared > 64.0) && !(distanceSquared >= bestDistance)) {
               best = tracked;
               bestDistance = distanceSquared;
            }
         }
      }

      return best;
   }

   private int findFullEffectDuration(ItemStack potion, RegistryEntry<StatusEffect> effectType, int amplifier) {
      PotionContentsComponent contents = (PotionContentsComponent)potion.get(DataComponentTypes.POTION_CONTENTS);
      if (contents == null) {
         return -1;
      }

      for (StatusEffectInstance effect : contents.getEffects()) {
         if (Objects.equals(effect.getEffectType(), effectType) && effect.getAmplifier() == amplifier) {
            return effect.getDuration();
         }
      }

      return -1;
   }

   private void refreshPotionStack(BuffHelper.TrackedSplashPotion tracked) {
      ItemStack syncedStack = tracked.entity.getStack();
      PotionContentsComponent contents = (PotionContentsComponent)syncedStack.get(DataComponentTypes.POTION_CONTENTS);
      if (contents != null && (contents.potion().isPresent() || contents.hasEffects() || !contents.customEffects().isEmpty())) {
         tracked.potion = syncedStack.copyWithCount(1);
      }
   }

   private String getPotionName(ItemStack potion) {
      if (potion.getCustomName() != null) {
         return potion.getCustomName().getString();
      }

      PotionContentsComponent contents = (PotionContentsComponent)potion.get(DataComponentTypes.POTION_CONTENTS);
      if (contents == null) {
         return potion.getName().getString();
      }

      if (contents.potion().isPresent()) {
         return contents.getName("item.minecraft.splash_potion.effect.").getString();
      }

      StringJoiner effectNames = new StringJoiner(" + ");

      for (StatusEffectInstance effect : contents.getEffects()) {
         effectNames.add(((StatusEffect)effect.getEffectType().value()).getName().getString() + " " + (effect.getAmplifier() + 1));
      }

      return effectNames.length() == 0 ? potion.getName().getString() : effectNames.toString();
   }

   private int hitPercent(BuffHelper.PlayerPotionHit hit) {
      if (hit.effects.isEmpty()) {
         return 0;
      }

      double total = 0.0;

      for (BuffHelper.ReceivedPotionEffect effect : hit.effects.values()) {
         if (((StatusEffect)effect.effectType.value()).isInstant()) {
            total++;
         } else if (effect.fullDuration > 0) {
            total += Math.min(1.0, (double)effect.receivedDuration / effect.fullDuration);
         }
      }

      return (int)Math.round(total * 100.0 / hit.effects.size());
   }

   private String describeReceivedEffects(BuffHelper.PlayerPotionHit hit) {
      StringJoiner effects = new StringJoiner("§8, §f");

      for (BuffHelper.ReceivedPotionEffect effect : hit.effects.values()) {
         String name = ((StatusEffect)effect.effectType.value()).getName().getString();
         int level = effect.amplifier + 1;
         String duration = ((StatusEffect)effect.effectType.value()).isInstant() ? "мгновенно" : this.formatDuration(effect.receivedDuration);
         effects.add(name + " " + level + " §7(" + duration + ")");
      }

      return effects.length() == 0 ? "нет" : effects.toString();
   }

   private String percentColor(int percent) {
      if (percent >= 75) {
         return "§a";
      } else {
         return percent >= 35 ? "§e" : "§c";
      }
   }

   private String formatDuration(int durationTicks) {
      int totalSeconds = Math.max(0, durationTicks / 20);
      return totalSeconds / 60 + ":" + String.format("%02d", totalSeconds % 60);
   }

   private int findPotionSlot(ItemStack selectedPotion) {
      if (mc.player != null && selectedPotion != null && !selectedPotion.isEmpty()) {
         for (int slot = 0; slot < 36; slot++) {
            ItemStack candidate = mc.player.getInventory().getStack(slot);
            if (this.samePotion(candidate, selectedPotion)) {
               return slot;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private boolean samePotion(ItemStack first, ItemStack second) {
      if (this.isSelectablePotion(first) && this.isSelectablePotion(second)) {
         PotionContentsComponent firstContents = (PotionContentsComponent)first.get(DataComponentTypes.POTION_CONTENTS);
         PotionContentsComponent secondContents = (PotionContentsComponent)second.get(DataComponentTypes.POTION_CONTENTS);
         return Objects.equals(firstContents, secondContents);
      } else {
         return false;
      }
   }

   private boolean isRingSlot(int ringSlot) {
      return ringSlot >= 0 && ringSlot < this.selectedPotions.length;
   }

   private static final class PlayerPotionHit {
      private final String playerName;
      private final Map<RegistryEntry<StatusEffect>, BuffHelper.ReceivedPotionEffect> effects = new LinkedHashMap<>();

      private PlayerPotionHit(String playerName) {
         this.playerName = playerName;
      }
   }

   private static final class ReceivedPotionEffect {
      private final RegistryEntry<StatusEffect> effectType;
      private final int amplifier;
      private final int receivedDuration;
      private final int fullDuration;

      private ReceivedPotionEffect(RegistryEntry<StatusEffect> effectType, int amplifier, int receivedDuration, int fullDuration) {
         this.effectType = effectType;
         this.amplifier = amplifier;
         this.receivedDuration = receivedDuration;
         this.fullDuration = fullDuration;
      }
   }

   private static final class TrackedSplashPotion {
      private final int entityId;
      private final SplashPotionEntity entity;
      private final String throwerName;
      private final Map<UUID, BuffHelper.PlayerPotionHit> hits = new LinkedHashMap<>();
      private ItemStack potion;
      private double x;
      private double y;
      private double z;
      private int age;
      private int missingTicks;

      private TrackedSplashPotion(int entityId, SplashPotionEntity entity, ItemStack potion, String throwerName, double x, double y, double z) {
         this.entityId = entityId;
         this.entity = entity;
         this.potion = potion;
         this.throwerName = throwerName;
         this.x = x;
         this.y = y;
         this.z = z;
      }
   }
}
