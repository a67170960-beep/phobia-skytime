package ruhack.phobia.system.modulesystem.impl.misc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.KeyEvent;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.eventsystem.impl.WorldRenderEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BindSetting;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.rendersystem.render3d.fill.Fill3D;
import ruhack.phobia.system.rendersystem.render3d.line.Line3D;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.player.swap.InventoryUtils;
import ruhack.phobia.util.player.swap.MovementController;
import ruhack.phobia.util.string.chat.ChatMessage;

public class HolyworldHelper extends ModuleStructure {
   private static final long STUN_LIFETIME_MS = 15000L;
   private static final int MAX_STUN_AREAS = 8;
   private static final float STUN_RADIUS = 15.01F;
   private final BindSetting thing = new BindSetting("Штучка", "Использовать огненный заряд");
   private final BindSetting stun = new BindSetting("Стан", "Использовать звезду Незера");
   private final BooleanSetting displayStun = new BooleanSetting("Отображение стана", "Показывать область действия обнаруженного стана");
   private final BindSetting explosiveTrap = new BindSetting("Взрывная Трапка", "Использовать призмариновый осколок");
   private final BindSetting chorusTrap = new BindSetting("Трапка (Хорус)", "Использовать лопнувший плод хоруса");
   private final BindSetting snowball = new BindSetting("Снежок", "Использовать снежок");
   private final BindSetting backpack = new BindSetting("Рюкзак (Шалкер)", "Использовать шалкеровый ящик");
   private final List<HelperItemBind> itemBinds;
   private final BooleanSetting fullStop = new BooleanSetting("Полная остановка", "Полностью останавливать движение перед использованием");
   private final SelectSetting mode = new SelectSetting(
      "Режим свапа", "Быстрый - моментальное использование, New - остановка на один тик перед свапом и возвратом", "Быстрый", "Быстрый", "New"
   );
   private final MovementController movement = new MovementController();
   private final List<HolyworldHelper.StunArea> stunAreas = new ArrayList<>();
   private HolyworldHelper.ActionPhase phase = HolyworldHelper.ActionPhase.IDLE;
   private int previousSlot = -1;
   private int targetSlot = -1;
   private int temporaryHotbarSlot = -1;
   private boolean fromHotbar;
   private int stopTicks;
   private long restoreAt;

   public List<HelperItemBind> getItemBinds() {
      return this.itemBinds;
   }

   public HolyworldHelper() {
      super("HolyWorldHelper", "Помощник для использования специальных предметов HolyWorld", ModuleCategory.MISC);
      this.itemBinds = List.of(
         new HelperItemBind("HW", Items.FIRE_CHARGE, this.thing),
         new HelperItemBind("HW", Items.NETHER_STAR, this.stun),
         new HelperItemBind("HW", Items.PRISMARINE_SHARD, this.explosiveTrap),
         new HelperItemBind("HW", Items.POPPED_CHORUS_FRUIT, this.chorusTrap),
         new HelperItemBind("HW", Items.SNOWBALL, this.snowball),
         new HelperItemBind("HW", Items.SHULKER_BOX, this.backpack)
      );
      this.settings(this.mode, this.thing, this.stun, this.explosiveTrap, this.chorusTrap, this.snowball, this.backpack, this.displayStun, this.fullStop);
   }

   @EventHandler
   public void onKey(KeyEvent event) {
      if (mc.player != null && mc.currentScreen == null && this.phase == HolyworldHelper.ActionPhase.IDLE) {
         Item item = this.getBoundItem(event);
         if (item != null) {
            if (!this.prepare(item)) {
               ChatMessage.brandmessage("Предмет не найден");
            } else {
               if (!this.mode.isSelected("New") && !this.fullStop.isValue()) {
                  this.usePreparedItem();
               } else {
                  this.beginStop(HolyworldHelper.ActionPhase.WAIT_USE_STOP);
               }
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player == null) {
         this.stunAreas.clear();
         this.cleanup();
      } else {
         if (!this.displayStun.isValue()) {
            this.stunAreas.clear();
         } else if (!this.stunAreas.isEmpty()) {
            long now = System.currentTimeMillis();
            this.stunAreas.removeIf(area -> area.expiresAt <= now);
         }

         if (this.phase == HolyworldHelper.ActionPhase.WAIT_USE_STOP) {
            this.movement.block();
            if (this.stopTicks-- <= 0) {
               this.usePreparedItem();
               this.restoreMovement();
            }
         } else if (this.phase == HolyworldHelper.ActionPhase.WAIT_RESTORE && System.currentTimeMillis() >= this.restoreAt) {
            if (this.fromHotbar || !this.mode.isSelected("New") && !this.fullStop.isValue()) {
               this.restoreItem();
               this.cleanup();
            } else {
               this.beginStop(HolyworldHelper.ActionPhase.WAIT_RESTORE_STOP);
            }
         } else {
            if (this.phase == HolyworldHelper.ActionPhase.WAIT_RESTORE_STOP) {
               this.movement.block();
               if (this.stopTicks-- > 0) {
                  return;
               }

               this.restoreItem();
               this.cleanup();
            }
         }
      }
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (this.displayStun.isValue()
         && event.getType() == PacketEvent.Type.RECEIVE
         && event.getPacket() instanceof PlaySoundS2CPacket packet
         && !packet.getSound().getKey().isEmpty()
         && "block.beacon.deactivate".equals(((RegistryKey)packet.getSound().getKey().get()).getValue().getPath())) {
         Vec3d position = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
         if (!this.stunAreas.stream().anyMatch(area -> area.position.squaredDistanceTo(position) < 1.0)) {
            if (this.stunAreas.size() >= 8) {
               this.stunAreas.removeFirst();
            }

            this.stunAreas.add(new HolyworldHelper.StunArea(position, System.currentTimeMillis() + 15000L));
         }
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent event) {
      if (this.displayStun.isValue() && !this.stunAreas.isEmpty() && mc.world != null) {
         long now = System.currentTimeMillis();
         Vec3d camera = Line3D.getCameraPos();
         Fill3D.begin(true);
         Line3D.begin(true);

         for (int i = 0; i < this.stunAreas.size(); i++) {
            HolyworldHelper.StunArea area = this.stunAreas.get(i);
            float lifetime = Math.max(0.0F, (float)(area.expiresAt - now) / 15000.0F);
            int color = ColorUtil.getClientColor(90);
            float alpha = 0.22F + lifetime * 0.3F;
            this.drawStunSquare(area.position, camera, color, alpha);
         }

         Fill3D.end();
         Line3D.end();
      }
   }

   private void drawStunSquare(Vec3d center, Vec3d camera, int color, float alpha) {
      double minX = center.x - 15.01F;
      double maxX = center.x + 15.01F;
      double minZ = center.z - 15.01F;
      double maxZ = center.z + 15.01F;
      double y = center.y + 0.035;
      Fill3D.box(new Box(minX, y, minZ, maxX, y + 0.025, maxZ), color, alpha * 0.16F);
      float x1 = (float)(minX - camera.x);
      float x2 = (float)(maxX - camera.x);
      float z1 = (float)(minZ - camera.z);
      float z2 = (float)(maxZ - camera.z);
      float renderY = (float)(y - camera.y);
      Line3D.line(x1, renderY, z1, x2, renderY, z1, color, alpha);
      Line3D.line(x2, renderY, z1, x2, renderY, z2, color, alpha);
      Line3D.line(x2, renderY, z2, x1, renderY, z2, color, alpha);
      Line3D.line(x1, renderY, z2, x1, renderY, z1, color, alpha);
   }

   private Item getBoundItem(KeyEvent event) {
      if (event.isBindReleased(this.thing)) {
         return Items.FIRE_CHARGE;
      } else if (event.isBindReleased(this.stun)) {
         return Items.NETHER_STAR;
      } else if (event.isBindReleased(this.explosiveTrap)) {
         return Items.PRISMARINE_SHARD;
      } else if (event.isBindReleased(this.chorusTrap)) {
         return Items.POPPED_CHORUS_FRUIT;
      } else if (event.isBindReleased(this.snowball)) {
         return Items.SNOWBALL;
      } else {
         return event.isBindReleased(this.backpack) ? Items.SHULKER_BOX : null;
      }
   }

   private boolean prepare(Item item) {
      int hotbarSlot = InventoryUtils.findItemInHotbar(item);
      int inventorySlot = hotbarSlot == -1 ? InventoryUtils.findItemInInventory(item) : -1;
      if (hotbarSlot == -1 && inventorySlot == -1) {
         return false;
      }

      this.previousSlot = mc.player.getInventory().getSelectedSlot();
      this.fromHotbar = hotbarSlot != -1;
      this.targetSlot = this.fromHotbar ? hotbarSlot : inventorySlot;
      this.temporaryHotbarSlot = this.fromHotbar ? hotbarSlot : this.previousSlot;
      return true;
   }

   private void beginStop(HolyworldHelper.ActionPhase nextPhase) {
      this.movement.saveState();
      this.movement.block();
      this.stopTicks = 1;
      this.phase = nextPhase;
   }

   private void usePreparedItem() {
      if (!this.fromHotbar) {
         InventoryUtils.swapHotbar(this.targetSlot, this.temporaryHotbarSlot);
      }

      InventoryUtils.selectSlotSilent(this.temporaryHotbarSlot);
      InventoryUtils.sendUsePacket(Hand.MAIN_HAND);
      mc.player.swingHand(Hand.MAIN_HAND);
      this.restoreAt = System.currentTimeMillis() + 100L;
      this.phase = HolyworldHelper.ActionPhase.WAIT_RESTORE;
   }

   private void restoreItem() {
      InventoryUtils.selectSlotSilent(this.previousSlot);
      if (!this.fromHotbar) {
         InventoryUtils.swapHotbar(this.targetSlot, this.temporaryHotbarSlot);
      }
   }

   private void restoreMovement() {
      if (this.movement.isBlocked()) {
         this.movement.restoreFromCurrent();
      }
   }

   private void cleanup() {
      this.restoreMovement();
      this.phase = HolyworldHelper.ActionPhase.IDLE;
      this.previousSlot = -1;
      this.targetSlot = -1;
      this.temporaryHotbarSlot = -1;
      this.fromHotbar = false;
      this.stopTicks = 0;
      this.restoreAt = 0L;
   }

   @Override
   public void deactivate() {
      if ((this.phase == HolyworldHelper.ActionPhase.WAIT_RESTORE || this.phase == HolyworldHelper.ActionPhase.WAIT_RESTORE_STOP) && mc.player != null) {
         this.restoreItem();
      }

      this.stunAreas.clear();
      this.cleanup();
   }

   enum ActionPhase {
      IDLE,
      WAIT_USE_STOP,
      WAIT_RESTORE,
      WAIT_RESTORE_STOP;
   }

   private record StunArea(Vec3d position, long expiresAt) {
   }
}
