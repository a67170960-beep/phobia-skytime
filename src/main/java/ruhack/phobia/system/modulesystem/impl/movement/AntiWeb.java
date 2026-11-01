package ruhack.phobia.system.modulesystem.impl.movement;

import com.adl.nativeprotect.Native;
import net.minecraft.block.Blocks;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.util.istance.Instance;
import ruhack.phobia.util.player.MovingUtil;
import ruhack.phobia.util.string.PlayerInteractionHelper;

public class AntiWeb extends ModuleStructure {
   public final SelectSetting webMode = new SelectSetting("Режим", "Выберите режим обхода", "Grim", "Grim");

   public static AntiWeb getInstance() {
      return Instance.get(AntiWeb.class);
   }

   public AntiWeb() {
      super("AntiWeb", "Ускоряет игрока в паутине", ModuleCategory.MOVEMENT);
      this.settings(this.webMode);
   }

   @EventHandler
   @Native
   public void onTick(TickEvent e) {
      if (PlayerInteractionHelper.isPlayerInBlock(Blocks.COBWEB)) {
         double[] speed = MovingUtil.calculateDirection(0.35);
         mc.player.addVelocity(speed[0], 0.0, speed[1]);
         mc.player.setVelocity(speed[0], mc.options.jumpKey.isPressed() ? 0.65F : (mc.options.sneakKey.isPressed() ? -0.65F : 0.0), speed[1]);
      }
   }
}
