package ruhack.phobia.system.modulesystem.impl.player;

import net.minecraft.client.option.Perspective;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.CameraPositionEvent;
import ruhack.phobia.system.eventsystem.impl.ChunkOcclusionEvent;
import ruhack.phobia.system.eventsystem.impl.GameLeftEvent;
import ruhack.phobia.system.eventsystem.impl.InputEvent;
import ruhack.phobia.system.eventsystem.impl.MoveEvent;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.istance.Instance;
import ruhack.phobia.util.math.MathUtils;
import ruhack.phobia.util.player.MovingUtil;

public class FreeCam extends ModuleStructure {
   private final SliderSettings speedSetting = new SliderSettings("Скорость", "Выберите скорость камеры отладки", 2.0F).range(0.5F, 5.0F);
   private final BooleanSetting freezeSetting = new BooleanSetting("Заморозка", "Вы замораживаетесь на месте").setValue(false);
   private final BooleanSetting reloadChunksSetting = new BooleanSetting("Reload Chunks", "Отключает cave culling").setValue(true);
   private final BooleanSetting toggleOnLogSetting = new BooleanSetting("Toggle On Log", "Выключает при дисконнекте").setValue(true);
   public Vec3d pos;
   public Vec3d prevPos;

   public static FreeCam getInstance() {
      return Instance.get(FreeCam.class);
   }

   public FreeCam() {
      super("FreeCam", "Свободная камера в мире", ModuleCategory.PLAYER);
      this.settings(this.speedSetting, this.freezeSetting, this.reloadChunksSetting, this.toggleOnLogSetting);
   }

   @Override
   public void activate() {
      this.prevPos = this.pos = mc.getEntityRenderDispatcher().camera.getCameraPos();
      if (this.reloadChunksSetting.isValue()) {
         mc.worldRenderer.reload();
      }

      super.activate();
   }

   @Override
   public void deactivate() {
      if (this.reloadChunksSetting.isValue()) {
         mc.execute(mc.worldRenderer::reload);
      }

      super.deactivate();
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      switch (e.getPacket()) {
         case PlayerMoveC2SPacket move when this.freezeSetting.isValue():
            e.cancel();
            break;
         case PlayerRespawnS2CPacket respawn:
            this.setState(false);
            break;
         case GameJoinS2CPacket join:
            this.setState(false);
            break;
         default:
      }
   }

   @EventHandler
   public void onMove(MoveEvent e) {
      if (this.freezeSetting.isValue()) {
         e.setMovement(Vec3d.ZERO);
      }
   }

   @EventHandler
   public void onInput(InputEvent e) {
      float speed = this.speedSetting.getValue();
      double[] motion = MovingUtil.calculateDirection(e.forward(), e.sideways(), speed);
      this.prevPos = this.pos;
      this.pos = this.pos.add(motion[0], e.getInput().jump() ? speed : (e.getInput().sneak() ? -speed : 0.0), motion[1]);
      e.inputNone();
   }

   @EventHandler
   public void onCameraPosition(CameraPositionEvent e) {
      e.setPos(MathUtils.interpolate(this.prevPos, this.pos));
      mc.options.setPerspective(Perspective.FIRST_PERSON);
   }

   @EventHandler
   public void onChunkOcclusion(ChunkOcclusionEvent event) {
      event.setCancelled(true);
   }

   @EventHandler
   public void onGameLeft(GameLeftEvent event) {
      if (this.toggleOnLogSetting.isValue()) {
         this.setState(false);
      }
   }
}
