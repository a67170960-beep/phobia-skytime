package ruhack.phobia.system.modulesystem.impl.rage;

import lombok.Generated;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.util.istance.Instance;

public class Velocity extends ModuleStructure {
   private final SelectSetting mode = new SelectSetting("Режим", "Выберите режим уменьшения отдачи", "New Grim", "New Grim", "Old Grim", "Matrix", "Vanilla");
   private boolean flag;
   private int grimTicks;
   private int ccCooldown;
   private Vec3d pendingVelocity;

   public static Velocity getInstance() {
      return Instance.get(Velocity.class);
   }

   public Velocity() {
      super("Velocity", "Убирает отдачу при получении урона", ModuleCategory.RAGE);
      this.settings(this.mode);
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (this.state) {
         if (e.getType() == PacketEvent.Type.RECEIVE) {
            if (mc.player != null && !mc.player.isTouchingWater() && !mc.player.isSubmergedInWater() && !mc.player.isInLava()) {
               if (this.ccCooldown > 0) {
                  this.ccCooldown--;
               } else {
                  if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket pac && pac.getEntityId() == mc.player.getId()) {
                     this.handleVelocityPacket(e, pac);
                  }

                  this.handleAdditionalPackets(e);
               }
            }
         }
      }
   }

   private void handleVelocityPacket(PacketEvent e, EntityVelocityUpdateS2CPacket pac) {
      Vec3d velocity = pac.getVelocity();
      switch (this.mode.getValue()) {
         case "Matrix":
            if (!this.flag) {
               e.setCancelled(true);
               this.flag = true;
            } else {
               this.flag = false;
               e.setCancelled(true);
               this.pendingVelocity = new Vec3d(velocity.x * -0.1, velocity.y, velocity.z * -0.1);
            }
            break;
         case "Vanilla":
            e.setCancelled(true);
            break;
         case "Old Grim":
            e.setCancelled(true);
            this.grimTicks = 6;
            break;
         case "New Grim":
            e.setCancelled(true);
            this.flag = true;
      }
   }

   private void handleAdditionalPackets(PacketEvent e) {
      if (this.mode.isSelected("OldGrim") && e.getPacket() instanceof CommonPingS2CPacket && this.grimTicks > 0) {
         e.setCancelled(true);
         this.grimTicks--;
      }

      if (e.getPacket() instanceof PlayerPositionLookS2CPacket && this.mode.isSelected("NewGrim")) {
         this.ccCooldown = 5;
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (this.state && mc.player != null && !mc.player.isTouchingWater() && !mc.player.isSubmergedInWater()) {
         if (this.mode.isSelected("Matrix")) {
            this.handleMatrixTick();
         }

         if (this.mode.isSelected("New Grim") && this.flag) {
            this.handleNewGrimTick();
         }

         if (this.grimTicks > 0) {
            this.grimTicks--;
         }
      }
   }

   private void handleMatrixTick() {
      if (this.pendingVelocity != null) {
         mc.player.setVelocity(this.pendingVelocity);
         this.pendingVelocity = null;
      }

      if (mc.player.hurtTime > 0 && !mc.player.isOnGround()) {
         double yaw = mc.player.getYaw() * (float) (Math.PI / 180.0);
         double speed = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
         mc.player.setVelocity(-Math.sin(yaw) * speed, mc.player.getVelocity().y, Math.cos(yaw) * speed);
         mc.player.setSprinting(mc.player.age % 2 != 0);
      }
   }

   private void handleNewGrimTick() {
      if (this.ccCooldown <= 0) {
         mc.player
            .networkHandler
            .sendPacket(new Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), false));
         mc.player
            .networkHandler
            .sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, BlockPos.ofFloored(mc.player.getEntityPos()), Direction.DOWN));
      }

      this.flag = false;
   }

   @Override
   public void activate() {
      super.activate();
      this.grimTicks = 0;
      this.flag = false;
      this.ccCooldown = 0;
      this.pendingVelocity = null;
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.pendingVelocity = null;
   }

   @Generated
   public void setFlag(boolean flag) {
      this.flag = flag;
   }

   @Generated
   public void setGrimTicks(int grimTicks) {
      this.grimTicks = grimTicks;
   }

   @Generated
   public void setCcCooldown(int ccCooldown) {
      this.ccCooldown = ccCooldown;
   }

   @Generated
   public void setPendingVelocity(Vec3d pendingVelocity) {
      this.pendingVelocity = pendingVelocity;
   }

   @Generated
   public SelectSetting getMode() {
      return this.mode;
   }

   @Generated
   public boolean isFlag() {
      return this.flag;
   }

   @Generated
   public int getGrimTicks() {
      return this.grimTicks;
   }

   @Generated
   public int getCcCooldown() {
      return this.ccCooldown;
   }

   @Generated
   public Vec3d getPendingVelocity() {
      return this.pendingVelocity;
   }
}
