package ruhack.phobia.system.modulesystem.impl.rage;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.AttackEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.istance.Instance;

public class Criticals extends ModuleStructure {
   private final SelectSetting mode = new SelectSetting("Режим", "Способ работы критов", "Fast", "Fast", "Packet");
   private final SliderSettings boost = new SliderSettings("Ускорение", "На сколько тиков раньше наносится удар", 2.0F)
      .range(1.0F, 4.0F)
      .step(1.0F)
      .visible(() -> this.mode.isSelected("Fast"));

   public Criticals() {
      super("Criticals", "Ускоряет удар или создаёт пакетный крит без прыжка", ModuleCategory.RAGE);
      this.settings(this.mode, this.boost);
   }

   @EventHandler
   public void onAttack(AttackEvent event) {
      if (this.mode.isSelected("Packet")
         && mc.player != null
         && mc.player.networkHandler != null
         && mc.player.isOnGround()
         && !mc.player.isTouchingWater()
         && !mc.player.isInLava()
         && !mc.player.isClimbing()
         && !mc.player.hasVehicle()) {
         double x = mc.player.getX();
         double y = mc.player.getY();
         double z = mc.player.getZ();
         boolean collision = mc.player.horizontalCollision;
         mc.player.networkHandler.sendPacket(new PositionAndOnGround(x, y + 0.0625, z, false, collision));
         mc.player.networkHandler.sendPacket(new PositionAndOnGround(x, y, z, false, collision));
      }
   }

   public static Criticals getInstance() {
      return Instance.get(Criticals.class);
   }

   public boolean isPostMode() {
      if (this.mode.isSelected("Post")) {
         this.mode.setValue("Fast");
      }

      return this.mode.isSelected("Fast");
   }

   public int getBoostTicks() {
      return this.isState() && this.isPostMode() ? (int)this.boost.getValue() : 0;
   }
}
