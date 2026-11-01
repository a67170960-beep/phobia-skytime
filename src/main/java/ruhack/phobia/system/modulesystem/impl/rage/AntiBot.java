package ruhack.phobia.system.modulesystem.impl.rage;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;
import net.minecraft.util.Hand;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.util.istance.Instance;

public class AntiBot extends ModuleStructure {
   private final Set<UUID> suspectSet = new HashSet<>();
   static Set<UUID> botSet = new HashSet<>();
   private final SelectSetting mode = new SelectSetting("Режим", "Выберите режим обнаружения ботов", "ReallyWorld", "Matrix", "ReallyWorld", "Divine");
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

   public static AntiBot getInstance() {
      return Instance.get(AntiBot.class);
   }

   public AntiBot() {
      super("AntiBot", "Не дает аурам бить по боту", ModuleCategory.RAGE);
      this.settings(this.mode);
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (mc.getNetworkHandler() != null) {
         switch (e.getPacket()) {
            case PlayerListS2CPacket list:
               this.checkPlayerAfterSpawn(list);
               break;
            case PlayerRemoveS2CPacket remove:
               this.removePlayerBecauseLeftServer(remove);
               break;
            default:
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null && mc.getNetworkHandler() != null) {
         if (!this.suspectSet.isEmpty()) {
            mc.world.getPlayers().stream().filter(p -> this.suspectSet.contains(p.getUuid())).forEach(this::evaluateSuspectPlayer);
         }

         if (this.mode.isSelected("Matrix")) {
            this.matrixMode();
         } else if (this.mode.isSelected("ReallyWorld")) {
            this.ReallyWorldMode();
         } else if (this.mode.isSelected("Divine")) {
            this.newMatrixMode();
         }
      }
   }

   private void checkPlayerAfterSpawn(PlayerListS2CPacket listS2CPacket) {
      listS2CPacket.getPlayerAdditionEntries().forEach(entry -> {
         GameProfile profile = entry.profile();
         if (profile != null && !this.isRealPlayer(entry, profile)) {
            if (this.isDuplicateProfile(profile)) {
               botSet.add(profile.id());
            } else {
               this.suspectSet.add(profile.id());
            }
         }
      });
   }

   private void removePlayerBecauseLeftServer(PlayerRemoveS2CPacket removeS2CPacket) {
      removeS2CPacket.profileIds().forEach(uuid -> {
         this.suspectSet.remove(uuid);
         botSet.remove(uuid);
      });
   }

   private boolean isRealPlayer(Entry entry, GameProfile profile) {
      return entry.latency() < 2 || profile.properties() != null && !profile.properties().isEmpty();
   }

   private void evaluateSuspectPlayer(PlayerEntity player) {
      List<ItemStack> armor = null;
      if (!this.isFullyEquipped(player)) {
         armor = this.getArmorItems(player);
      }

      if (this.isFullyEquipped(player) || this.hasArmorChanged(player, armor)) {
         botSet.add(player.getUuid());
      }

      this.suspectSet.remove(player.getUuid());
   }

   private List<ItemStack> getArmorItems(PlayerEntity entity) {
      List<ItemStack> armorItems = new ArrayList<>();

      for (EquipmentSlot slot : ARMOR_SLOTS) {
         armorItems.add(entity.getEquippedStack(slot));
      }

      return armorItems;
   }

   private ItemStack getArmorStack(PlayerEntity entity, int index) {
      return index >= 0 && index < ARMOR_SLOTS.length ? entity.getEquippedStack(ARMOR_SLOTS[index]) : ItemStack.EMPTY;
   }

   private boolean isArmorItem(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }

      EquippableComponent equippable = (EquippableComponent)stack.get(DataComponentTypes.EQUIPPABLE);
      if (equippable == null) {
         return false;
      }

      EquipmentSlot slot = equippable.slot();
      return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
   }

   private void matrixMode() {
      for (Iterator<UUID> iterator = this.suspectSet.iterator(); iterator.hasNext(); iterator.remove()) {
         UUID susPlayer = iterator.next();
         PlayerEntity entity = mc.world.getPlayerByUuid(susPlayer);
         if (entity != null) {
            String playerName = entity.getName().getString();
            boolean isNameBot = playerName.startsWith("CIT-") && !playerName.contains("NPC") && !playerName.contains("[ZNPC]");
            int armorCount = 0;

            for (EquipmentSlot slot : ARMOR_SLOTS) {
               ItemStack item = entity.getEquippedStack(slot);
               if (!item.isEmpty()) {
                  armorCount++;
               }
            }

            boolean isFullArmor = armorCount == 4;
            boolean isFakeUUID = !entity.getUuid().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes()));
            if (isFullArmor || isNameBot || isFakeUUID) {
               botSet.add(susPlayer);
            }
         }
      }

      if (mc.player.age % 100 == 0) {
         botSet.removeIf(uuid -> mc.world.getPlayerByUuid(uuid) == null);
      }
   }

   private void ReallyWorldMode() {
      for (PlayerEntity entity : mc.world.getPlayers()) {
         if (!entity.getUuid().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + entity.getName().getString()).getBytes()))
            && !botSet.contains(entity.getUuid())
            && !entity.getName().getString().contains("NPC")
            && !entity.getName().getString().startsWith("[ZNPC]")) {
            botSet.add(entity.getUuid());
         }
      }
   }

   private void newMatrixMode() {
      for (PlayerEntity entity : mc.world.getPlayers()) {
         if (entity != mc.player) {
            List<ItemStack> armorItems = this.getArmorItems(entity);
            boolean allArmorValid = true;

            for (ItemStack item : armorItems) {
               if (item.isEmpty() || !item.isEnchantable() || item.getDamage() > 0) {
                  allArmorValid = false;
                  break;
               }
            }

            boolean hasSpecificArmor = false;

            for (ItemStack item : armorItems) {
               if (item.getItem() == Items.LEATHER_BOOTS
                  || item.getItem() == Items.LEATHER_LEGGINGS
                  || item.getItem() == Items.LEATHER_CHESTPLATE
                  || item.getItem() == Items.LEATHER_HELMET
                  || item.getItem() == Items.IRON_BOOTS
                  || item.getItem() == Items.IRON_LEGGINGS
                  || item.getItem() == Items.IRON_CHESTPLATE
                  || item.getItem() == Items.IRON_HELMET) {
                  hasSpecificArmor = true;
                  break;
               }
            }

            if (allArmorValid
               && hasSpecificArmor
               && entity.getStackInHand(Hand.OFF_HAND).getItem() == Items.AIR
               && entity.getStackInHand(Hand.MAIN_HAND).getItem() != Items.AIR
               && entity.getHungerManager().getFoodLevel() == 20
               && !entity.getName().getString().contains("NPC")
               && !entity.getName().getString().startsWith("[ZNPC]")) {
               botSet.add(entity.getUuid());
            } else {
               botSet.remove(entity.getUuid());
            }
         }
      }
   }

   public boolean isDuplicateProfile(GameProfile profile) {
      return Objects.requireNonNull(mc.getNetworkHandler())
            .getPlayerList()
            .stream()
            .filter(player -> player.getProfile().name().equals(profile.name()) && !player.getProfile().id().equals(profile.id()))
            .count()
         == 1L;
   }

   public boolean isFullyEquipped(PlayerEntity entity) {
      for (EquipmentSlot slot : ARMOR_SLOTS) {
         ItemStack stack = entity.getEquippedStack(slot);
         if (!this.isArmorItem(stack) || stack.hasEnchantments()) {
            return false;
         }
      }

      return true;
   }

   public boolean hasArmorChanged(PlayerEntity entity, List<ItemStack> prevArmor) {
      if (prevArmor == null) {
         return true;
      }

      List<ItemStack> currentArmorList = this.getArmorItems(entity);
      if (currentArmorList.size() != prevArmor.size()) {
         return true;
      }

      for (int i = 0; i < currentArmorList.size(); i++) {
         if (!ItemStack.areEqual(currentArmorList.get(i), prevArmor.get(i))) {
            return true;
         }
      }

      return false;
   }

   public boolean isBot(PlayerEntity entity) {
      String playerName = entity.getName().getString();
      boolean isNameBot = playerName.startsWith("CIT-") && !playerName.contains("NPC") && !playerName.startsWith("[ZNPC]");
      boolean isMarkedBot = botSet.contains(entity.getUuid());
      this.isBotU(entity);
      return isNameBot || isMarkedBot;
   }

   public boolean isBot(UUID uuid) {
      return botSet.contains(uuid);
   }

   public boolean isBotU(Entity entity) {
      return !entity.getUuid().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + entity.getName().getString()).getBytes()))
         && entity.isInvisible()
         && !entity.getName().getString().contains("NPC")
         && !entity.getName().getString().startsWith("[ZNPC]");
   }

   public void reset() {
      this.suspectSet.clear();
      botSet.clear();
   }

   @Override
   public void deactivate() {
      this.reset();
      super.deactivate();
   }
}
