package ruhack.phobia.system.modulesystem.impl.misc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.RunCommand;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ruhack.phobia.system.commandsystem.CommandManager;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.DrawEvent;
import ruhack.phobia.system.eventsystem.impl.KeyEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BindSetting;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.rendersystem.render2d.Render2D;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.Msdf;
import ruhack.phobia.system.rendersystem.render2d.font.msdf.MsdfManager;
import ruhack.phobia.system.screensystem.clickgui.ClickGui;
import ruhack.phobia.system.screensystem.clickgui.IrcClient;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.string.chat.ChatMessage;

public final class PartyHelper extends ModuleStructure {
   private static final Identifier ARROW_TEXTURE = Identifier.of("phobia", "images/arrows/triangle.png");
   private static final float ARROW_SIZE = 46.0F;
   private static final float RADIUS = 82.0F;
   private final BooleanSetting partyArrows = new BooleanSetting("Arrows к участникам", "Показывает направление, Login и расстояние до участников Party")
      .setValue(true);
   private final BooleanSetting throwCoordinates = new BooleanSetting("Выброс корд", "Отправляет участникам Party текущие координаты").setValue(true);
   private final BindSetting coordinateBind = new BindSetting("Кнопка выброса корд", "Отправить участникам Party текущие координаты");
   private final IrcClient client = IrcClient.INSTANCE;
   private final List<Matrix4f> matrices = new ArrayList<>();

   public PartyHelper() {
      super("PartyHelper", "Помощник участников IRC Party", ModuleCategory.MISC);
      this.settings(this.partyArrows, this.throwCoordinates, this.coordinateBind);
   }

   @Override
   public void activate() {
      this.client.startFromProfile();
      this.client.requestPartyInfo(false);
   }

   @EventHandler
   public void onKey(KeyEvent event) {
      if (this.throwCoordinates.isValue() && mc.player != null && mc.currentScreen == null) {
         if (event.isBindDown(this.coordinateBind)) {
            this.client.sendCoordinates(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      IrcClient.CoordinateEvent coordinates;
      while ((coordinates = this.client.pollCoordinateEvent()) != null) {
         if (this.throwCoordinates.isValue()) {
            this.showCoordinates(coordinates);
         }
      }
   }

   @EventHandler
   public void onDraw(DrawEvent event) {
      if (this.partyArrows.isValue() && mc.player != null && mc.world != null && !mc.options.hudHidden && this.canRenderOnCurrentScreen()) {
         IrcClient.PartySnapshot party = this.client.party();
         if (party.members().size() >= 2) {
            float centerX = Render2D.getFixedScaledWidth() / 2.0F;
            float centerY = Render2D.getFixedScaledHeight() / 2.0F;
            double yaw = Math.toRadians(mc.gameRenderer.getCamera().getYaw());
            double cos = Math.cos(yaw);
            double sin = Math.sin(yaw);
            Vec3d camera = mc.gameRenderer.getCamera().getCameraPos();
            int matrixIndex = 0;

            for (String login : party.members()) {
               if (!login.equalsIgnoreCase(this.client.login())) {
                  IrcClient.PartyPosition remote = this.client.partyPosition(login);
                  if (remote != null) {
                     Vec3d position = new Vec3d(remote.x(), remote.y(), remote.z());
                     double dx = position.x - camera.x;
                     double dz = position.z - camera.z;
                     if (!(dx * dx + dz * dz < 4.0)) {
                        double rotationY = -(dz * cos - dx * sin);
                        double rotationX = -(dx * cos + dz * sin);
                        float angle = (float)Math.toDegrees(Math.atan2(rotationY, rotationX));
                        double radians = Math.toRadians(angle);
                        float arrowX = centerX + (float)Math.cos(radians) * 82.0F;
                        float arrowY = centerY + (float)Math.sin(radians) * 82.0F;
                        int color = ColorUtil.getClientColorAt(matrixIndex * 0.18F);
                        Matrix4f matrix = this.matrix(matrixIndex++);
                        matrix.set(Render2D.createProjection())
                           .translate(arrowX, arrowY, 0.0F)
                           .rotateZ((float)Math.toRadians(angle))
                           .translate(-arrowX, -arrowY, 0.0F);
                        Render2D.image(matrix, arrowX - 23.0F, arrowY - 23.0F, 46.0F, ARROW_TEXTURE, color, 0.0F, true);
                        int distance = (int)Math.round(new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()).distanceTo(position));
                        this.renderLabel(event.getDrawContext(), login, distance, arrowX, arrowY - 23.0F - 10.0F, color);
                     }
                  }
               }
            }
         }
      }
   }

   private void renderLabel(DrawContext context, String login, int distance, float centerX, float y, int color) {
      if (MsdfManager.BOLD != null) {
         float size = 9.0F;
         String text = login + "  " + distance + " м";
         float width = Msdf.width(MsdfManager.BOLD, text, size);
         float x = centerX - width / 2.0F;
         Render2D.rect(context, x - 5.0F, y - 3.0F, width + 10.0F, 14.0F, 5.0F, -871559918, true);
         Msdf.text(context, MsdfManager.BOLD, text, x, y, size, color, true);
      }
   }

   private void showCoordinates(IrcClient.CoordinateEvent event) {
      String prefix = CommandManager.getInstance().getPrefix();
      String gpsCommand = prefix + "gps " + event.x() + " " + event.z();
      Text message = Text.literal("Party:")
         .formatted(Formatting.GRAY)
         .append(Text.literal(event.login()).formatted(Formatting.WHITE))
         .append(Text.literal(" просит помощь на координатах ").formatted(Formatting.GRAY))
         .append(Text.literal(event.x() + " " + event.y() + " " + event.z()).formatted(Formatting.WHITE))
         .append(Text.literal(" [Поставить GPS]").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withClickEvent(new RunCommand(gpsCommand))));
      ChatMessage.brandmessage(message);
   }

   private boolean canRenderOnCurrentScreen() {
      return mc.currentScreen == null
         || mc.currentScreen instanceof ChatScreen
         || mc.currentScreen instanceof GameMenuScreen
         || mc.currentScreen instanceof HandledScreen
         || mc.currentScreen instanceof ClickGui;
   }

   private Matrix4f matrix(int index) {
      while (this.matrices.size() <= index) {
         this.matrices.add(new Matrix4f());
      }

      return this.matrices.get(index);
   }
}
