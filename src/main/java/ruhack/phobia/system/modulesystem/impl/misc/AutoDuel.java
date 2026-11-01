package ruhack.phobia.system.modulesystem.impl.misc;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.system.modulesystem.setting.implement.TextSetting;
import ruhack.phobia.util.timer.TimerUtil;

public class AutoDuel extends ModuleStructure {
   private final Pattern pattern = Pattern.compile("^\\w{3,16}$");
   private final SelectSetting mode = new SelectSetting(
      "Режим", "Режим дуэли", "Шары", "Шары", "Щит", "Шипы 3", "Незеритка", "Читерский рай", "Лук", "Классик", "Тотемы", "Нодебафф"
   );
   private final SliderSettings slowTime = new SliderSettings("Скорость отправки", "Задержка между запросами", 500.0F).range(300.0F, 1000.0F);
   private final BooleanSetting babki = new BooleanSetting("Играть на деньги", "Ставка монет").setValue(false);
   private final TextSetting money = new TextSetting("Монет", "Количество монет для ставки", "0").visible(() -> this.babki.isValue());
   private double lastPosX;
   private double lastPosY;
   private double lastPosZ;
   private final List<String> sent = Lists.newArrayList();
   private final TimerUtil counter = TimerUtil.create();
   private final TimerUtil counter2 = TimerUtil.create();
   private final TimerUtil counterChoice = TimerUtil.create();
   private final TimerUtil counterTo = TimerUtil.create();

   public AutoDuel() {
      super("AutoDuel", "Автоматически кидает запросы на дуэль", ModuleCategory.MISC);
      this.settings(this.mode, this.slowTime, this.babki, this.money);
   }

   @Override
   public void activate() {
      this.counter.resetCounter();
      this.counter2.resetCounter();
      this.counterChoice.resetCounter();
      this.counterTo.resetCounter();
      this.sent.clear();
      super.activate();
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         this.handleDuelLogic();
         this.handleScreenInteraction();
      }
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (event.getType() == PacketEvent.Type.RECEIVE && event.getPacket() instanceof GameMessageS2CPacket chat) {
         String text = chat.content().getString();
         if (text.contains("начало") && text.contains("через") && text.contains("секунд!")
            || text.contains("дуэли » во время поединка запрещено использовать команды")) {
            this.setState(false);
         }
      }
   }

   private void handleDuelLogic() {
      List<String> players = this.getOnlinePlayers();
      double distance = Math.sqrt(
         Math.pow(this.lastPosX - mc.player.getX(), 2.0) + Math.pow(this.lastPosY - mc.player.getY(), 2.0) + Math.pow(this.lastPosZ - mc.player.getZ(), 2.0)
      );
      if (distance > 500.0) {
         this.setState(false);
      } else {
         this.lastPosX = mc.player.getX();
         this.lastPosY = mc.player.getY();
         this.lastPosZ = mc.player.getZ();
         if (this.counter2.hasTimeElapsed(800L * players.size())) {
            this.sent.clear();
            this.counter2.resetCounter();
         }

         for (String player : players) {
            if (!this.sent.contains(player) && !player.equals(mc.player.getGameProfile().name()) && this.counter.hasTimeElapsed((long)this.slowTime.getValue())
               )
             {
               this.sendDuelRequest(player);
               this.sent.add(player);
               this.counter.resetCounter();
            }
         }
      }
   }

   private void sendDuelRequest(String player) {
      if (this.babki.isValue()) {
         mc.player.networkHandler.sendChatCommand("duel " + player + " " + this.money.getValue());
      } else {
         mc.player.networkHandler.sendChatCommand("duel " + player);
      }
   }

   private void handleScreenInteraction() {
      if (mc.currentScreen != null && mc.player.currentScreenHandler instanceof ScreenHandler chest) {
         String title = mc.currentScreen.getTitle().getString();
         if (title.contains("Выбор набора (1/1)")) {
            if (this.counterChoice.hasTimeElapsed(150L)) {
               int slotID = this.getKitSlot();
               if (slotID >= 0) {
                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slotID, 0, SlotActionType.QUICK_MOVE, mc.player);
               }

               this.counterChoice.resetCounter();
            }
         } else if (title.contains("Настройка поединка") && this.counterTo.hasTimeElapsed(150L)) {
            mc.interactionManager.clickSlot(chest.syncId, 0, 0, SlotActionType.QUICK_MOVE, mc.player);
            this.counterTo.resetCounter();
         }
      }
   }

   private int getKitSlot() {
      return switch (this.mode.getValue()) {
         case "Щит" -> 0;
         case "Шипы 3" -> 1;
         case "Лук" -> 2;
         case "Тотемы" -> 3;
         case "Нодебафф" -> 4;
         case "Шары" -> 5;
         case "Классик" -> 6;
         case "Читерский рай" -> 7;
         case "Незеритка" -> 8;
         default -> -1;
      };
   }

   private List<String> getOnlinePlayers() {
      return mc.player
         .networkHandler
         .getPlayerList()
         .stream()
         .map(PlayerListEntry::getProfile)
         .<String>map(GameProfile::name)
         .filter(profileName -> this.pattern.matcher(profileName).matches())
         .collect(Collectors.toList());
   }
}
