package ruhack.phobia.system.modulesystem.impl.misc;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.KeyEvent;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BindSetting;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.util.player.swap.InventoryUtils;
import ruhack.phobia.util.player.swap.MovementController;
import ruhack.phobia.util.string.chat.ChatMessage;

public final class ReallyWorldHelper extends ModuleStructure {
   private static final List<Pattern> BANNED_WORDS = List.of(
      pattern("expensive|celestial|newcode|arbuz|akrien|nursultan|relake|wild|wurst"),
      pattern("catlavan|excellent|rockstar|impact|matix|inertia|wexside|wex|rich"),
      pattern("funpay|fluger|riker|funtime|holyworld|hvh|rogalik|thunderhack|liquidbounce"),
      pattern("акриен|рич|нюкод|экспенсив|импакт|экселлент|катлаван|целестиал"),
      pattern("матикс|инерти|эксп|флюгер|рикер|фанпей|вексайд|нурсултан|нурик|нурлан")
   );
   private final BooleanSetting closeMenu = new BooleanSetting("Закрывать меню", "Закрывает стартовое серверное меню ReallyWorld");
   private final BooleanSetting chatFilter = new BooleanSetting("Фильтр чата", "Отменяет отправку сообщений с названиями других клиентов");
   private final BooleanSetting fixAfterPvp = new BooleanSetting("/fix all после PvP", "Отправляет /fix all после завершения PvP-режима");
   private final BindSetting trap = new BindSetting("Кнопка ловушки", "Использовать сердце моря");
   private final BindSetting throwBall = new BindSetting("Кнопка кидай шар", "Отправить фразу для шара");
   private final BindSetting antiFlight = new BindSetting("Кнопка Анти-Полёта", "Использовать звезду фейерверка");
   private final SelectSetting swapMode = new SelectSetting(
      "Режим свапа", "Быстрый - моментально, New - остановка на тик перед свапом и возвратом", "Быстрый", "Быстрый", "New"
   );
   private final MovementController movement = new MovementController();
   private ReallyWorldHelper.ActionPhase phase = ReallyWorldHelper.ActionPhase.IDLE;
   private int previousSlot = -1;
   private int targetSlot = -1;
   private int temporaryHotbarSlot = -1;
   private int stopTicks;
   private boolean fromHotbar;
   private long restoreAt;
   private long fixAt;

   public ReallyWorldHelper() {
      super("ReallyWorldHelper", "Помощник для специальных функций сервера ReallyWorld", ModuleCategory.MISC);
      this.settings(this.closeMenu, this.chatFilter, this.fixAfterPvp, this.trap, this.throwBall, this.antiFlight, this.swapMode);
   }

   @EventHandler
   public void onKey(KeyEvent event) {
      if (mc.player != null && mc.currentScreen == null) {
         if (event.isBindDown(this.throwBall)) {
            mc.player.networkHandler.sendChatMessage("Кидай шар отпущу я тебе обещаю");
         } else if (event.isBindReleased(this.antiFlight)) {
            this.useAntiFlight();
         } else if (event.isBindReleased(this.trap) && this.phase == ReallyWorldHelper.ActionPhase.IDLE) {
            if (!this.prepareTrap()) {
               ChatMessage.brandmessage("Ловушки нет в инвентаре");
            } else {
               if (this.swapMode.isSelected("New")) {
                  this.beginStop(ReallyWorldHelper.ActionPhase.WAIT_USE_STOP);
               } else {
                  this.usePreparedTrap();
               }
            }
         }
      }
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (mc.player != null) {
         if (this.chatFilter.isValue()
            && event.isSend()
            && event.getPacket() instanceof ChatMessageC2SPacket packet
            && containsBannedWord(packet.chatMessage())) {
            event.setCancelled(true);
            ChatMessage.brandmessage("В сообщении найдено запрещённое слово, отправка отменена");
         }

         if (this.fixAfterPvp.isValue() && event.getType() == PacketEvent.Type.RECEIVE && event.getPacket() instanceof GameMessageS2CPacket packet) {
            String message = packet.content().getString().toLowerCase(Locale.ROOT);
            if (message.contains("pvp режим закончился") || message.contains("можно спокойно выходить")) {
               this.fixAt = System.currentTimeMillis() + 500L;
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player == null) {
         this.cleanup();
         this.fixAt = 0L;
      } else {
         this.closeServerMenu();
         if (this.fixAt != 0L && System.currentTimeMillis() >= this.fixAt) {
            mc.player.networkHandler.sendChatCommand("fix all");
            this.fixAt = 0L;
         }

         if (this.phase == ReallyWorldHelper.ActionPhase.WAIT_USE_STOP) {
            this.movement.block();
            if (this.stopTicks-- > 0) {
               return;
            }

            this.usePreparedTrap();
            this.restoreMovement();
         } else if (this.phase == ReallyWorldHelper.ActionPhase.WAIT_RESTORE && System.currentTimeMillis() >= this.restoreAt) {
            if (this.swapMode.isSelected("New") && !this.fromHotbar) {
               this.beginStop(ReallyWorldHelper.ActionPhase.WAIT_RESTORE_STOP);
            } else {
               this.restoreTrap();
               this.cleanup();
            }
         } else if (this.phase == ReallyWorldHelper.ActionPhase.WAIT_RESTORE_STOP) {
            this.movement.block();
            if (this.stopTicks-- > 0) {
               return;
            }

            this.restoreTrap();
            this.cleanup();
         }
      }
   }

   private void closeServerMenu() {
      if (this.closeMenu.isValue() && mc.currentScreen instanceof HandledScreen<?> screen && mc.player.age < 100) {
         if (screen.getTitle().getString().contains("Если нету 1")) {
            mc.player.closeHandledScreen();
         }
      }
   }

   private boolean prepareTrap() {
      int hotbar = InventoryUtils.findItemInHotbar(Items.HEART_OF_THE_SEA);
      int inventory = hotbar == -1 ? InventoryUtils.findItemInInventory(Items.HEART_OF_THE_SEA) : -1;
      if (hotbar == -1 && inventory == -1) {
         return false;
      }

      this.previousSlot = mc.player.getInventory().getSelectedSlot();
      this.fromHotbar = hotbar != -1;
      this.targetSlot = this.fromHotbar ? hotbar : inventory;
      this.temporaryHotbarSlot = this.fromHotbar ? hotbar : this.previousSlot;
      return true;
   }

   private void usePreparedTrap() {
      if (!this.fromHotbar) {
         InventoryUtils.swapHotbar(this.targetSlot, this.temporaryHotbarSlot);
      }

      InventoryUtils.selectSlotSilent(this.temporaryHotbarSlot);
      InventoryUtils.sendUsePacket(Hand.MAIN_HAND);
      mc.player.swingHand(Hand.MAIN_HAND);
      this.restoreAt = System.currentTimeMillis() + 100L;
      this.phase = ReallyWorldHelper.ActionPhase.WAIT_RESTORE;
   }

   private void restoreTrap() {
      InventoryUtils.selectSlotSilent(this.previousSlot);
      if (!this.fromHotbar) {
         InventoryUtils.swapHotbar(this.targetSlot, this.temporaryHotbarSlot);
      }
   }

   private void useAntiFlight() {
      boolean inOffhand = mc.player.getOffHandStack().isOf(Items.FIREWORK_STAR);
      int slot = inOffhand ? -1 : InventoryUtils.findItemAnywhere(Items.FIREWORK_STAR);
      if (!inOffhand && slot == -1) {
         ChatMessage.brandmessage("Звезды фейерверка нет в инвентаре");
      } else {
         int screenSlot = slot >= 0 && slot < 9 ? slot + 36 : slot;
         if (!inOffhand) {
            InventoryUtils.click(screenSlot, 40, SlotActionType.SWAP);
         }

         mc.options.sneakKey.setPressed(true);
         mc.player.setSneaking(true);
         mc.options.sneakKey.setPressed(false);
         mc.player.setSneaking(false);
         if (!inOffhand) {
            InventoryUtils.click(screenSlot, 40, SlotActionType.SWAP);
         }
      }
   }

   private void beginStop(ReallyWorldHelper.ActionPhase next) {
      this.movement.saveState();
      this.movement.block();
      this.stopTicks = 1;
      this.phase = next;
   }

   private void restoreMovement() {
      if (this.movement.isBlocked()) {
         this.movement.restoreFromCurrent();
      }
   }

   private void cleanup() {
      this.restoreMovement();
      this.phase = ReallyWorldHelper.ActionPhase.IDLE;
      this.previousSlot = -1;
      this.targetSlot = -1;
      this.temporaryHotbarSlot = -1;
      this.stopTicks = 0;
      this.fromHotbar = false;
      this.restoreAt = 0L;
   }

   private static boolean containsBannedWord(String message) {
      String lower = message.toLowerCase(Locale.ROOT);
      return BANNED_WORDS.stream().anyMatch(pattern -> pattern.matcher(lower).find());
   }

   private static Pattern pattern(String alternatives) {
      return Pattern.compile("(?:^|[^a-zа-яё])(?:" + alternatives + ")(?:$|[^a-zа-яё])", 66);
   }

   @Override
   public void deactivate() {
      if ((this.phase == ReallyWorldHelper.ActionPhase.WAIT_RESTORE || this.phase == ReallyWorldHelper.ActionPhase.WAIT_RESTORE_STOP) && mc.player != null) {
         this.restoreTrap();
      }

      this.cleanup();
      this.fixAt = 0L;
   }

   private enum ActionPhase {
      IDLE,
      WAIT_USE_STOP,
      WAIT_RESTORE,
      WAIT_RESTORE_STOP;
   }
}
