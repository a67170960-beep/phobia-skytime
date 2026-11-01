package ruhack.phobia.system.modulesystem.impl.rage;

import net.minecraft.block.Blocks;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import ruhack.phobia.mixin.ClientWorldAccessor;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.RotationUpdateEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic.LinearSmoothMode;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.util.math.TaskPriority;
import ruhack.phobia.util.rotation.RotationConfig;
import ruhack.phobia.util.rotation.RotationController;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.timer.StopWatch;

public class AutoPotion extends ModuleStructure {
   private final BooleanSetting autoOff = new BooleanSetting("Авто отключение", "Автоматически выключать модуль после использования").setValue(false);
   private final MultiSelectSetting potions = new MultiSelectSetting("Бросать", "Выберите зелья для автоброса")
      .value("Силу", "Скорость", "Огнестойкость")
      .selected("Силу", "Скорость");
   private final StopWatch timer = new StopWatch();
   private boolean spoofed = false;
   private boolean isActivePotion = false;
   private int rotationTicks = 0;
   private int selectedSlot = -1;
   private final float THROW_PITCH = 90.0F;
   private final int ROTATION_WAIT_TICKS = 2;

   public AutoPotion() {
      super("AutoPotion", "Автоматически кидает зелье под игрока", ModuleCategory.RAGE);
      this.settings(this.potions, this.autoOff);
   }

   @Override
   public void deactivate() {
      this.isActivePotion = false;
      this.spoofed = false;
      this.rotationTicks = 0;
      this.selectedSlot = -1;
      RotationController.INSTANCE.clear();
   }

   private int findPotionSlot(AutoPotion.PotionType type) {
      if (mc.player == null) {
         return -1;
      }

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.isOf(Items.SPLASH_POTION)) {
            PotionContentsComponent potionComponent = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
            if (potionComponent != null) {
               for (StatusEffectInstance effect : potionComponent.getEffects()) {
                  if (effect.getEffectType() == type.effect) {
                     return i;
                  }
               }
            }
         }
      }

      return -1;
   }

   private boolean hasEffect(RegistryEntry<StatusEffect> effect) {
      return mc.player != null && mc.player.hasStatusEffect(effect);
   }

   private boolean canBuff(AutoPotion.PotionType type) {
      return this.hasEffect(type.effect) ? false : type.isEnabled(this) && this.findPotionSlot(type) != -1;
   }

   private boolean canBuff() {
      return mc.player != null && mc.world != null
         ? (this.canBuff(AutoPotion.PotionType.STRENGTH) || this.canBuff(AutoPotion.PotionType.SPEED) || this.canBuff(AutoPotion.PotionType.FIRE_RESISTANCE))
            && mc.player.isOnGround()
            && this.timer.finished(500.0)
         : false;
   }

   private boolean isActive() {
      return this.isActivePotion
         || this.canBuff(AutoPotion.PotionType.STRENGTH)
         || this.canBuff(AutoPotion.PotionType.SPEED)
         || this.canBuff(AutoPotion.PotionType.FIRE_RESISTANCE);
   }

   private boolean shouldThrow() {
      return mc.player != null && mc.world != null
         ? this.isActive() && this.canBuff() && mc.world.getBlockState(mc.player.getBlockPos().down()).getBlock() != Blocks.AIR
         : false;
   }

   @EventHandler
   public void onRotationUpdate(RotationUpdateEvent event) {
      if (mc.player != null && mc.world != null) {
         if (event.getType() == 0 && (this.shouldThrow() || this.spoofed)) {
            this.performRotation();
         }
      }
   }

   private void performRotation() {
      Angle throwAngle = new Angle(mc.player.getYaw(), 90.0F);
      RotationConfig config = new RotationConfig(new LinearSmoothMode(), true, true, false);
      RotationController.INSTANCE.rotateTo(throwAngle, 3, config, TaskPriority.HIGH_IMPORTANCE_1, this);
      if (!this.spoofed) {
         this.spoofed = true;
         this.isActivePotion = true;
         this.rotationTicks = 0;
         this.selectedSlot = mc.player.getInventory().getSelectedSlot();
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (this.isActivePotion && !this.shouldThrow() && !this.spoofed) {
            this.isActivePotion = false;
            if (this.autoOff.isValue()) {
               this.setState(false);
            }
         }

         if (this.spoofed) {
            this.processThrow();
         }
      }
   }

   private void processThrow() {
      this.rotationTicks++;
      Angle currentRotation = RotationController.INSTANCE.getRotation();
      boolean rotationReady = currentRotation != null && currentRotation.getPitch() >= 80.0F;
      boolean waitedEnough = this.rotationTicks >= 2;
      if (rotationReady && waitedEnough) {
         boolean threwAny = false;
         if (this.canBuff(AutoPotion.PotionType.STRENGTH)) {
            this.throwPotion(AutoPotion.PotionType.STRENGTH);
            threwAny = true;
         }

         if (this.canBuff(AutoPotion.PotionType.SPEED)) {
            this.throwPotion(AutoPotion.PotionType.SPEED);
            threwAny = true;
         }

         if (this.canBuff(AutoPotion.PotionType.FIRE_RESISTANCE)) {
            this.throwPotion(AutoPotion.PotionType.FIRE_RESISTANCE);
            threwAny = true;
         }

         if (this.selectedSlot != -1) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.selectedSlot));
         }

         this.timer.reset();
         this.spoofed = false;
         this.rotationTicks = 0;
         this.isActivePotion = false;
         if (this.autoOff.isValue() || !threwAny) {
            this.setState(false);
         }
      }

      if (this.rotationTicks > 10) {
         this.resetThrowState();
      }
   }

   private void resetThrowState() {
      this.spoofed = false;
      this.rotationTicks = 0;
      this.isActivePotion = false;
      if (this.selectedSlot != -1) {
         mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.selectedSlot));
      }
   }

   private void throwPotion(AutoPotion.PotionType type) {
      if (type.isEnabled(this) && !this.hasEffect(type.effect)) {
         if (mc.player != null && mc.player.networkHandler != null) {
            int slot = this.findPotionSlot(type);
            if (slot != -1) {
               mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
               this.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), 90.0F));
            }
         }
      }
   }

   private void sendSequencedPacket(SequencedPacketCreator packetCreator) {
      if (mc.player != null && mc.player.networkHandler != null && mc.world != null) {
         try {
            ClientWorldAccessor worldAccessor = (ClientWorldAccessor)mc.world;
            PendingUpdateManager pendingUpdateManager = worldAccessor.phobia$getPendingUpdateManager().incrementSequence();
            int sequence = pendingUpdateManager.getSequence();
            mc.player.networkHandler.sendPacket(packetCreator.predict(sequence));
            pendingUpdateManager.close();
         } catch (Exception e) {
            mc.player.networkHandler.sendPacket(packetCreator.predict(0));
         }
      }
   }

   private enum PotionType {
      STRENGTH(StatusEffects.STRENGTH, "Силу"),
      SPEED(StatusEffects.SPEED, "Скорость"),
      FIRE_RESISTANCE(StatusEffects.FIRE_RESISTANCE, "Огнестойкость");

      final RegistryEntry<StatusEffect> effect;
      final String settingName;

      PotionType(RegistryEntry<StatusEffect> effect, String settingName) {
         this.effect = effect;
         this.settingName = settingName;
      }

      public boolean isEnabled(AutoPotion module) {
         return module.potions.isSelected(this.settingName);
      }
   }
}
