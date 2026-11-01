package ruhack.phobia.system.modulesystem.impl.display;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import ruhack.phobia.system.commandsystem.impl.GpsCommand;
import ruhack.phobia.system.dragsystem.AbstractHudElement;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfFont;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.util.colorschem.ColorUtil;

public final class GpsHud extends AbstractHudElement {
   private static final Identifier GPS_TEXTURE = Identifier.of("phobia", "images/gps.png");
   private static final float ARROW_SIZE = 24.0F;
   private static final float FONT_SIZE = 7.0F;

   public GpsHud() {
      super("GPS", 0, 0, 24, 31, false);
   }

   @Override
   public boolean visible() {
      return GpsCommand.isEnabled() && this.mc.player != null && this.mc.world != null;
   }

   @Override
   public void drawDraggable(DrawContext context, int alpha) {
      float contextScale = Math.max(0.01F, Render2D.getContextScale());
      float screenWidth = Render2D.getFixedScaledWidth() / contextScale;
      float screenHeight = Render2D.getFixedScaledHeight() / contextScale;
      float drawX = (screenWidth - 24.0F) / 2.0F;
      float drawY = screenHeight / 2.0F - 220.0F;
      drawY = Math.max(8.0F, drawY);
      double dx = GpsCommand.getTargetX() - this.mc.player.getX();
      double dz = GpsCommand.getTargetZ() - this.mc.player.getZ();
      float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - this.mc.player.getYaw() - 180.0);
      int distance = (int)Math.sqrt(dx * dx + dz * dz);
      float centerX = drawX + 12.0F;
      float centerY = drawY + 12.0F;
      Matrix4f rotation = new Matrix4f(Render2D.createProjection())
         .translate(centerX, centerY, 0.0F)
         .rotateZ((float)Math.toRadians(yaw))
         .translate(-centerX, -centerY, 0.0F);
      int white = ColorUtil.rgba(255, 255, 255, alpha);
      int themeColor = ColorUtil.replAlpha(ColorUtil.getClientColorAt(centerX / Math.max(1.0F, screenWidth)), alpha);
      Render2D.image(rotation, drawX, drawY + 3.0F, 24.0F, GPS_TEXTURE, themeColor, 0.0F, true);
      String distanceText = distance + "m";
      MsdfFont font = MsdfManager.BOLD;
      float textWidth = Msdf.width(font, distanceText, 7.0F);
      Msdf.text(context, font, distanceText, centerX - textWidth / 2.0F, drawY - 5.0F, 7.0F, white, false);
      this.x = Math.round(drawX);
      this.y = Math.round(drawY);
      this.width = 24;
      this.height = 31;
   }
}
