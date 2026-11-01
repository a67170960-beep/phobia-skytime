package ruhack.phobia.system.modulesystem.impl.render;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.WorldRenderEvent;
import ruhack.phobia.system.friendsystem.FriendUtils;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.ColorSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.system.rendersystem.render3d.fill.Fill3D;
import ruhack.phobia.system.rendersystem.render3d.line.Line3D;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.istance.Instance;

public class Box extends ModuleStructure {
   private static final String LINE = "Линия";
   private static final String CORNERS = "Углы";
   private static final String DASHED = "Пунктир";
   private static final double CORNER_PART = 0.24;
   private static final double DASH_LENGTH = 0.12;
   private static final double DASH_GAP = 0.08;
   private final SelectSetting mode = new SelectSetting("Вид", "Стиль линий хитбокса", "Линия", "Линия", "Углы", "Пунктир");
   private final BooleanSetting fill = new BooleanSetting("Заполнение", "Добавлять полупрозрачное заполнение внутри контура").setValue(false);
   private final BooleanSetting theme = new BooleanSetting("От темы", "Использовать цвет темы клиента").setValue(true);
   private final ColorSetting color = new ColorSetting("Цвет", "Цвет хитбокса").value(-1).visible(() -> !this.theme.isValue());
   private final ColorSetting friendColor = new ColorSetting("Цвет друзей", "Отдельный цвет хитбокса друзей").value(-11141291);
   private final BooleanSetting throughWalls = new BooleanSetting("Через стены", "Отображать хитбокс сквозь блоки").setValue(true);
   private final BooleanSetting self = new BooleanSetting("Себя", "Отображать свой хитбок от третьего лица").setValue(false);
   private final SliderSettings fillAlpha = new SliderSettings("Прозрачность заполнения", "Прозрачность обычного заполнения", 0.25F)
      .range(0.05F, 1.0F)
      .step(0.05F)
      .visible(this.fill::isValue);

   public Box() {
      super("Box", "Отображает хитбоксы игроков", ModuleCategory.RENDER);
      this.settings(this.mode, this.fill, this.fillAlpha, this.theme, this.color, this.friendColor, this.throughWalls, this.self);
   }

   public static Box getInstance() {
      return Instance.get(Box.class);
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent event) {
      if (mc.player != null && mc.world != null) {
         boolean fillEnabled = this.fill.isValue();
         Vec3d camera = Line3D.getCameraPos();
         if (fillEnabled) {
            Fill3D.begin(this.throughWalls.isValue());
         }

         Line3D.begin(this.throughWalls.isValue());

         for (PlayerEntity player : mc.world.getPlayers()) {
            if (this.shouldRender(player)) {
               net.minecraft.util.math.Box hitbox = this.interpolatedHitbox(player, event.getPartialTicks());
               int lineColor = FriendUtils.isFriend(player)
                  ? this.friendColor.getColorNoAlpha()
                  : (this.theme.isValue() ? ColorUtil.getClientColor() : this.color.getColorNoAlpha());
               float alpha = FriendUtils.isFriend(player) ? this.friendColor.getAlpha() : (this.theme.isValue() ? 1.0F : this.color.getAlpha());
               if (fillEnabled) {
                  float opacity = this.fillAlpha.getValue();
                  Fill3D.box(hitbox, lineColor, alpha * Math.min(1.0F, opacity * 1.35F));
               }

               this.drawHitbox(hitbox, camera, lineColor, alpha);
            }
         }

         if (fillEnabled) {
            Fill3D.end();
         }

         Line3D.end();
      }
   }

   private boolean shouldRender(PlayerEntity player) {
      if (!player.isAlive() || player.isSpectator()) {
         return false;
      } else {
         return player != mc.player ? true : this.self.isValue() && !mc.options.getPerspective().isFirstPerson();
      }
   }

   private net.minecraft.util.math.Box interpolatedHitbox(PlayerEntity player, float tickDelta) {
      Vec3d renderedPosition = player.getLerpedPos(tickDelta);
      return player.getBoundingBox().offset(renderedPosition.subtract(player.getEntityPos()));
   }

   private void drawHitbox(net.minecraft.util.math.Box box, Vec3d camera, int color, float alpha) {
      double minX = box.minX - camera.x;
      double minY = box.minY - camera.y;
      double minZ = box.minZ - camera.z;
      double maxX = box.maxX - camera.x;
      double maxY = box.maxY - camera.y;
      double maxZ = box.maxZ - camera.z;
      double[][] points = new double[][]{
         {minX, minY, minZ},
         {maxX, minY, minZ},
         {maxX, minY, maxZ},
         {minX, minY, maxZ},
         {minX, maxY, minZ},
         {maxX, maxY, minZ},
         {maxX, maxY, maxZ},
         {minX, maxY, maxZ}
      };
      int[][] edges = new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};

      for (int[] edge : edges) {
         double[] start = points[edge[0]];
         double[] end = points[edge[1]];
         if (this.mode.isSelected("Углы")) {
            this.drawCorners(start, end, color, alpha);
         } else if (this.mode.isSelected("Пунктир")) {
            this.drawDashed(start, end, color, alpha);
         } else {
            this.drawLine(start, end, color, alpha);
         }
      }
   }

   private void drawCorners(double[] start, double[] end, int color, float alpha) {
      this.drawLine(start, this.lerp(start, end, 0.24), color, alpha);
      this.drawLine(this.lerp(start, end, 0.76), end, color, alpha);
   }

   private void drawDashed(double[] start, double[] end, int color, float alpha) {
      double length = this.distance(start, end);
      int dashCount = Math.max(1, (int)Math.round((length + 0.08) / 0.2));
      double dashLength = (length - 0.08 * (dashCount - 1)) / dashCount;

      for (int index = 0; index < dashCount; index++) {
         double offset = index * (dashLength + 0.08);
         double from = offset / length;
         double to = (offset + dashLength) / length;
         this.drawLine(this.lerp(start, end, from), this.lerp(start, end, to), color, alpha);
      }
   }

   private void drawLine(double[] start, double[] end, int color, float alpha) {
      Line3D.line((float)start[0], (float)start[1], (float)start[2], (float)end[0], (float)end[1], (float)end[2], color, alpha);
   }

   private double[] lerp(double[] start, double[] end, double delta) {
      return new double[]{start[0] + (end[0] - start[0]) * delta, start[1] + (end[1] - start[1]) * delta, start[2] + (end[2] - start[2]) * delta};
   }

   private double distance(double[] start, double[] end) {
      double x = end[0] - start[0];
      double y = end[1] - start[1];
      double z = end[2] - start[2];
      return Math.sqrt(x * x + y * y + z * z);
   }
}
